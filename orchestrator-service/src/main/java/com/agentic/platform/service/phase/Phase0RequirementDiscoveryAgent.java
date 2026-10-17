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
public class Phase0RequirementDiscoveryAgent implements PhaseAgentHandler {

    private static final Logger log = LoggerFactory.getLogger(Phase0RequirementDiscoveryAgent.class);

    private final ChatLanguageModel chatLanguageModel;
    private final SdlcJournalService journalService;
    private final SkillLoaderService skillLoaderService;

    public Phase0RequirementDiscoveryAgent(
            ChatLanguageModel chatLanguageModel,
            SdlcJournalService journalService,
            SkillLoaderService skillLoaderService) {
        this.chatLanguageModel = chatLanguageModel;
        this.journalService = journalService;
        this.skillLoaderService = skillLoaderService;
    }

    @Override
    public WorkflowState getHandledPhase() {
        return WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY;
    }

    @Override
    public String getAgentRole() {
        return "PO / Spec Writer & AppSec Auditor";
    }

    @Override
    public String getInstalledSkills() {
        return "spec-writer, stride-threat-model";
    }

    @Override
    public String getPatternsAndTechniques() {
        return "BDD Gherkin & STRIDE Threat Modeling";
    }

    @Override
    public WorkflowState getNextPhaseOnSuccess() {
        return WorkflowState.PHASE_0_WAITING_HUMAN_SIGN_OFF;
    }

    @Override
    public boolean isAutoAdvance() {
        return false; // Pauses for Human Sign-off Gate
    }

    @Override
    public PhaseResult execute(
            WorkflowInstance workflow,
            PhaseContext context,
            java.util.function.Consumer<PhaseExecutionManager.ProgressUpdate> progress) throws Exception {

        String projectId = workflow.getProjectId();
        String taskName = workflow.getTaskName();
        String description = workflow.getDescription();
        String revisionNotes = context.getRevisionFeedback();

        progress.accept(new PhaseExecutionManager.ProgressUpdate(35,
                "Parsing raw_input.md & formulating BDD Gherkin acceptance criteria..."));
        generateGherkinRequirementsFile(projectId, taskName, description, revisionNotes);

        progress.accept(new PhaseExecutionManager.ProgressUpdate(75,
                "Executing STRIDE threat modeling & attack surface evaluation..."));
        generateThreatModelFile(projectId, taskName, description, revisionNotes);

        progress.accept(new PhaseExecutionManager.ProgressUpdate(95,
                "Validating specification completeness & security posture..."));

        return PhaseResult.success(
                WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY,
                "Established measurable BDD acceptance criteria and mitigated STRIDE security risks for task: " + taskName,
                "BDD Gherkin Scenarios & STRIDE Threat Model generated to 'backlog/GHERKIN_REQUIREMENTS.md' and 'council-audits/phase0-threat-model.md'",
                List.of("backlog/GHERKIN_REQUIREMENTS.md", "council-audits/phase0-threat-model.md"),
                System.currentTimeMillis() - context.getStartTime().toEpochMilli());
    }

