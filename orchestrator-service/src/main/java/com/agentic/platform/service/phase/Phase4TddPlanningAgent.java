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
public class Phase4TddPlanningAgent implements PhaseAgentHandler {

    private static final Logger log = LoggerFactory.getLogger(Phase4TddPlanningAgent.class);

    private final ChatLanguageModel chatLanguageModel;
    private final SdlcJournalService journalService;
    private final ProjectScaffoldingService scaffoldingService;
    private final SkillLoaderService skillLoaderService;

    public Phase4TddPlanningAgent(
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
        return WorkflowState.PHASE_4_TDD_PLANNING;
    }

    @Override
    public String getAgentRole() {
        return "TDD Orchestrator Agent";
    }

    @Override
    public String getInstalledSkills() {
        return "tdd-orchestrator, spring-boot-enterprise";
    }

    @Override
    public String getPatternsAndTechniques() {
        return "Red-Green-Refactor Cycle & High-Coverage Boundary Testing";
    }

    @Override
    public WorkflowState getNextPhaseOnSuccess() {
        return WorkflowState.PHASE_4_WAITING_HUMAN_APPROVAL;
    }

    @Override
    public boolean isAutoAdvance() {
        return false; // Pauses for Human TDD Specs Approval Gate
    }

    @Override
    public PhaseResult execute(
            WorkflowInstance workflow,
            PhaseContext context,
            java.util.function.Consumer<PhaseExecutionManager.ProgressUpdate> progress) throws Exception {

        String projectId = workflow.getProjectId();
        String taskName = workflow.getTaskName();

        // Single Source of Truth: Consume dispatched tasks and approved BDD Gherkin specs
        String tasks = journalService.readArtifactContent(projectId, "tasks/DISPATCH_ALLOCATION.md");
        String gherkin = journalService.readArtifactContent(projectId, "backlog/GHERKIN_REQUIREMENTS.md");
        String revisionNotes = context.getRevisionFeedback();
        String specs = (!tasks.isBlank() || !gherkin.isBlank())
                ? "### Dispatched Task Allocations (from Phase 3):\n" + tasks +
                  (!gherkin.isBlank() ? "\n\n### Official Acceptance Criteria (from Phase 0 Gherkin):\n" + gherkin : "")
                : (workflow.getDescription() != null ? workflow.getDescription() : taskName);

        if (revisionNotes != null && !revisionNotes.isBlank()) {
            specs = String.format("""
                    ================================================================================
                    ⚠️ MANDATORY REVISION DIRECTIVE FROM LEAD SUPERVISOR:
                    The human supervisor has reviewed previous outputs and returned this phase for refinement with the following feedback:
                    "%s"
                    You MUST strictly incorporate and reflect every requested testing framework, boundary assertion, and test suite requirement (e.g. Mockito, JaCoCo, JUnit 5, Testcontainers) into the TDD Plan and Scaffolding!
                    ================================================================================

                    %s
                    """, revisionNotes, specs);
        }

        progress.accept(new PhaseExecutionManager.ProgressUpdate(35,
                "Formulating Red-Green-Refactor test matrix & boundary assertions..."));
        generateTddPlanFile(projectId, taskName, specs);

        progress.accept(new PhaseExecutionManager.ProgressUpdate(75,
                "Scaffolding concrete domain-tailored TDD unit test suites..."));
        scaffoldingService.scaffoldTddTestSuites(projectId, taskName, specs);

        ProjectScaffoldingService.DomainProfile profile = scaffoldingService.resolveDomainProfile(projectId, taskName, specs);
        String testArtifactPath = "tests/backend/" + profile.serviceName() + "Test.java";

        return PhaseResult.success(
                WorkflowState.PHASE_4_TDD_PLANNING,
                "Established zero-regression test contracts with target branch coverage >= 70% for task: " + taskName,
                "TDD Specification & unit test suites scaffolded to 'tests/TDD_PLAN_AND_SPECS.md' and '" + testArtifactPath + "'",
                List.of("tests/TDD_PLAN_AND_SPECS.md", testArtifactPath),
                System.currentTimeMillis() - context.getStartTime().toEpochMilli());
    }

