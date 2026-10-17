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
public class Phase5DevImplementationAgent implements PhaseAgentHandler {

    private static final Logger log = LoggerFactory.getLogger(Phase5DevImplementationAgent.class);

    private final ChatLanguageModel chatLanguageModel;
    private final SdlcJournalService journalService;
    private final ProjectScaffoldingService scaffoldingService;
    private final SkillLoaderService skillLoaderService;

    public Phase5DevImplementationAgent(
            ChatLanguageModel chatLanguageModel,
            SdlcJournalService journalService,
            ProjectScaffoldingService scaffoldingService,
            SkillLoaderService skillLoaderService) {
        this.chatLanguageModel = chatLanguageModel;
        this.journalService = journalService;
        this.scaffoldingService = scaffoldingService;
        this.skillLoaderService = skillLoaderService;
    }

    @Override
    public WorkflowState getHandledPhase() {
        return WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION;
    }

    @Override
    public String getAgentRole() {
        return "Dev Engineer Agent";
    }

    @Override
    public String getInstalledSkills() {
        return "spring-boot-enterprise, typescript-fullstack, clean-architecture";
    }

    @Override
    public String getPatternsAndTechniques() {
        return "Hexagonal Domain Implementation & RFC 7807 Error Handling";
    }

    @Override
    public WorkflowState getNextPhaseOnSuccess() {
        return WorkflowState.PHASE_5_WAITING_HUMAN_APPROVAL;
    }

    @Override
    public boolean isAutoAdvance() {
        return false; // Pauses for Human Code Implementation Sign-Off Gate
    }

    @Override
    public PhaseResult execute(
            WorkflowInstance workflow,
            PhaseContext context,
            java.util.function.Consumer<PhaseExecutionManager.ProgressUpdate> progress) throws Exception {

        String projectId = workflow.getProjectId();
        String taskName = workflow.getTaskName();

        // Single Source of Truth: Consume TDD plan & specs and C4 architectural model
        String tddSpecs = journalService.readArtifactContent(projectId, "tests/TDD_PLAN_AND_SPECS.md");
        String c4Model = journalService.readArtifactContent(projectId, "architecture/C4_MODEL.md");
        String revisionNotes = context.getRevisionFeedback();
        String specs = (!tddSpecs.isBlank() || !c4Model.isBlank())
                ? "### Target TDD Specifications (from Phase 4):\n" + tddSpecs +
                  (!c4Model.isBlank() ? "\n\n### Hexagonal Architecture & Port Contracts (from Phase 1):\n" + c4Model : "")
                : (workflow.getDescription() != null ? workflow.getDescription() : taskName);

        if (revisionNotes != null && !revisionNotes.isBlank()) {
            specs = String.format("""
                    ================================================================================
                    ⚠️ MANDATORY REVISION DIRECTIVE FROM LEAD SUPERVISOR:
                    The human supervisor has reviewed previous outputs and returned this phase for refinement with the following feedback:
                    "%s"
                    You MUST strictly incorporate and reflect every requested framework, library, protocol, and feature (e.g. Netflix DGS GraphQL, Apollo GraphQL, Mockito, JaCoCo, Kafka, MySQL) into the Implementation Summary and Scaffolded Code!
                    ================================================================================

                    %s
                    """, revisionNotes, specs);
        }

        progress.accept(new PhaseExecutionManager.ProgressUpdate(25,
                "Implementing Hexagonal Domain Core & Inbound UseCases..."));
        progress.accept(new PhaseExecutionManager.ProgressUpdate(60,
                "Scaffolding Outbound Persistence Adapters, Spring Boot REST Controllers, and Next.js UI..."));

        generateImplementationSummaryFile(projectId, taskName, specs);
        scaffoldingService.scaffoldSourceCode(projectId, taskName, specs);

        ProjectScaffoldingService.DomainProfile profile = scaffoldingService.resolveDomainProfile(projectId, taskName, specs);
        String mainAppPath = "src/backend/src/main/java/" + profile.packageDir() + "/" + profile.applicationClassName() + ".java";

        return PhaseResult.success(
                WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION,
                "Materialized specifications into clean architecture code decoupled from persistence layer for task: " + taskName,
                "Spring Boot 3 backend ('src/backend/') and Next.js 14 frontend ('src/frontend/') scaffolded successfully.",
                List.of("src/backend/pom.xml", mainAppPath, "src/frontend/package.json", "src/frontend/app/page.tsx", "src/IMPLEMENTATION_SUMMARY.md", "docker-compose.yml"),
                System.currentTimeMillis() - context.getStartTime().toEpochMilli());
    }

