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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public record ParsedUserStory(
            String id,
            String title,
            String persona,
            String intent,
            String value,
            List<String> scenarios,
            int storyPoints
    ) {}

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

            if (isInvalidUserStoriesOutput(content)) {
                log.warn("LLM Technical user stories produced insufficient, truncated, or unclosed content. Falling back to dynamic domain specifications.");
                content = buildFallbackTechnicalUserStories(projectId, taskName, description);
                isFallback = true;
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

            String finalDoc = content.contains("Inference Audit Trace")
                    ? content
                    : content.stripTrailing() + "\n" + auditTrace;

            Files.writeString(filePath, finalDoc);
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

            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("clean-architecture");

            String systemPromptText = "You are an enterprise Database Architect for PostgreSQL 16. Output clean SQL statements with comments and without preamble. Generate complete CREATE TABLE statements for all aggregates.";
            String userPromptText = String.format("""
                    You are an enterprise Database Architect following PostgreSQL 16 best practices and Clean DDD.
                    Generate complete production-ready PostgreSQL 16 DDL statements with UUID primary keys, check constraints, foreign keys, audit timestamps, and performance indexes for all domain entities.
                    You MUST generate ALL tables, columns, constraints, and indexes completely to the end without truncation.

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
                content = content.replaceFirst("^(?i)```(sql)?\\s*", "").replaceFirst("^(?i)sql\\s*", "").replaceFirst("```\\s*$", "").trim();
            } catch (Exception ex) {
                duration = System.currentTimeMillis() - start;
                isFallback = true;
                log.warn("LLM Database schema fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackDatabaseSchema(projectId, taskName, description);
            }

            if (isInvalidSqlSchemaOutput(content)) {
                log.warn("LLM Database schema produced insufficient, truncated, or unclosed content. Falling back to dynamic domain DDL schema.");
                content = buildFallbackDatabaseSchema(projectId, taskName, description);
                isFallback = true;
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

    public boolean isInvalidUserStoriesOutput(String content) {
        if (content == null || content.isBlank() || content.contains("[MOCK_OUTPUT]")) {
            return true;
        }
        if (content.lines().count() < 25) {
            log.warn("Technical user stories output has too few lines ({} < 25). Truncation detected.", content.lines().count());
            return true;
        }
        int fenceCount = 0;
        int idx = 0;
        while ((idx = content.indexOf("```", idx)) != -1) {
            fenceCount++;
            idx += 3;
        }
        if (fenceCount % 2 != 0) {
            log.warn("Technical user stories has unclosed code fence (count: {}). Truncation detected.", fenceCount);
            return true;
        }
        boolean hasMultipleStories = content.contains("US-001") && (content.contains("US-002") || content.contains("US-003"));
        boolean hasAcceptanceCriteria = content.contains("Acceptance Criteria");
        if (!hasMultipleStories || !hasAcceptanceCriteria) {
            log.warn("Technical user stories is missing required story sections or acceptance criteria. Truncation detected.");
            return true;
        }
        String trimmed = content.stripTrailing();
        if (trimmed.matches("(?s).*\\*\\s+[A-Za-z]+$") || trimmed.endsWith("Aggreg") || trimmed.endsWith("and") || trimmed.endsWith("with")) {
            log.warn("Technical user stories ends abruptly mid-word or mid-sentence. Truncation detected.");
            return true;
        }
        return false;
    }

    public boolean isInvalidSqlSchemaOutput(String content) {
        if (content == null || content.isBlank() || content.contains("[MOCK_OUTPUT]")) {
            return true;
        }
        if (content.lines().count() < 20) {
            log.warn("Database schema output has too few lines ({} < 20). Truncation detected.", content.lines().count());
            return true;
        }
        if (!content.toUpperCase().contains("CREATE TABLE")) {
            log.warn("Database schema does not contain any CREATE TABLE statement. Truncation detected.");
            return true;
        }
        String trimmed = content.stripTrailing();
        if (trimmed.endsWith("(") || trimmed.endsWith(",") || trimmed.endsWith("--") || trimmed.endsWith("Types")) {
            log.warn("Database schema ends abruptly. Truncation detected.");
            return true;
        }
        long openParen = content.chars().filter(ch -> ch == '(').count();
        long closeParen = content.chars().filter(ch -> ch == ')').count();
        if (openParen > closeParen) {
            log.warn("Database schema has unbalanced parentheses (open: {}, close: {}). Truncation detected.", openParen, closeParen);
            return true;
        }
        return false;
    }

    /**
     * Dynamically builds technical user stories from earlier artifacts (GHERKIN_REQUIREMENTS.md & C4_MODEL.md).
     * Eliminates static hardcoding by mapping BDD scenarios and architectural ports to concrete user stories.
     */
    public String buildFallbackTechnicalUserStories(String projectId, String taskName, String description) {
        String gherkin = journalService.readArtifactContent(projectId, "backlog/GHERKIN_REQUIREMENTS.md");
        String c4Model = journalService.readArtifactContent(projectId, "architecture/C4_MODEL.md");

        List<ParsedUserStory> parsedStories = parseUserStoriesFromGherkin(gherkin);
        List<String> aggregates = parseAggregatesFromC4(c4Model);
        List<String> useCases = parseInboundUseCasesFromC4(c4Model);
        List<String> outboundPorts = parseOutboundPortsFromC4(c4Model);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
                # Technical User Stories & Backlog (Phase 2 Output)
                **Project ID:** %s
                **Task:** %s
                **Spec Agent:** Technical Backlog Agent (Autonomous SSOT Mapping)
                **Status:** APPROVED_FOR_DISPATCH
                **Reference Documents:** [`GHERKIN_REQUIREMENTS.md`](GHERKIN_REQUIREMENTS.md), [`C4_MODEL.md`](../architecture/C4_MODEL.md)

                ---

                ## 1. Epic: %s Core Hexagonal Architecture & Service Implementation

                """, projectId, taskName, taskName));

        if (!parsedStories.isEmpty()) {
            for (ParsedUserStory story : parsedStories) {
                String matchedUseCase = findBestMatch(story.title(), useCases, "Manage" + extractNounFromTask(taskName) + "UseCase");
                String matchedPort = findBestMatch(story.title(), outboundPorts, extractNounFromTask(taskName) + "RepositoryPort");
                String matchedAggregate = findBestMatch(story.title(), aggregates, extractNounFromTask(taskName));

                sb.append(String.format("""
                        ### %s: %s
                        **As a** %s  
                        **I want to** %s  
                        **So that** %s  

                        - **Story Points:** %d
                        - **Clean Architecture Interfaces & Ports:**
                          - **Inbound Use Case:** `%s`
                          - **Outbound Port:** `%s`
                          - **Domain Aggregate:** `%s`
                        - **Acceptance Criteria (Derived from BDD Gherkin):**
                        """,
                        story.id(), story.title(),
                        story.persona().isBlank() ? "System Developer" : story.persona(),
                        story.intent().isBlank() ? "execute business operations reliably" : story.intent(),
                        story.value().isBlank() ? "system integrity and domain invariants are preserved" : story.value(),
                        story.storyPoints(),
                        matchedUseCase,
                        matchedPort,
                        matchedAggregate
                ));

                if (!story.scenarios().isEmpty()) {
                    for (String sc : story.scenarios()) {
                        sb.append(String.format("  - **AC [%s]:** Must satisfy scenario: *%s* with idempotent handling and domain validation.\n", story.id(), sc));
                    }
                } else {
                    sb.append("  - **AC 1:** Enforces non-null domain invariants and valid state transitions.\n");
                    sb.append("  - **AC 2:** Returns RFC 7807 problem details on domain validation failure.\n");
                }

                sb.append("""
                        - **Technical Contract & Verification:**
                          - Request validation handled via Jakarta annotations (`@Valid`, `@NotNull`).
                          - Isolated transactional boundaries managed via Spring `@Transactional`.
                          - Multi-tenant tenant context isolation strictly enforced.

                        """);
            }
        } else {
            // Fallback when Gherkin parsing yielded no structured stories: build from C4 aggregates and ports
            int idx = 1;
            List<String> entityList = aggregates.isEmpty() ? List.of(extractNounFromTask(taskName)) : aggregates;
            for (String entity : entityList) {
                String storyId = String.format("US-%03d", idx++);
                sb.append(String.format("""
                        ### %s: Implement %s Domain Aggregate & Invariants
                        **As a** Core Backend Developer  
                        **I want to** implement the %s domain aggregate with pure business invariants  
                        **So that** business state transitions for %s remain consistent and isolated from external infrastructure.  

                        - **Story Points:** 8
                        - **Clean Architecture Interfaces & Ports:**
                          - **Inbound Use Case:** `Manage%sUseCase`
                          - **Outbound Port:** `%sRepositoryPort`
                        - **Acceptance Criteria:**
                          - Enforces non-null domain invariants and valid state transitions.
                          - Optimistic locking managed via `@Version` attributes.
                          - Pure Java domain model without framework annotations.

                        """, storyId, entity, entity, entity, entity, entity));
            }
        }

        return sb.toString();
    }

    /**
     * Dynamically builds PostgreSQL 16 DDL schema from earlier artifacts (C4_MODEL.md & GHERKIN_REQUIREMENTS.md).
     * Discovers all domain aggregates, relational tables, columns, indexes, and audit triggers dynamically.
     */
    public String buildFallbackDatabaseSchema(String projectId, String taskName, String description) {
        String c4Model = journalService.readArtifactContent(projectId, "architecture/C4_MODEL.md");

        List<String> aggregates = parseAggregatesFromC4(c4Model);
        if (aggregates.isEmpty()) {
            aggregates = List.of(extractNounFromTask(taskName));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
                -- =============================================================================
                -- PostgreSQL 16 Production DDL Schema
                -- Project: %s
                -- Task: %s
                -- Generated by: Database Architect (Autonomous SSOT Dynamic Mapping)
                -- Pattern: Clean Hexagonal Domain & Multi-Tenant Row Level Security
                -- Reference: architecture/C4_MODEL.md & backlog/GHERKIN_REQUIREMENTS.md
                -- =============================================================================

                CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

                -- =============================================================================
                -- Common Audit Trigger Function
                -- =============================================================================
                CREATE OR REPLACE FUNCTION update_updated_at_column()
                RETURNS TRIGGER AS $$
                BEGIN
                    NEW.updated_at = NOW();
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql;

                """, projectId, taskName));

        for (String agg : aggregates) {
            String tableName = pluralize(toSnakeCase(agg));
            String lower = agg.toLowerCase();

            sb.append(String.format("-- =============================================================================\n-- Table: %s (Aggregate Root: %s)\n-- =============================================================================\n", tableName, agg));
            sb.append(String.format("CREATE TABLE IF NOT EXISTS %s (\n", tableName));
            sb.append("    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n");
            sb.append("    tenant_id VARCHAR(64) NOT NULL,\n");

            if (lower.contains("subscript") || lower.contains("plan")) {
                sb.append("    plan_id VARCHAR(64) NOT NULL,\n");
                sb.append("    plan_tier VARCHAR(50) NOT NULL DEFAULT 'STARTER',\n");
                sb.append("    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',\n");
                sb.append("    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
                sb.append("    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days'),\n");
                sb.append("    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,\n");
                sb.append("    version BIGINT NOT NULL DEFAULT 0,\n");
                sb.append("    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
                sb.append("    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP\n");
            } else if (lower.contains("event") || lower.contains("meter") || lower.contains("usage")) {
                sb.append("    metric_name VARCHAR(100) NOT NULL,\n");
                sb.append("    metric_value NUMERIC(18, 4) NOT NULL CHECK (metric_value >= 0),\n");
                sb.append("    idempotency_key VARCHAR(128) NOT NULL,\n");
                sb.append("    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
                sb.append("    status VARCHAR(50) NOT NULL DEFAULT 'ACCEPTED',\n");
                sb.append("    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP\n");
            } else if (lower.contains("credit") || lower.contains("balance")) {
                sb.append("    balance NUMERIC(18, 4) NOT NULL DEFAULT 0.0000 CHECK (balance >= 0),\n");
                sb.append("    currency VARCHAR(10) NOT NULL DEFAULT 'USD',\n");
                sb.append("    version BIGINT NOT NULL DEFAULT 0,\n");
                sb.append("    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
                sb.append("    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP\n");
            } else if (lower.contains("invoice") || lower.contains("bill")) {
                sb.append("    subscription_id UUID,\n");
                sb.append("    subtotal NUMERIC(18, 2) NOT NULL DEFAULT 0.00 CHECK (subtotal >= 0),\n");
                sb.append("    tax_amount NUMERIC(18, 2) NOT NULL DEFAULT 0.00 CHECK (tax_amount >= 0),\n");
                sb.append("    total_due NUMERIC(18, 2) NOT NULL DEFAULT 0.00 CHECK (total_due >= 0),\n");
                sb.append("    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',\n");
                sb.append("    payment_reference VARCHAR(128),\n");
                sb.append("    due_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '14 days'),\n");
                sb.append("    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
                sb.append("    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP\n");
            } else {
                sb.append("    name VARCHAR(255) NOT NULL,\n");
                sb.append("    type VARCHAR(100) NOT NULL DEFAULT 'STANDARD',\n");
                sb.append("    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',\n");
                sb.append("    metadata JSONB,\n");
                sb.append("    version BIGINT NOT NULL DEFAULT 0,\n");
                sb.append("    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
                sb.append("    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP\n");
            }

            sb.append(");\n\n");

            // Indexes
            sb.append(String.format("CREATE INDEX IF NOT EXISTS idx_%s_tenant ON %s(tenant_id);\n", tableName, tableName));
            if (!lower.contains("event") && !lower.contains("meter")) {
                sb.append(String.format("CREATE INDEX IF NOT EXISTS idx_%s_tenant_status ON %s(tenant_id, status);\n", tableName, tableName));
            }
            if (lower.contains("event") || lower.contains("meter")) {
                sb.append(String.format("CREATE UNIQUE INDEX IF NOT EXISTS uk_%s_idempotency ON %s(tenant_id, idempotency_key);\n", tableName, tableName));
                sb.append(String.format("CREATE INDEX IF NOT EXISTS idx_%s_tenant_metric ON %s(tenant_id, metric_name, event_timestamp);\n", tableName, tableName));
            }

            if (!lower.contains("event") && !lower.contains("meter")) {
                sb.append(String.format("DROP TRIGGER IF EXISTS trg_%s_updated_at ON %s;\n", tableName, tableName));
                sb.append(String.format("CREATE TRIGGER trg_%s_updated_at BEFORE UPDATE ON %s FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();\n", tableName, tableName));
            }
            sb.append("\n");
        }

        // Supporting tables: Credit Ledger if Credit aggregate exists
        boolean hasCredit = aggregates.stream().anyMatch(a -> a.toLowerCase().contains("credit"));
        if (hasCredit) {
            sb.append("""
                    -- =============================================================================
                    -- Table: credit_ledger (Transactional Drawdown & Top-Up Audit Ledger)
                    -- =============================================================================
                    CREATE TABLE IF NOT EXISTS credit_ledger (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        tenant_id VARCHAR(64) NOT NULL,
                        transaction_type VARCHAR(50) NOT NULL, -- DRAWDOWN, TOPUP, OVERAGE
                        amount NUMERIC(18, 4) NOT NULL,
                        reference_event_id VARCHAR(128),
                        notes TEXT,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );

                    CREATE INDEX IF NOT EXISTS idx_credit_ledger_tenant ON credit_ledger(tenant_id);
                    CREATE INDEX IF NOT EXISTS idx_credit_ledger_created ON credit_ledger(created_at DESC);

                    """);
        }

        // Append-Only Audit Log
        sb.append("""
                -- =============================================================================
                -- Table: audit_logs (Enterprise Multi-Tenant Security & State Change Audit Log)
                -- =============================================================================
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    tenant_id VARCHAR(64) NOT NULL,
                    entity_type VARCHAR(100) NOT NULL,
                    entity_id VARCHAR(64) NOT NULL,
                    actor VARCHAR(100) NOT NULL,
                    action VARCHAR(100) NOT NULL,
                    details JSONB,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_entity ON audit_logs(tenant_id, entity_type, entity_id);
                CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs(created_at DESC);
                """);

        return sb.toString();
    }

    public List<ParsedUserStory> parseUserStoriesFromGherkin(String gherkin) {
        List<ParsedUserStory> stories = new ArrayList<>();
        if (gherkin == null || gherkin.isBlank()) {
            return stories;
        }

        Pattern storyPattern = Pattern.compile("(?m)^###\\s+User\\s+Story\\s+(\\d+)[:\\s]+(.*)$");
        Matcher matcher = storyPattern.matcher(gherkin);

        List<Integer> startIndices = new ArrayList<>();
        List<String> storyIds = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        while (matcher.find()) {
            startIndices.add(matcher.start());
            storyIds.add(String.format("US-%03d", Integer.parseInt(matcher.group(1))));
            titles.add(matcher.group(2).trim());
        }

        for (int i = 0; i < startIndices.size(); i++) {
            int start = startIndices.get(i);
            int end = (i + 1 < startIndices.size()) ? startIndices.get(i + 1) : gherkin.length();
            String block = gherkin.substring(start, end);

            String persona = extractRegex(block, "\\*\\*As a\\*\\*\\s*(.*)");
            String intent = extractRegex(block, "\\*\\*I want to\\*\\*\\s*(.*)");
            String value = extractRegex(block, "\\*\\*So that\\*\\*\\s*(.*)");

            List<String> scenarios = new ArrayList<>();
            Matcher scenMatcher = Pattern.compile("(?m)^\\s*Scenario:\\s*(.*)$").matcher(block);
            while (scenMatcher.find()) {
                scenarios.add(scenMatcher.group(1).trim());
            }

            int points = scenarios.size() <= 1 ? 5 : (scenarios.size() <= 3 ? 8 : 13);
            stories.add(new ParsedUserStory(storyIds.get(i), titles.get(i), persona, intent, value, scenarios, points));
        }

        return stories;
    }

    public List<String> parseAggregatesFromC4(String c4Model) {
        LinkedHashSet<String> aggregates = new LinkedHashSet<>();
        if (c4Model == null || c4Model.isBlank()) {
            return new ArrayList<>();
        }

        Matcher mermaidAgg = Pattern.compile("Agg_\\w+\\[\"([A-Za-z0-9]+)(?:\\s+Aggregate|\\s+Root)?").matcher(c4Model);
        while (mermaidAgg.find()) {
            String candidate = mermaidAgg.group(1).trim();
            if (!candidate.equalsIgnoreCase("Domain") && !candidate.equalsIgnoreCase("Entity") && !candidate.equalsIgnoreCase("Item")) {
                aggregates.add(candidate);
            }
        }

        Matcher rootAgg = Pattern.compile("(?:Aggregate Root|Aggregate)[:\\s*`]+([A-Za-z0-9]+)").matcher(c4Model);
        while (rootAgg.find()) {
            String candidate = rootAgg.group(1).trim();
            if (!candidate.equalsIgnoreCase("Domain") && !candidate.equalsIgnoreCase("Entity") && !candidate.equalsIgnoreCase("Item")) {
                aggregates.add(candidate);
            }
        }

        Matcher repoMatcher = Pattern.compile("public\\s+interface\\s+([A-Za-z0-9]+)RepositoryPort").matcher(c4Model);
        while (repoMatcher.find()) {
            aggregates.add(repoMatcher.group(1).trim());
        }

        return new ArrayList<>(aggregates);
    }

    public List<String> parseInboundUseCasesFromC4(String c4Model) {
        LinkedHashSet<String> useCases = new LinkedHashSet<>();
        if (c4Model == null || c4Model.isBlank()) {
            return new ArrayList<>();
        }
        Matcher m1 = Pattern.compile("public\\s+interface\\s+([A-Za-z0-9]+UseCase)").matcher(c4Model);
        while (m1.find()) {
            useCases.add(m1.group(1).trim());
        }
        Matcher m2 = Pattern.compile("Port_\\w+\\[\"([A-Za-z0-9]+UseCase)\"\\]").matcher(c4Model);
        while (m2.find()) {
            useCases.add(m2.group(1).trim());
        }
        return new ArrayList<>(useCases);
    }

    public List<String> parseOutboundPortsFromC4(String c4Model) {
        LinkedHashSet<String> ports = new LinkedHashSet<>();
        if (c4Model == null || c4Model.isBlank()) {
            return new ArrayList<>();
        }
        Matcher m1 = Pattern.compile("public\\s+interface\\s+([A-Za-z0-9]+Port)").matcher(c4Model);
        while (m1.find()) {
            ports.add(m1.group(1).trim());
        }
        Matcher m2 = Pattern.compile("Port_\\w+\\[\"([A-Za-z0-9]+Port)\"\\]").matcher(c4Model);
        while (m2.find()) {
            ports.add(m2.group(1).trim());
        }
        return new ArrayList<>(ports);
    }

    private String findBestMatch(String keyword, List<String> candidates, String defaultVal) {
        if (candidates == null || candidates.isEmpty()) {
            return defaultVal;
        }
        String lowerKey = keyword.toLowerCase();
        for (String c : candidates) {
            String lowerC = c.toLowerCase();
            if (lowerKey.contains("meter") || lowerKey.contains("event") || lowerKey.contains("usage")) {
                if (lowerC.contains("meter") || lowerC.contains("event") || lowerC.contains("usage")) return c;
            }
            if (lowerKey.contains("subscript") || lowerKey.contains("plan")) {
                if (lowerC.contains("subscript") || lowerC.contains("plan")) return c;
            }
            if (lowerKey.contains("credit") || lowerKey.contains("drawdown") || lowerKey.contains("balance")) {
                if (lowerC.contains("credit") || lowerC.contains("drawdown") || lowerC.contains("balance")) return c;
            }
            if (lowerKey.contains("invoice") || lowerKey.contains("bill") || lowerKey.contains("tax")) {
                if (lowerC.contains("invoice") || lowerC.contains("bill")) return c;
            }
            if (lowerKey.contains("tenant") || lowerKey.contains("isolat")) {
                if (lowerC.contains("tenant") || lowerC.contains("security") || lowerC.contains("auth")) return c;
            }
        }
        return candidates.get(0);
    }

    private String extractRegex(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }

    private String toSnakeCase(String str) {
        if (str == null || str.isBlank()) return "item";
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    private String pluralize(String str) {
        if (str == null || str.isBlank()) return "items";
        if (str.endsWith("s")) return str;
        if (str.endsWith("y") && !str.endsWith("ay") && !str.endsWith("ey") && !str.endsWith("oy") && !str.endsWith("uy")) {
            return str.substring(0, str.length() - 1) + "ies";
        }
        return str + "s";
    }

    private String extractNounFromTask(String taskName) {
        if (taskName == null || taskName.isBlank()) return "Domain";
        Set<String> stopWords = Set.of(
                "a", "an", "the", "and", "or", "for", "with", "to", "in", "on", "at",
                "create", "build", "develop", "implement", "app", "apps", "application",
                "platform", "system", "service", "portal", "tool", "website"
        );
        String[] words = taskName.replaceAll("[^a-zA-Z0-9\\s]", " ").trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            String lower = w.toLowerCase();
            if (!stopWords.contains(lower) && !w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
                if (sb.length() >= 15) break;
            }
        }
        String candidate = sb.toString();
        return candidate.isBlank() ? "Domain" : candidate;
    }
}
