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
public class Phase3TaskDispatchingAgent implements PhaseAgentHandler {

    private static final Logger log = LoggerFactory.getLogger(Phase3TaskDispatchingAgent.class);

    private final ChatLanguageModel chatLanguageModel;
    private final SdlcJournalService journalService;
    private final SkillLoaderService skillLoaderService;

    public Phase3TaskDispatchingAgent(
            ChatLanguageModel chatLanguageModel,
            SdlcJournalService journalService,
            SkillLoaderService skillLoaderService) {
        this.chatLanguageModel = chatLanguageModel;
        this.journalService = journalService;
        this.skillLoaderService = skillLoaderService;
    }

    @Override
    public WorkflowState getHandledPhase() {
        return WorkflowState.PHASE_3_TASK_DISPATCHING;
    }

    @Override
    public String getAgentRole() {
        return "Task Dispatcher Agent";
    }

    @Override
    public String getInstalledSkills() {
        return "clean-architecture, c4-model-designer";
    }

    @Override
    public String getPatternsAndTechniques() {
        return "Team Topologies & Port/Adapter Modular Boundaries";
    }

    @Override
    public WorkflowState getNextPhaseOnSuccess() {
        return WorkflowState.PHASE_3_WAITING_HUMAN_APPROVAL;
    }

    @Override
    public boolean isAutoAdvance() {
        return false; // Pauses for Human Task Dispatch Approval Gate
    }

    @Override
    public PhaseResult execute(
            WorkflowInstance workflow,
            PhaseContext context,
            java.util.function.Consumer<PhaseExecutionManager.ProgressUpdate> progress) throws Exception {

        String projectId = workflow.getProjectId();
        String taskName = workflow.getTaskName();

        // Single Source of Truth: Consume technical user stories and C4 component architecture
        String stories = journalService.readArtifactContent(projectId, "backlog/TECHNICAL_USER_STORIES.md");
        String c4Model = journalService.readArtifactContent(projectId, "architecture/C4_MODEL.md");
        String revisionNotes = context.getRevisionFeedback();
        String specs = (!stories.isBlank() || !c4Model.isBlank())
                ? "### Technical User Stories (from Phase 2 Backlog):\n" + stories +
                  (!c4Model.isBlank() ? "\n\n### C4 Architecture & Ports (from Phase 1):\n" + c4Model : "")
                : (workflow.getDescription() != null ? workflow.getDescription() : taskName);

        if (revisionNotes != null && !revisionNotes.isBlank()) {
            specs = String.format("""
                    ================================================================================
                    ⚠️ MANDATORY REVISION DIRECTIVE FROM LEAD SUPERVISOR:
                    The human supervisor has reviewed previous outputs and returned this phase for refinement with the following feedback:
                    "%s"
                    You MUST strictly incorporate and reflect every requested requirement, team stream, and component into the Task Dispatch Allocation Matrix!
                    ================================================================================

                    %s
                    """, revisionNotes, specs);
        }

        progress.accept(new PhaseExecutionManager.ProgressUpdate(30,
                "Analyzing domain dependencies & allocating stream-aligned team topology..."));
        generateDispatchAllocationFile(projectId, taskName, specs);

        progress.accept(new PhaseExecutionManager.ProgressUpdate(80,
                "Defining interface port boundaries & dispatching to development streams..."));

        return PhaseResult.success(
                WorkflowState.PHASE_3_TASK_DISPATCHING,
                "Partitioned core domain, inbound/outbound ports, and stream-aligned team assignments for task: " + taskName,
                "Team Topology Dispatch Matrix generated to 'tasks/DISPATCH_ALLOCATION.md'",
                List.of("tasks/DISPATCH_ALLOCATION.md"),
                System.currentTimeMillis() - context.getStartTime().toEpochMilli());
    }