    private void generateImplementationSummaryFile(String projectId, String taskName, String description) {
        try {
            Path base = journalService.resolveProjectBase(projectId);
            Path srcDir = base.resolve("src");
            Files.createDirectories(srcDir);
            Path filePath = srcDir.resolve("IMPLEMENTATION_SUMMARY.md");

            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("spring-boot-enterprise, clean-architecture, typescript-fullstack");

            String systemPromptText = "You are a Lead Dev Engineer implementing Spring Boot 3 and Clean Architecture. Output pure markdown directly without preamble.";
            String userPromptText = String.format("""
                    You are a Lead Dev Engineer in an enterprise platform implementing Clean Architecture and Spring Boot 3.
                    Generate an Implementation Summary and Code Scaffold document for:

                    Project ID: %s
                    Task Name: %s
                    Requirements Description:
                    %s
                    %s

                    Format the output strictly as Markdown:
                    # Implementation Summary & Code Scaffold (Phase 5 Output)
                    **Project ID:** %s
                    **Task:** %s
                    **Developer Agent:** Dev Engineer Agent
                    **Status:** IMPLEMENTATION_COMPLETED

                    ---

                    ## 1. Architecture Implementation Overview
                    (Summarize implemented domain entities, inbound usecase ports, and outbound adapters)

                    ## 2. Implemented REST Endpoints & Error Handling
                    (List Spring Boot REST controllers, RFC 7807 ProblemDetail handlers, and DTO mappings)

                    ## 3. Persistence & Concurrency Strategy
                    (Explain JPA repository integration, optimistic locking, and transaction demarcations)
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
                log.warn("LLM Implementation summary fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackImplementationSummary(projectId, taskName, description);
            }

            String auditTrace = String.format("""

                    ---

                    ## 4. Dev Engineer Inference Audit Trace
                    - **Agent Role:** Dev Engineer Agent
                    - **Model / Engine:** `%s`
                    - **Inference Latency:** `%d ms`
                    - **Status:** %s
                    - **Audit Ledger:** Recorded in [`../inferences/phase5-dev-engineer.md`](../inferences/phase5-dev-engineer.md)
                    """,
                    isFallback ? "Deterministic Domain Draft Fallback" : "Gemini 3.5 Flash",
                    duration,
                    isFallback ? "FALLBACK_DRAFT" : "LLM_GENERATED");

            Files.writeString(filePath, content + auditTrace);
            log.info("Successfully generated IMPLEMENTATION_SUMMARY.md for project: {}", projectId);

            journalService.recordPromptAndInference(
                    projectId, "PHASE_5_DEVELOPMENT_IMPLEMENTATION", "Dev Engineer Agent",
                    "Implementation Summary & Code Scaffold", systemPromptText, userPromptText, content, duration, isFallback);

        } catch (Exception e) {
            log.error("Failed to generate implementation summary for project {}: {}", projectId, e.getMessage());
        }
    }

    private String buildFallbackImplementationSummary(String projectId, String taskName, String description) {
        return String.format("""
                # Implementation Summary & Code Scaffold (Phase 5 Output)
                **Project ID:** %s
                **Task:** %s
                **Developer Agent:** Dev Engineer Agent
                **Status:** IMPLEMENTATION_COMPLETED

                ---

                ## 1. Architecture Implementation Overview
                - **Domain Core**: Pure Java aggregate model implementing business rules for %s without external framework bindings.
                - **Inbound Port**: `ManageUseCase` port defining contract boundaries for commands and queries.
                - **Outbound Port**: `RepositoryPort` defining persistence operations decoupled from database technology.
                - **Application Service**: Concrete orchestrator managing transaction boundaries and port wiring.

                ---

                ## 2. Implemented REST Endpoints & Error Handling
                - **Controller**: `@RestController` exposing endpoints under `/api/...`.
                - **RFC 7807 ProblemDetail**: Handled via global `@ControllerAdvice` mapping domain exceptions to RFC 7807 responses.
                - **Validation**: Jakarta `@Valid` constraints active on request payloads.

                ---

                ## 3. Persistence & Concurrency Strategy
                - **Database Engine**: PostgreSQL 16 with connection pooling managed by HikariCP.
                - **Optimistic Locking**: `@Version` column prevents lost updates in concurrent transactions.
                - **Transaction Demarcation**: Handled via Spring declarative `@Transactional` boundaries.
                """, projectId, taskName, taskName);
    }
}
