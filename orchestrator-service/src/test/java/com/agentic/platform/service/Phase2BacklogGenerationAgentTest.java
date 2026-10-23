package com.agentic.platform.service;

import com.agentic.platform.service.phase.Phase2BacklogGenerationAgent;
import com.agentic.platform.service.phase.SdlcJournalService;
import com.agentic.platform.service.phase.SkillLoaderService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Phase2BacklogGenerationAgentTest {

    private StubJournalService journalService;
    private ChatLanguageModel chatLanguageModel;
    private SkillLoaderService skillLoaderService;
    private Phase2BacklogGenerationAgent agent;

    static class StubJournalService extends SdlcJournalService {
        private String gherkin = "";
        private String c4 = "";

        public void setGherkin(String gherkin) { this.gherkin = gherkin; }
        public void setC4(String c4) { this.c4 = c4; }

        @Override
        public String readArtifactContent(String projectId, String relativePath) {
            if ("backlog/GHERKIN_REQUIREMENTS.md".equals(relativePath)) return gherkin;
            if ("architecture/C4_MODEL.md".equals(relativePath)) return c4;
            return "";
        }
    }

    @BeforeEach
    void setUp() {
        journalService = new StubJournalService();
        chatLanguageModel = messages -> null;
        skillLoaderService = new SkillLoaderService();
        agent = new Phase2BacklogGenerationAgent(chatLanguageModel, journalService, skillLoaderService);
    }

    @Test
    @DisplayName("Should parse structured BDD user stories from Gherkin artifact")
    void shouldParseUserStoriesFromGherkin() {
        String sampleGherkin = """
                # Gherkin Requirements
                ### User Story 1: Metered Usage Tracking
                **As a** Tenant Dev
                **I want to** ingest usage events
                **So that** billing is accurate.

                ```gherkin
                Scenario: Successfully ingest valid events
                  Given a valid tenant
                  When event is sent
                  Then return 200 OK
                Scenario: Idempotent duplicate event handling
                  Given duplicate event
                  Then return DUPLICATE_IGNORED
                ```

                ### User Story 2: Subscription Plan Management
                **As a** Tenant Admin
                **I want to** upgrade plan
                **So that** features scale.

                ```gherkin
                Scenario: Upgrade plan with pro-rata charge
                  Given active plan
                  When upgrade requested
                  Then update plan
                ```
                """;

        List<Phase2BacklogGenerationAgent.ParsedUserStory> stories = agent.parseUserStoriesFromGherkin(sampleGherkin);

        assertThat(stories).hasSize(2);
        assertThat(stories.get(0).id()).isEqualTo("US-001");
        assertThat(stories.get(0).title()).isEqualTo("Metered Usage Tracking");
        assertThat(stories.get(0).persona()).isEqualTo("Tenant Dev");
        assertThat(stories.get(0).scenarios()).hasSize(2);
        assertThat(stories.get(0).storyPoints()).isEqualTo(8);

        assertThat(stories.get(1).id()).isEqualTo("US-002");
        assertThat(stories.get(1).title()).isEqualTo("Subscription Plan Management");
        assertThat(stories.get(1).scenarios()).hasSize(1);
        assertThat(stories.get(1).storyPoints()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should parse domain aggregates and hexagonal ports from C4 Model artifact")
    void shouldParseAggregatesAndPortsFromC4() {
        String sampleC4 = """
                # C4 Architecture Model
                ```mermaid
                graph TD
                    Agg_Sub["Subscription Aggregate"]
                    Agg_Event["MeteredEvent Aggregate"]
                    Port_Ingest["IngestUsageEventsUseCase"]
                    Port_SubRepo["SubscriptionRepositoryPort"]
                ```
                ## 2. Bounded Contexts
                ### 1. Subscription Context
                - **Aggregate Root:** Subscription
                ### 2. Metering Context
                - **Aggregate Root:** MeteredEvent
                """;

        List<String> aggregates = agent.parseAggregatesFromC4(sampleC4);
        List<String> useCases = agent.parseInboundUseCasesFromC4(sampleC4);
        List<String> outboundPorts = agent.parseOutboundPortsFromC4(sampleC4);

        assertThat(aggregates).contains("Subscription", "MeteredEvent");
        assertThat(useCases).contains("IngestUsageEventsUseCase");
        assertThat(outboundPorts).contains("SubscriptionRepositoryPort");
    }

    @Test
    @DisplayName("Should dynamically generate fallback Technical User Stories referencing earlier artifacts without hardcoding")
    void shouldGenerateDynamicTechnicalUserStoriesFallback() {
        journalService.setGherkin("""
                ### User Story 1: Fleet Telemetry Ingestion
                **As a** Logistics Officer
                **I want to** ingest GPS coordinates
                **So that** vehicles are monitored.

                Scenario: Valid GPS coordinate ingestion
                  Given valid truck
                  When GPS ping arrives
                  Then location is updated
                """);
        journalService.setC4("""
                public interface IngestTelemetryUseCase { }
                public interface VehicleRepositoryPort { }
                - **Aggregate Root:** Vehicle
                """);

        String storiesDoc = agent.buildFallbackTechnicalUserStories("PRJ-FLEET-1", "Fleet Management System", "Track trucks");

        assertThat(storiesDoc).contains("US-001: Fleet Telemetry Ingestion");
        assertThat(storiesDoc).contains("Logistics Officer");
        assertThat(storiesDoc).contains("IngestTelemetryUseCase");
        assertThat(storiesDoc).contains("VehicleRepositoryPort");
        assertThat(storiesDoc).contains("Valid GPS coordinate ingestion");
        assertThat(storiesDoc).doesNotContain("%3$s"); // No unparsed format placeholders
    }

    @Test
    @DisplayName("Should dynamically generate fallback PostgreSQL 16 DDL schema matching C4 aggregates without hardcoded dummy entity")
    void shouldGenerateDynamicDatabaseSchemaFallback() {
        journalService.setC4("""
                - **Aggregate Root:** Device
                - **Aggregate Root:** SensorReading
                public interface DeviceRepositoryPort { }
                public interface SensorReadingRepositoryPort { }
                """);

        String sql = agent.buildFallbackDatabaseSchema("PRJ-IOT-1", "IoT Sensor Monitoring", "Telemetry");

        assertThat(sql).contains("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS devices (");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS sensor_readings (");
        assertThat(sql).contains("tenant_id VARCHAR(64) NOT NULL");
        assertThat(sql).contains("idx_devices_tenant");
        assertThat(sql).contains("audit_logs");
        assertThat(sql).doesNotContain("taskname_entity"); // No hardcoded generic entity
    }

    @Test
    @DisplayName("Should rigorously detect truncated or interrupted LLM outputs")
    void shouldDetectTruncatedOutputs() {
        // 1. Truncated user story ending mid-word (exact symptom from user's report)
        String truncatedStory = """
                # Technical User Stories
                ## 1. Epic: Core Service
                ### US-001: Implement Core
                *   All DDD Aggreg
                """;
        assertThat(agent.isInvalidUserStoriesOutput(truncatedStory)).isTrue();

        // 2. Truncated database schema with no CREATE TABLE and cutting off at enum section (exact symptom from report)
        String truncatedSql = """
                sql
                CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
                CREATE OR REPLACE FUNCTION update_updated_at_column()
                RETURNS TRIGGER AS $$
                BEGIN
                    NEW.updated_at = NOW();
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql;

                -- 2. Custom ENUM Types
                """;
        assertThat(agent.isInvalidSqlSchemaOutput(truncatedSql)).isTrue();

        // 3. Complete valid SQL schema
        String validSql = """
                CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
                CREATE TABLE IF NOT EXISTS subscriptions (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    tenant_id VARCHAR(64) NOT NULL,
                    plan_tier VARCHAR(50) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
                CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant ON subscriptions(tenant_id);
                """;
        // make sure it has >= 20 lines to test valid length
        StringBuilder longValidSql = new StringBuilder(validSql);
        for (int i = 0; i < 15; i++) {
            longValidSql.append("-- Comment line ").append(i).append("\n");
        }
        assertThat(agent.isInvalidSqlSchemaOutput(longValidSql.toString())).isFalse();
    }
}