    private void generateDispatchAllocationFile(String projectId, String taskName, String description) {
        try {
            Path base = journalService.resolveProjectBase(projectId);
            Path tasksDir = base.resolve("tasks");
            Files.createDirectories(tasksDir);
            Path filePath = tasksDir.resolve("DISPATCH_ALLOCATION.md");

            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("clean-architecture, c4-model-designer");

            String systemPromptText = "You are an expert Task Dispatcher Agent following Clean Architecture and Team Topologies. Output pure markdown directly without preamble.";
            String userPromptText = String.format("""
                    You are an enterprise Task Dispatcher Agent following Clean Architecture (Hexagonal Ports & Adapters) and Team Topologies.
                    Create a concrete, fully populated modular task allocation and stream-aligned team dispatch document for:

                    Project ID: %s
                    Task Name: %s
                    Requirements Description:
                    %s
                    %s

                    Format the output strictly as Markdown:
                    # Task Dispatch Allocation & Team Topologies (Phase 3 Output)
                    **Project ID:** %s
                    **Task:** %s
                    **Dispatcher Agent:** Task Dispatcher Agent
                    **Pattern:** Hexagonal Inbound/Outbound Port Dispatching & Team Topologies
                    **Status:** DISPATCHED_TO_ENGINEERING

                    ---

                    ## 1. Stream-Aligned Team Allocation Matrix
                    | Stream / Team | Component Responsibility | Inbound Port | Outbound Port | Target Module |
                    | :--- | :--- | :--- | :--- | :--- |
                    (Provide concrete rows tailored to the domain: Core Domain Stream, Inbound Web Adapter Stream, Outbound Persistence Stream, Security & Audit Stream)

                    ## 2. Module Boundary Isolation & Inversion of Control
                    (Define concrete interface specifications and dependency injection boundaries)

                    ## 3. Dispatch Checklist & Execution Sequence
                    (Step-by-step engineering sequence for TDD and Implementation agents)
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

                // Guard against blank or broken LLM output
                if (content.isBlank() || content.lines().count() < 5 || content.startsWith("---")) {
                    log.warn("LLM Dispatch allocation produced insufficient content, falling back to rich domain template.");
                    content = buildFallbackDispatchAllocation(projectId, taskName, description);
                    isFallback = true;
                }
            } catch (Exception ex) {
                duration = System.currentTimeMillis() - start;
                isFallback = true;
                log.warn("LLM Dispatch allocation fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackDispatchAllocation(projectId, taskName, description);
            }

            String auditTrace = String.format("""

                    ---

                    ## 4. Task Dispatcher Inference Audit Trace
                    - **Agent Role:** Task Dispatcher Agent
                    - **Model / Engine:** `%s`
                    - **Inference Latency:** `%d ms`
                    - **Status:** %s
                    - **Audit Ledger:** Recorded in [`../inferences/phase3-task-dispatcher.md`](../inferences/phase3-task-dispatcher.md)
                    """,
                    isFallback ? "Deterministic Domain Draft Fallback" : "Gemini 3.5 Flash",
                    duration,
                    isFallback ? "FALLBACK_DRAFT" : "LLM_GENERATED");

            Files.writeString(filePath, content + auditTrace);
            log.info("Successfully generated DISPATCH_ALLOCATION.md for project: {}", projectId);

            journalService.recordPromptAndInference(
                    projectId, "PHASE_3_TASK_DISPATCHING", "Task Dispatcher Agent",
                    "Task Dispatch Allocation & Team Topologies", systemPromptText, userPromptText, content, duration, isFallback);

        } catch (Exception e) {
            log.error("Failed to generate task dispatch allocation for project {}: {}", projectId, e.getMessage());
        }
    }

    private String buildFallbackDispatchAllocation(String projectId, String taskName, String description) {
        return String.format("""
                # Task Dispatch Allocation & Team Topologies (Phase 3 Output)
                **Project ID:** %s
                **Task:** %s
                **Dispatcher Agent:** Task Dispatcher Agent
                **Pattern:** Hexagonal Inbound/Outbound Port Dispatching & Team Topologies
                **Status:** DISPATCHED_TO_ENGINEERING

                ---

                ## 1. Stream-Aligned Team Allocation Matrix

                | Stream / Team | Component Responsibility | Inbound Port | Outbound Port | Target Module |
                | :--- | :--- | :--- | :--- | :--- |
                | **Core Domain Stream** | Business invariant validation, aggregate lifecycle, and state transitions for %s | `Manage%sUseCase` | `%sRepositoryPort` | `src/backend/.../domain` |
                | **Inbound Web Adapter Stream** | Spring Boot 3.3 REST Controllers, RFC 7807 Error Handlers, and DTO validation | REST Controller | Inbound Domain Port | `src/backend/.../adapter/in` |
                | **Outbound Persistence Stream** | PostgreSQL 16 JPA Repositories, Spring Data integration, and `@Version` optimistic locking | Outbound Repository Port | JDBC / Postgres Adapter | `src/backend/.../adapter/out` |
                | **Security & Governance Stream** | STRIDE mitigation, RBAC policies, and immutable append-only transition logging | Security Context Filter | TransitionLogPort | `src/backend/.../config` |
                | **Frontend Experience Stream** | Next.js 14 App Router dashboard, responsive telemetry cards, and status streamer | Next.js Client | Backend REST API | `src/frontend/app` |

                ---

                ## 2. Module Boundary Isolation & Inversion of Control
                1. **Zero Framework Dependencies in Domain Core**:
                   The `domain` package must contain pure Java models without Spring, Hibernate, or Jakarta annotations.
                2. **Strict Inversion of Control**:
                   Application services implement inbound ports and consume outbound ports. Controllers and Repositories never reference each other directly.
                3. **Granular Interface Segregation**:
                   Ports are partitioned by functional responsibility (ISP) rather than coalesced into monolithic services.

                ---

                ## 3. Dispatch Checklist & Execution Sequence
                - [x] **Step 1 (Phase 3):** Team topology boundaries and port interfaces established.
                - [ ] **Step 2 (Phase 4):** TDD Orchestrator Agent scaffolds unit test suites enforcing >= 70%% branch coverage.
                - [ ] **Step 3 (Phase 5):** Dev Engineer Agent implements Hexagonal core, Spring Boot REST controllers, and Next.js frontend.
                - [ ] **Step 4 (Phase 6):** Triad Council Consortium conducts multi-agent review (Architect, AppSec, Performance).
                """, projectId, taskName, taskName,
                taskName.replaceAll("[^a-zA-Z0-9]", ""),
                taskName.replaceAll("[^a-zA-Z0-9]", ""));
    }
}