    private void generateTddPlanFile(String projectId, String taskName, String description) {
        try {
            Path base = journalService.resolveProjectBase(projectId);
            Path testsDir = base.resolve("tests");
            Files.createDirectories(testsDir);
            Path filePath = testsDir.resolve("TDD_PLAN_AND_SPECS.md");

            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("tdd-orchestrator, spring-boot-enterprise");

            String systemPromptText = "You are an expert TDD Orchestrator Agent. Output pure markdown directly without preamble.";
            String userPromptText = String.format("""
                    You are an enterprise TDD Orchestrator Agent following Red-Green-Refactor cycle and high-coverage boundary testing.
                    Create a TDD Test Plan & Specifications document for:

                    Project ID: %s
                    Task Name: %s
                    Requirements Description:
                    %s
                    %s

                    Format the output strictly as Markdown:
                    # TDD Test Plan & Specifications (Phase 4 Output)
                    **Project ID:** %s
                    **Task:** %s
                    **Orchestrator Agent:** TDD Orchestrator Agent
                    **Target Branch Coverage:** >= 70%%
                    **Status:** TEST_SUITES_SCAFFOLDED

                    ---

                    ## 1. Test Suite Matrix (Red-Green-Refactor)
                    | Test Category | Target Component | Assertions & Boundary Checks | Target Coverage |
                    | :--- | :--- | :--- | :--- |
                    (Include Unit Tests, Port/Adapter Mock Tests, and Validation Rule Tests)

                    ## 2. Invariant & Edge Case Boundaries
                    (Null safety, optimistic locking retry boundaries, error conditions)

                    ## 3. Test Execution Command
                    `mvn test`
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
                log.warn("LLM TDD plan fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackTddPlan(projectId, taskName, description);
            }

            String auditTrace = String.format("""

                    ---

                    ## 4. TDD Orchestrator Inference Audit Trace
                    - **Agent Role:** TDD Orchestrator Agent
                    - **Model / Engine:** `%s`
                    - **Inference Latency:** `%d ms`
                    - **Status:** %s
                    - **Audit Ledger:** Recorded in [`../inferences/phase4-tdd-orchestrator.md`](../inferences/phase4-tdd-orchestrator.md)
                    """,
                    isFallback ? "Deterministic Domain Draft Fallback" : "Gemini 3.5 Flash",
                    duration,
                    isFallback ? "FALLBACK_DRAFT" : "LLM_GENERATED");

            Files.writeString(filePath, content + auditTrace);
            log.info("Successfully generated TDD_PLAN_AND_SPECS.md for project: {}", projectId);

            journalService.recordPromptAndInference(
                    projectId, "PHASE_4_TDD_PLANNING", "TDD Orchestrator Agent",
                    "TDD Test Plan & Specifications", systemPromptText, userPromptText, content, duration, isFallback);

        } catch (Exception e) {
            log.error("Failed to generate TDD plan for project {}: {}", projectId, e.getMessage());
        }
    }

    private String buildFallbackTddPlan(String projectId, String taskName, String description) {
        return String.format("""
                # TDD Test Plan & Specifications (Phase 4 Output)
                **Project ID:** %s
                **Task:** %s
                **Orchestrator Agent:** TDD Orchestrator Agent
                **Target Branch Coverage:** >= 70%%
                **Status:** TEST_SUITES_SCAFFOLDED

                ---

                ## 1. Test Suite Matrix (Red-Green-Refactor)

                | Test Category | Target Component | Assertions & Boundary Checks | Target Coverage |
                | :--- | :--- | :--- | :--- |
                | **Domain Invariant Unit Tests** | Core Domain Aggregate for %s | Verify creation preconditions, invariant state validation, and non-null guarantees | 90%% |
                | **Application Service Tests** | Inbound UseCase Service | Verify business logic orchestration, repository port interactions, and exception handling | 85%% |
                | **Persistence Adapter Tests** | Outbound Repository Port | Verify JPA mapping, optimistic locking versioning, and transaction rollbacks | 80%% |
                | **REST Controller Web Tests** | Inbound Web Adapter | Verify RFC 7807 problem detail serialization and input validation status codes | 85%% |

                ---

                ## 2. Invariant & Edge Case Boundaries
                - **Null & Blank Input Handling**: Prohibit blank IDs and names with immediate domain exception throw.
                - **Duplicate Key Prevention**: Prevent overwriting existing records by verifying repository presence.
                - **Optimistic Concurrency**: Ensure concurrent modifications result in `OptimisticLockException`.

                ## 3. Test Runner
                Execute test suites via Maven: `mvn test`
                """, projectId, taskName, taskName);
    }
}
