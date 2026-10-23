package com.agentic.platform.service;

import com.agentic.platform.service.phase.Phase3TaskDispatchingAgent;
import com.agentic.platform.service.phase.SdlcJournalService;
import com.agentic.platform.service.phase.SkillLoaderService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Phase3TaskDispatchingAgentTest {

    private StubJournalService journalService;
    private ChatLanguageModel chatLanguageModel;
    private SkillLoaderService skillLoaderService;
    private Phase3TaskDispatchingAgent agent;

    static class StubJournalService extends SdlcJournalService {
        private String stories = "";
        private String c4 = "";

        public void setStories(String stories) { this.stories = stories; }
        public void setC4(String c4) { this.c4 = c4; }

        @Override
        public String readArtifactContent(String projectId, String relativePath) {
            if ("backlog/TECHNICAL_USER_STORIES.md".equals(relativePath)) return stories;
            if ("architecture/C4_MODEL.md".equals(relativePath)) return c4;
            return "";
        }
    }

    @BeforeEach
    void setUp() {
        journalService = new StubJournalService();
        chatLanguageModel = messages -> null;
        skillLoaderService = new SkillLoaderService();
        agent = new Phase3TaskDispatchingAgent(chatLanguageModel, journalService, skillLoaderService);
    }

    @Test
    @DisplayName("Should extract domain aggregates and hexagonal ports from C4 model and user stories")
    void shouldExtractAggregatesAndPorts() {
        String c4 = """
                # C4 Model
                ```mermaid
                graph TD
                    Agg_Order["Order Aggregate"]
                    Agg_Item["InventoryItem Aggregate"]
                    Port_Checkout["ProcessCheckoutUseCase"]
                    Port_OrderRepo["OrderRepositoryPort"]
                ```
                """;
        String stories = """
                ### US-001: Implement Order Placement
                - **Inbound Use Case Port:** `ProcessCheckoutUseCase`
                - **Outbound Port:** `OrderRepositoryPort`
                - **Domain Aggregate:** `Order`
                """;

        List<String> aggregates = agent.parseAggregates(c4, stories);
        List<String> useCases = agent.parseInboundUseCases(c4, stories);
        List<String> outboundPorts = agent.parseOutboundPorts(c4, stories);

        assertThat(aggregates).contains("Order", "InventoryItem");
        assertThat(useCases).contains("ProcessCheckoutUseCase");
        assertThat(outboundPorts).contains("OrderRepositoryPort");
    }

    @Test
    @DisplayName("Should parse dispatched user story titles from Technical Backlog")
    void shouldParseUserStories() {
        String stories = """
                # Technical User Stories
                ### US-001: Implement Metered Event Ingestion
                ### US-002: Implement Subscription Plan Lifecycle
                ### US-003: Implement Prepaid Credit Drawdown
                """;

        List<Phase3TaskDispatchingAgent.DispatchedStory> list = agent.parseUserStorySummaries(stories);

        assertThat(list).hasSize(3);
        assertThat(list.get(0).id()).isEqualTo("US-001");
        assertThat(list.get(0).title()).isEqualTo("Implement Metered Event Ingestion");
        assertThat(list.get(1).id()).isEqualTo("US-002");
        assertThat(list.get(2).id()).isEqualTo("US-003");
    }

    @Test
    @DisplayName("Should dynamically generate fallback DISPATCH_ALLOCATION.md referencing earlier artifacts without hardcoding")
    void shouldGenerateDynamicDispatchAllocationFallback() {
        journalService.setC4("""
                - **Aggregate Root:** Subscription
                - **Aggregate Root:** MeteredEvent
                public interface IngestUsageEventsUseCase { }
                public interface SubscriptionRepositoryPort { }
                """);
        journalService.setStories("""
                ### US-001: Metered Event Ingestion & Aggregation
                ### US-002: Subscription Lifecycle & Plan Tier Management
                """);

        String doc = agent.buildFallbackDispatchAllocation("PRJ-TEST-001", "Multi SAAS Subscription and Usage Billing", "Billing");

        assertThat(doc).contains("Stream-Aligned Team Allocation Matrix");
        assertThat(doc).contains("Subscription, MeteredEvent");
        assertThat(doc).contains("IngestUsageEventsUseCase");
        assertThat(doc).contains("SubscriptionRepositoryPort");
        assertThat(doc).contains("**US-001:** Metered Event Ingestion & Aggregation");
        assertThat(doc).contains("**US-002:** Subscription Lifecycle & Plan Tier Management");
        assertThat(doc).contains("Module Boundary Isolation & Inversion of Control");
        assertThat(doc).contains("Dispatch Checklist & Execution Sequence");
        assertThat(doc).doesNotContain("%s"); // No raw format string placeholders
    }

    @Test
    @DisplayName("Should detect truncated, incomplete, or unclosed dispatch allocation outputs")
    void shouldDetectTruncatedOutputs() {
        // 1. Text with too few lines
        String shortText = "# Dispatch Allocation\n## 1. Stream-Aligned\nSome content";
        assertThat(agent.isInvalidDispatchAllocationOutput(shortText)).isTrue();

        // 2. Unclosed code fence
        StringBuilder unclosed = new StringBuilder();
        unclosed.append("```java\npublic class Ingest {}\n");
        for (int i = 0; i < 30; i++) unclosed.append("line ").append(i).append("\n");
        assertThat(agent.isInvalidDispatchAllocationOutput(unclosed.toString())).isTrue();

        // 3. Missing required sections
        StringBuilder missingSection = new StringBuilder();
        for (int i = 0; i < 30; i++) missingSection.append("line ").append(i).append("\n");
        assertThat(agent.isInvalidDispatchAllocationOutput(missingSection.toString())).isTrue();

        // 4. Complete valid document
        String validDoc = agent.buildFallbackDispatchAllocation("PRJ-1", "Task Alpha", "Desc");
        assertThat(agent.isInvalidDispatchAllocationOutput(validDoc)).isFalse();
    }
}