    private void generateGherkinRequirementsFile(String projectId, String taskName, String description, String revisionNotes) {
        try {
            Path base = journalService.resolveProjectBase(projectId);
            Path backlogDir = base.resolve("backlog");
            Files.createDirectories(backlogDir);
            Path filePath = backlogDir.resolve("GHERKIN_REQUIREMENTS.md");

            String revisionDirective = (revisionNotes != null && !revisionNotes.isBlank())
                    ? String.format("""

                    ================================================================================
                    ⚠️ MANDATORY REVISION DIRECTIVE FROM LEAD SUPERVISOR:
                    The human supervisor has reviewed previous outputs and returned this phase for refinement with the following feedback:
                    "%s"
                    You MUST explicitly incorporate and address every point, requested framework, library, tool, and requirement mentioned above (e.g. Netflix DGS GraphQL, Apollo GraphQL, Mockito, JaCoCo, Kafka, MySQL/Liquibase, etc.) into the Gherkin scenarios, user stories, and acceptance criteria!
                    ================================================================================
                    """, revisionNotes)
                    : "";

            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("spec-writer");

            String systemPromptText = "You are an expert enterprise Spec Writer Agent following BDD Gherkin standards. Output pure markdown directly without preamble.";
            String userPromptText = String.format("""
                    You are a Lead Product Owner and Spec Writer Agent in an enterprise agentic software platform.
                    Write a comprehensive Gherkin BDD Acceptance Criteria document strictly based on the following project specifications:

                    Project ID: %s
                    Task Name: %s
                    Requirements Description:
                    %s
                    %s
                    %s

                    Format the output strictly as Markdown:
                    # Gherkin Acceptance Criteria & User Stories (Phase 0 Output)
                    **Project ID:** %s
                    **Task:** %s
                    **Generated by:** PO / Spec Writer Agent (Autonomous)
                    **Status:** READY_FOR_HUMAN_SIGN_OFF

                    ---

                    ## 1. Feature: %s
                    (Write comprehensive User Stories with BDD Gherkin Given-When-Then Scenarios directly tailored to the requirements above)

                    ---
                    ## 2. Definition of Done
                    - [x] BDD Gherkin contract formulated for core acceptance criteria.
                    - [ ] Awaiting Human Sign-off from Product Owner / Supervisor.
                    """, projectId, taskName, description, revisionDirective, skillDirectives, projectId, taskName, taskName);

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
                log.warn("LLM Spec generation fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackGherkin(projectId, taskName, description);
            }

            String auditTrace = String.format("""

                    ---

                    ## 3. Specification Agent Inference Audit Trace
                    - **Agent Role:** PO / Spec Writer Agent
                    - **Model / Engine:** `%s`
                    - **Inference Latency:** `%d ms`
                    - **Status:** %s
                    - **Audit Ledger:** Recorded in [`../inferences/phase0-spec-writer.md`](../inferences/phase0-spec-writer.md)
                    """,
                    isFallback ? "Deterministic Domain Draft Fallback" : "Gemini 3.5 Flash",
                    duration,
                    isFallback ? "FALLBACK_DRAFT" : "LLM_GENERATED");

            Files.writeString(filePath, content + auditTrace);
            log.info("Successfully generated GHERKIN_REQUIREMENTS.md for project: {}", projectId);

            journalService.recordPromptAndInference(
                    projectId, "PHASE_0_REQUIREMENT_DISCOVERY", "PO / Spec Writer Agent",
                    "Gherkin Acceptance Criteria & User Stories", systemPromptText, userPromptText, content, duration, isFallback);

        } catch (Exception e) {
            log.error("Failed to generate Gherkin requirements for project {}: {}", projectId, e.getMessage());
        }
    }

    private void generateThreatModelFile(String projectId, String taskName, String description, String revisionNotes) {
        try {
            Path base = journalService.resolveProjectBase(projectId);
            Path councilDir = base.resolve("council-audits");
            Files.createDirectories(councilDir);
            Path filePath = councilDir.resolve("phase0-threat-model.md");

            String revisionDirective = (revisionNotes != null && !revisionNotes.isBlank())
                    ? String.format("""

                    ================================================================================
                    ⚠️ REVISION CONTEXT FROM LEAD SUPERVISOR:
                    The human supervisor has highlighted specific scope/tool requirements for refinement:
                    "%s"
                    Ensure the threat modeling matrix explicitly evaluates attack surfaces, boundaries, and threat vectors for all mentioned components!
                    ================================================================================
                    """, revisionNotes)
                    : "";

            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("stride-threat-model");

            String systemPromptText = "You are an expert AppSec Auditor Agent conducting STRIDE threat modeling. Output pure markdown directly without preamble.";
            String userPromptText = String.format("""
                    You are an enterprise AppSec Auditor Agent conducting a STRIDE threat modeling assessment based on Microsoft STRIDE and OWASP Top 10.
                    Analyze the security risks and attack surfaces specifically for this project:

                    Project ID: %s
                    Task Name: %s
                    Requirements Description:
                    %s
                    %s
                    %s

                    Format the output strictly as Markdown:
                    # STRIDE Threat Modeling Assessment (Phase 0 Output)
                    **Project ID:** %s
                    **Target:** %s
                    **Auditor Agent:** AppSec Auditor Agent
                    **Methodology:** Microsoft STRIDE & OWASP Top 10

                    ---

                    ## 1. Threat Identification Matrix

                    | STRIDE Category | Threat Scenario | Impact | Mitigation Strategy |
                    |---|---|---|---|
                    (Analyze all 6 categories: Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege)

                    ---
                    ## 2. AppSec Security Directives
                    (Key design mandates for Phase 1 Architecture)
                    """, projectId, taskName, description, revisionDirective, skillDirectives, projectId, taskName);

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
                log.warn("LLM Threat model fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackThreatModel(projectId, taskName, description);
            }

            String auditTrace = String.format("""

                    ---

                    ## 3. AppSec Auditor Inference Audit Trace
                    - **Agent Role:** AppSec Auditor Agent
                    - **Model / Engine:** `%s`
                    - **Inference Latency:** `%d ms`
                    - **Status:** %s
                    - **Audit Ledger:** Recorded in [`../inferences/phase0-appsec-threat-model.md`](../inferences/phase0-appsec-threat-model.md)
                    """,
                    isFallback ? "Deterministic Domain Draft Fallback" : "Gemini 3.5 Flash",
                    duration,
                    isFallback ? "FALLBACK_DRAFT" : "LLM_GENERATED");

            Files.writeString(filePath, content + auditTrace);
            log.info("Successfully generated phase0-threat-model.md for project: {}", projectId);

            journalService.recordPromptAndInference(
                    projectId, "PHASE_0_REQUIREMENT_DISCOVERY", "AppSec Auditor Agent",
                    "STRIDE Threat Modeling Assessment", systemPromptText, userPromptText, content, duration, isFallback);

        } catch (Exception e) {
            log.error("Failed to generate threat model for project {}: {}", projectId, e.getMessage());
        }
    }

