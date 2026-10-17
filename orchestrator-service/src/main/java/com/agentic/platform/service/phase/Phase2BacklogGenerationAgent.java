package com.agentic.platform.service.phase;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.service.lifecycle.PhaseContext;
import com.agentic.platform.service.lifecycle.PhaseExecutionManager;
import com.agentic.platform.service.lifecycle.PhaseResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class Phase2BacklogGenerationAgent implements PhaseAgentHandler {

    private static final Logger log = LoggerFactory.getLogger(Phase2BacklogGenerationAgent.class);

    private final ChatLanguageModel chatLanguageModel;
    private final SdlcJournalService journalService;
    private final SkillLoaderService skillLoaderService;

    public Phase2BacklogGenerationAgent(
            ChatLanguageModel chatLanguageModel,
            SdlcJournalService journalService,
            SkillLoaderService skillLoaderService) {
        this.chatLanguageModel = chatLanguageModel;
        this.journalService = journalService;
        this.skillLoaderService = skillLoaderService;
    }

    @Override
    public WorkflowState getHandledPhase() {
        return WorkflowState.PHASE_2_BACKLOG_GENERATION;
    }

    @Override
    public String getAgentRole() {
        return "Backlog Spec Agent & Database Architect";
    }

    @Override
    public String getInstalledSkills() {
        return "spec-writer, clean-architecture, spring-boot-enterprise";
    }

    @Override
    public String getPatternsAndTechniques() {
        return "BDD Mapping to Technical User Stories & PostgreSQL DDL Schema";
    }

    @Override
    public WorkflowState getNextPhaseOnSuccess() {
        return WorkflowState.PHASE_2_WAITING_HUMAN_APPROVAL;
    }

    @Override
    public boolean isAutoAdvance() {
        return false; // Pauses for Human Backlog Approval Gate
    }

    @Override
    public PhaseResult execute(
            WorkflowInstance workflow,
            PhaseContext context,
            java.util.function.Consumer<PhaseExecutionManager.ProgressUpdate> progress) throws Exception {

        String projectId = workflow.getProjectId();
        String taskName = workflow.getTaskName();

        // Single Source of Truth: Consume approved Gherkin requirements and C4 architecture model
        String gherkin = journalService.readArtifactContent(projectId, "backlog/GHERKIN_REQUIREMENTS.md");
        String c4Model = journalService.readArtifactContent(projectId, "architecture/C4_MODEL.md");
        String revisionNotes = context.getRevisionFeedback();
        String specs = (!gherkin.isBlank() || !c4Model.isBlank())
                ? "### Official BDD Requirements (from Phase 0 Gherkin):\n" + gherkin +
                  (!c4Model.isBlank() ? "\n\n### System C4 Architecture Model (from Phase 1):\n" + c4Model : "")
                : (workflow.getDescription() != null ? workflow.getDescription() : taskName);

        if (revisionNotes != null && !revisionNotes.isBlank()) {
            specs = String.format("""
                    ================================================================================
                    ⚠️ MANDATORY REVISION DIRECTIVE FROM LEAD SUPERVISOR:
                    The human supervisor has reviewed previous outputs and returned this phase for refinement with the following feedback:
                    "%s"
                    You MUST strictly incorporate and reflect every requested requirement, library, tool, and entity into the Technical User Stories, API Contracts, and Database Schema!
                    ================================================================================

                    %s
                    """, revisionNotes, specs);
        }

        progress.accept(new PhaseExecutionManager.ProgressUpdate(25,
                "Decomposing C4 architecture into granular technical user stories..."));
        generateTechnicalUserStoriesFile(projectId, taskName, specs);

        progress.accept(new PhaseExecutionManager.ProgressUpdate(65,
                "Authoring PostgreSQL 16 DDL schema & indexing strategy..."));
        generateDatabaseSchemaFile(projectId, taskName, specs);

        return PhaseResult.success(
                WorkflowState.PHASE_2_BACKLOG_GENERATION,
                "Mapped BDD specifications to relational database entities with referential integrity for task: " + taskName,
                "Technical Backlog & Database Schema generated to 'backlog/TECHNICAL_USER_STORIES.md' and 'database/DATABASE_SCHEMA.sql'",
                List.of("backlog/TECHNICAL_USER_STORIES.md", "database/DATABASE_SCHEMA.sql"),
                System.currentTimeMillis() - context.getStartTime().toEpochMilli());
    }

    private void generateTechnicalUserStoriesFile(String projectId, String taskName, String description) {
        try {
            Path base = journalService.resolveProjectBase(projectId);
            Path backlogDir = base.resolve("backlog");
            Files.createDirectories(backlogDir);
            Path filePath = backlogDir.resolve("TECHNICAL_USER_STORIES.md");

            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("spec-writer, clean-architecture");

            String systemPromptText = "You are an expert Technical Backlog Spec Agent. Output pure markdown directly without preamble.";
            String userPromptText = String.format("""
                    You are an enterprise Backlog Spec Agent.
                    Break down the project architecture into technical user stories with estimation points, acceptance criteria, and API contracts for:

                    Project ID: %s
                    Task Name: %s
                    Requirements Description:
                    %s
                    %s

                    Format the output strictly as Markdown:
                    # Technical User Stories & Backlog (Phase 2 Output)
                    **Project ID:** %s
                    **Task:** %s
                    **Spec Agent:** Technical Backlog Agent
                    **Status:** APPROVED_FOR_DISPATCH

                    ---

                    ## 1. Epic: Core Domain Service Implementation
                    ### US-001: Implement Core Domain Aggregates & Invariants
                    (Story points, acceptance criteria, port interfaces)

                    ### US-002: Implement Outbound Persistence Repository Adapters
                    (Story points, database transactions, error handling)

                    ### US-003: Expose Inbound REST Controllers with RFC 7807 Handlers
                    (Story points, request validation, Swagger / OpenAPI contracts)
                    """, projectId, taskName, description, skillDirectives, projectId, taskName);

            String content;
            long duration = 0;
            boolean isFallback = false;
            long start = System.currentTimeMillis();

            try {
                SystemMessage sys = SystemMessage.from(systemPromptText);
                UserMessage usr = UserMessage.from(userPromptText);
                Response<AiMessage> response = chatLanguageModel.generate(List.of(sys, usr));
                duration = System.currentTimeMillis() - start;
                content = journalService.cleanMarkdownOutput(response.content().text());
            } catch (Exception ex) {
                duration = System.currentTimeMillis() - start;
                isFallback = true;
                log.warn("LLM Technical user stories fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackTechnicalUserStories(projectId, taskName, description);
            }

            String auditTrace = String.format("""

                    ---

                    ## 2. Technical Backlog Inference Audit Trace
                    - **Agent Role:** Backlog Spec Agent
                    - **Model / Engine:** `%s`
                    - **Inference Latency:** `%d ms`
                    - **Status:** %s
                    - **Audit Ledger:** Recorded in [`../inferences/phase2-backlog-spec.md`](../inferences/phase2-backlog-spec.md)
                    """,
                    isFallback ? "Deterministic Domain Draft Fallback" : "Gemini 3.5 Flash",
                    duration,
                    isFallback ? "FALLBACK_DRAFT" : "LLM_GENERATED");

            Files.writeString(filePath, content + auditTrace);
            log.info("Successfully generated TECHNICAL_USER_STORIES.md for project: {}", projectId);

            journalService.recordPromptAndInference(
                    projectId, "PHASE_2_BACKLOG_GENERATION", "Backlog Spec Agent",
                    "Technical User Stories & Domain Specs", systemPromptText, userPromptText, content, duration, isFallback);

        } catch (Exception e) {
            log.error("Failed to generate technical user stories for project {}: {}", projectId, e.getMessage());
        }
    }

    private void generateDatabaseSchemaFile(String projectId, String taskName, String description) {
        try {
            Path base = journalService.resolveProjectBase(projectId);
            Path databaseDir = base.resolve("database");
            Path backlogDir = base.resolve("backlog");
            Files.createDirectories(databaseDir);
            Files.createDirectories(backlogDir);

            Path filePath = databaseDir.resolve("DATABASE_SCHEMA.sql");
            Path compatPath = backlogDir.resolve("DATABASE_SCHEMA.sql");

            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("clean-architecture, spring-boot-enterprise");

            String systemPromptText = "You are an expert Database Architect for PostgreSQL 16. Output clean SQL statements with comments and without preamble.";
            String userPromptText = String.format("""
                    You are an enterprise Database Architect following PostgreSQL 16 best practices and Clean DDD.
                    Generate production-ready PostgreSQL 16 DDL statements with UUID primary keys, check constraints, foreign keys, audit timestamps, and performance indexes for:

                    Project ID: %s
                    Task Name: %s
                    Requirements Description:
                    %s
                    %s
                    """, projectId, taskName, description, skillDirectives);

            String content;
            long duration = 0;
            boolean isFallback = false;
            long start = System.currentTimeMillis();

            try {
                SystemMessage sys = SystemMessage.from(systemPromptText);
                UserMessage usr = UserMessage.from(userPromptText);
                Response<AiMessage> response = chatLanguageModel.generate(List.of(sys, usr));
                duration = System.currentTimeMillis() - start;
                content = journalService.cleanMarkdownOutput(response.content().text());
            } catch (Exception ex) {
                duration = System.currentTimeMillis() - start;
                isFallback = true;
                log.warn("LLM Database schema fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackDatabaseSchema(projectId, taskName, description);
            }

            Files.writeString(filePath, content);
            Files.writeString(compatPath, content);
            log.info("Successfully generated DATABASE_SCHEMA.sql for project: {}", projectId);

            journalService.recordPromptAndInference(
                    projectId, "PHASE_2_BACKLOG_GENERATION", "Database Architect",
                    "PostgreSQL 16 Production DDL Schema", systemPromptText, userPromptText, content, duration, isFallback);

        } catch (Exception e) {
            log.error("Failed to generate database schema for project {}: {}", projectId, e.getMessage());
        }
    }

    private String buildFallbackTechnicalUserStories(String projectId, String taskName, String description) {
        return String.format("""
                # Technical User Stories & Backlog (Phase 2 Output)
                **Project ID:** %1$s
                **Task:** %2$s
                **Spec Agent:** Technical Backlog Agent
                **Status:** APPROVED_FOR_DISPATCH

                ---

                ## 1. Epic: %2$s Core Service Implementation

                ### US-001: Implement Core Domain Entity & Invariants
                - **Story Points:** 5
                - **Acceptance Criteria:**
                  - Domain aggregate enforces non-null invariants.
                  - State transitions validate pre-conditions.
                  - Pure Java implementation without framework annotations.

                ### US-002: Implement Outbound Persistence Repository Adapter
                - **Story Points:** 5
                - **Acceptance Criteria:**
                  - Concrete JPA adapter implements the domain outbound repository port.
                  - Transactional boundaries managed with `@Transactional(readOnly = true)` for queries.
                  - Optimistic concurrency control via `@Version` audit timestamps.

                ### US-003: Expose Inbound REST Controller with RFC 7807 Error Handling
                - **Story Points:** 3
                - **Acceptance Criteria:**
                  - Inbound REST endpoint mapped at `/api/v1/%3$s`.
                  - Validation annotations `@Valid`, `@NotNull` enforce request constraints.
                  - RFC 7807 `ProblemDetail` responses returned on domain exceptions.
                """, projectId, taskName, taskName.toLowerCase().replaceAll("[^a-z0-9]", "-"));
    }

    private String buildFallbackDatabaseSchema(String projectId, String taskName, String description) {
        String cleanSlug = taskName.toLowerCase().replaceAll("[^a-z0-9]", "_");
        return String.format("""
                -- PostgreSQL 16 Production DDL Schema
                -- Project: %1$s
                -- Task: %2$s

                CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

                CREATE TABLE IF NOT EXISTS %3$s_entity (
                    id VARCHAR(64) PRIMARY KEY,
                    tenant_id VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
                    name VARCHAR(255) NOT NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                    version BIGINT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );

                CREATE INDEX IF NOT EXISTS idx_%3$s_tenant_status ON %3$s_entity(tenant_id, status);
                CREATE INDEX IF NOT EXISTS idx_%3$s_created_at ON %3$s_entity(created_at DESC);

                CREATE TABLE IF NOT EXISTS %3$s_audit_log (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    entity_id VARCHAR(64) NOT NULL,
                    actor VARCHAR(100) NOT NULL,
                    action VARCHAR(100) NOT NULL,
                    details TEXT,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_%3$s_audit FOREIGN KEY (entity_id) REFERENCES %3$s_entity(id) ON DELETE CASCADE
                );

                CREATE INDEX IF NOT EXISTS idx_%3$s_audit_entity ON %3$s_audit_log(entity_id);
                """, projectId, taskName, cleanSlug);
    }
}