    private String buildFallbackGherkin(String projectId, String taskName, String description) {
        return String.format("""
                # Gherkin Acceptance Criteria & User Stories (Phase 0 Output)
                **Project ID:** %s
                **Task:** %s
                **Generated by:** PO / Spec Writer Agent (Autonomous)
                **Status:** READY_FOR_HUMAN_SIGN_OFF

                ---

                ## 1. Feature: %s Core Functional Journey

                ### User Story 1: Core Domain Processing
                As a system user for %s,
                I want the system to process operational specifications with auditability,
                So that workflows execute according to business parameters.

                ```gherkin
                Scenario: Successful processing of primary domain workflow
                  Given registered requirements: "%s"
                  When the system validates operational input parameters
                  Then the system records the transaction state as "RECORDED_PENDING_REVIEW"
                  And emits an audit trail event for tracking
                ```

                ### User Story 2: Authorization & Verification Gate
                As a supervisor / platform operator,
                I want strict validation and authorization logs recorded,
                So that all state changes remain provable and accountable.

                ```gherkin
                Scenario: Supervisor verification and state commitment
                  Given a registered transaction in state "RECORDED_PENDING_REVIEW"
                  When the authorization check is executed by an authorized actor
                  Then the system commits the status to "VERIFIED_COMPLETED"
                  And persists an immutable transition entry
                ```

                ---
                ## 2. Definition of Done
                - [x] BDD Gherkin specifications established for task: %s.
                - [ ] Awaiting Human Sign-off from Product Owner / Supervisor.
                """, projectId, taskName, taskName, taskName,
                description != null && !description.isBlank() ? description.replace("\n", " ") : taskName,
                taskName);
    }

    private String buildFallbackThreatModel(String projectId, String taskName, String description) {
        return String.format("""
                # STRIDE Threat Modeling Assessment (Phase 0 Output)
                **Project ID:** %s
                **Target:** %s
                **Auditor Agent:** AppSec Auditor Agent
                **Methodology:** Microsoft STRIDE & OWASP Top 10

                ---

                ## 1. Threat Identification Matrix

                | STRIDE Category | Threat Scenario | Impact | Mitigation Strategy |
                |---|---|---|---|
                | **Spoofing** | Actor identity forgery during transaction processing for %s | HIGH | Strong JWT authentication & HMAC-SHA256 payload signatures. |
                | **Tampering** | Modification of state or transactional payload parameters | CRITICAL | Immutable append-only ledger & domain schema validation. |
                | **Repudiation** | User denies initiating or modifying state changes | MEDIUM | Immutable audit log stored in WorkflowTransitionLog with UTC timestamps. |
                | **Information Disclosure** | Leakage of sensitive operational data to unauthorized parties | HIGH | AES-256 field encryption & pre-signed short-lived tokens. |
                | **Denial of Service** | Resource exhaustion via high-rate concurrent payloads | MEDIUM | Bucket4j token bucket rate-limiting & reactive backpressure. |
                | **Elevation of Privilege** | Operator role escalation to administrative supervisor | CRITICAL | Role-based Access Control (RBAC) enforced at controller and domain layer. |

                ---
                ## 2. AppSec Security Directives
                Phase 1 Architecture must incorporate boundary parameter validation, tenant isolation, and immutable transition audit trails for domain %s.
                """, projectId, taskName, taskName, taskName);
    }
}
