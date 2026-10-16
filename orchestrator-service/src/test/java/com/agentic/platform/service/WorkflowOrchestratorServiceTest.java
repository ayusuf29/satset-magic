package com.agentic.platform.service;

import com.agentic.platform.domain.exception.InvalidWorkflowTransitionException;
import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.domain.repository.WorkflowRepository;
import com.agentic.platform.domain.repository.WorkflowTransitionLogRepository;
import com.agentic.platform.service.lifecycle.PhaseExecutionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowOrchestratorServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowTransitionLogRepository transitionLogRepository;

    @Mock
    private WorkflowEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AgentExecutionService agentExecutionService;

    @Mock
    private dev.langchain4j.model.chat.ChatLanguageModel chatLanguageModel;

    private PhaseExecutionManager phaseExecutionManager;

    private WorkflowOrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        phaseExecutionManager = new PhaseExecutionManager(java.util.Collections.emptyList());

        orchestratorService = new WorkflowOrchestratorServiceImpl(
                workflowRepository,
                transitionLogRepository,
                eventPublisher,
                notificationService,
                agentExecutionService,
                chatLanguageModel,
                phaseExecutionManager
        );
        ReflectionTestUtils.setField(orchestratorService, "maxReviewLoops", 3);
        ReflectionTestUtils.setField(orchestratorService, "minTestCoverage", 70.0);

        lenient().when(workflowRepository.save(any(WorkflowInstance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Harus membuat workflow baru pada status PHASE_0_REQUIREMENT_DISCOVERY")
    void shouldCreateWorkflowSuccessfully() {
        WorkflowInstance instance = orchestratorService.createWorkflow(
                "Integrasi Payment Gateway",
                "Fitur pembayaran QRIS dan Virtual Account",
                "{}"
        );

        assertNotNull(instance);
        assertTrue(instance.getId().startsWith("wf-"));
        assertEquals(WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY, instance.getCurrentState());
        assertEquals(0, instance.getReviewLoopCount());
        verify(workflowRepository).save(any(WorkflowInstance.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Harus memajukan state ke fase berikutnya")
    void shouldAdvanceWorkflowToNextState() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-test-01")
                .taskName("Test Task")
                .currentState(WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN)
                .build();

        when(workflowRepository.findById("wf-test-01")).thenReturn(Optional.of(workflow));

        WorkflowInstance updated = orchestratorService.advanceWorkflow("wf-test-01", "ARCHITECT", "C4 diagram approved");

        assertEquals(WorkflowState.PHASE_2_BACKLOG_GENERATION, updated.getCurrentState());
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }

    @Test
    @DisplayName("Dilarang memajukan workflow jika agent masih aktif mengeksekusi tugas")
    void shouldThrowExceptionWhenAdvancingWhileAgentIsRunning() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-agent-running")
                .taskName("Active Agent Task")
                .currentState(WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY)
                .agentRunning(true)
                .currentAgentRole("PO / Spec Writer Agent")
                .currentAgentActivity("Memproses skenario Gherkin")
                .build();

        when(workflowRepository.findById("wf-agent-running")).thenReturn(Optional.of(workflow));

        assertThrows(InvalidWorkflowTransitionException.class, () ->
                orchestratorService.advanceWorkflow("wf-agent-running", "HUMAN", "Premature advance")
        );
    }

    @Test
    @DisplayName("Dilarang memberikan keputusan sign-off jika agent masih aktif bekerja")
    void shouldThrowExceptionWhenHumanSignOffWhileAgentIsRunning() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-agent-gate-running")
                .taskName("Active Gate Task")
                .currentState(WorkflowState.PHASE_0_WAITING_HUMAN_SIGN_OFF)
                .agentRunning(true)
                .currentAgentRole("AppSec Auditor Agent")
                .currentAgentActivity("Melakukan threat modeling")
                .build();

        when(workflowRepository.findById("wf-agent-gate-running")).thenReturn(Optional.of(workflow));

        assertThrows(InvalidWorkflowTransitionException.class, () ->
                orchestratorService.handleHumanDecision("wf-agent-gate-running", "APPROVE", "Premature signoff", "HUMAN")
        );
    }

    @Test
    @DisplayName("Dilarang memajukan workflow jika sedang menunggu keputusan manusia")
    void shouldThrowExceptionWhenAdvancingAtHumanGate() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-test-gate")
                .taskName("Gate Task")
                .currentState(WorkflowState.PHASE_0_WAITING_HUMAN_SIGN_OFF)
                .agentRunning(false)
                .build();

        when(workflowRepository.findById("wf-test-gate")).thenReturn(Optional.of(workflow));

        assertThrows(InvalidWorkflowTransitionException.class, () ->
                orchestratorService.advanceWorkflow("wf-test-gate", "SYSTEM", "Auto advance")
        );
    }

    @Test
    @DisplayName("Code review disetujui jika lolos dan coverage >= 70%")
    void shouldApproveCodeReviewWhenPassingAndCoverageMet() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-review-pass")
                .taskName("Review Task")
                .currentState(WorkflowState.PHASE_6_CODE_REVIEW)
                .reviewLoopCount(1)
                .build();

        when(workflowRepository.findById("wf-review-pass")).thenReturn(Optional.of(workflow));

        WorkflowInstance updated = orchestratorService.handleCodeReviewResult(
                "wf-review-pass",
                true,
                85.5,
                "Clean code standards satisfied",
                "SENIOR_REVIEWER"
        );

        assertEquals(WorkflowState.PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE, updated.getCurrentState());
        assertEquals(85.5, updated.getCoveragePercentage());
    }

    @Test
    @DisplayName("Persetujuan manusia pada PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE memajukan workflow ke PHASE_7_QA_VERIFICATION")
    void shouldAdvanceToQaWhenHumanApprovesMergeGate() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-merge-gate")
                .taskName("Merge to Main Gate")
                .currentState(WorkflowState.PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE)
                .build();

        when(workflowRepository.findById("wf-merge-gate")).thenReturn(Optional.of(workflow));

        WorkflowInstance updated = orchestratorService.handleHumanDecision(
                "wf-merge-gate",
                "APPROVE",
                "PR approved and merged to protected main branch",
                "TECH_LEAD"
        );

        assertEquals(WorkflowState.PHASE_7_QA_VERIFICATION, updated.getCurrentState());
        assertEquals("APPROVED", updated.getLastDecision());
    }

    @Test
    @DisplayName("Penolakan manusia pada PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE mengembalikan workflow ke PHASE_5_DEVELOPMENT_IMPLEMENTATION")
    void shouldReturnToDevWhenHumanRejectsMergeGate() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-merge-reject")
                .taskName("Merge to Main Gate")
                .currentState(WorkflowState.PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE)
                .reviewLoopCount(0)
                .build();

        when(workflowRepository.findById("wf-merge-reject")).thenReturn(Optional.of(workflow));

        WorkflowInstance updated = orchestratorService.handleHumanDecision(
                "wf-merge-reject",
                "REJECT",
                "Need additional integration tests before merging to main",
                "TECH_LEAD"
        );

        assertEquals(WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION, updated.getCurrentState());
        assertEquals("REJECTED", updated.getLastDecision());
        assertEquals(1, updated.getReviewLoopCount());
    }

    @Test
    @DisplayName("Code review gagal pada iterasi < 3 mengembalikan task ke DEVELOPMENT")
    void shouldReturnToDevelopmentWhenReviewFailsBelowMaxLoops() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-review-fail")
                .taskName("Review Task")
                .currentState(WorkflowState.PHASE_6_CODE_REVIEW)
                .reviewLoopCount(0)
                .build();

        when(workflowRepository.findById("wf-review-fail")).thenReturn(Optional.of(workflow));

        WorkflowInstance updated = orchestratorService.handleCodeReviewResult(
                "wf-review-fail",
                false,
                55.0,
                "Kurang unit test pada edge cases",
                "SENIOR_REVIEWER"
        );

        assertEquals(WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION, updated.getCurrentState());
        assertEquals(1, updated.getReviewLoopCount());
    }

    @Test
    @DisplayName("Pemicu DEADLOCK aktif ketika code review gagal 3 kali berturut-turut")
    void shouldTriggerDeadlockWhenReviewLoopsReachMax() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-deadlock")
                .taskName("Complex Refactor")
                .currentState(WorkflowState.PHASE_6_CODE_REVIEW)
                .reviewLoopCount(2)
                .build();

        when(workflowRepository.findById("wf-deadlock")).thenReturn(Optional.of(workflow));

        WorkflowInstance updated = orchestratorService.handleCodeReviewResult(
                "wf-deadlock",
                false,
                62.0,
                "Persistent architectural inconsistency",
                "SENIOR_REVIEWER"
        );

        assertEquals(WorkflowState.PHASE_6_DEADLOCK_WAITING_HUMAN, updated.getCurrentState());
        assertEquals(3, updated.getReviewLoopCount());
        verify(notificationService).sendHumanApprovalRequiredAlert(any(), contains("DEADLOCK"));
    }

    @Test
    @DisplayName("Persetujuan manusia pada PHASE_8_READY_FOR_PRODUCTION_GATE merilis workflow ke PHASE_9_RELEASED")
    void shouldReleaseWorkflowWhenHumanApprovesProductionGate() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-prod-gate")
                .taskName("Production Release")
                .currentState(WorkflowState.PHASE_8_READY_FOR_PRODUCTION_GATE)
                .build();

        when(workflowRepository.findById("wf-prod-gate")).thenReturn(Optional.of(workflow));

        WorkflowInstance updated = orchestratorService.handleHumanDecision(
                "wf-prod-gate",
                "APPROVE",
                "All DAST & k6 tests passed, ready to ship",
                "VP_ENGINEERING"
        );

        assertEquals(WorkflowState.PHASE_9_RELEASED, updated.getCurrentState());
        assertEquals("APPROVED", updated.getLastDecision());
    }

    @Test
    @DisplayName("Council Triad lolos jika Architect, Security, dan Perf semuanya setuju dengan coverage >= 70%")
    void shouldPassCouncilReviewWhenAllThreeApprove() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-council-pass")
                .taskName("Council Pass Task")
                .currentState(WorkflowState.PHASE_6_CODE_REVIEW)
                .reviewLoopCount(0)
                .build();

        when(workflowRepository.findById("wf-council-pass")).thenReturn(Optional.of(workflow));

        WorkflowInstance updated = orchestratorService.handleCouncilReviewResult(
                "wf-council-pass",
                true, // Architect
                true, // Security
                true, // Performance
                82.0,
                "Konsensus Triad tercapai"
        );

        assertEquals(WorkflowState.PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE, updated.getCurrentState());
        assertTrue(updated.isArchitectApproved());
        assertTrue(updated.isSecurityApproved());
        assertTrue(updated.isPerformanceApproved());
        assertEquals(82.0, updated.getCoveragePercentage());
    }

    @Test
    @DisplayName("Council Triad gagal jika salah satu pilar veto (Security REJECT) meskipun Architect dan Perf setuju")
    void shouldRejectCouncilReviewWhenSecurityVetos() {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .id("wf-council-veto")
                .taskName("Security Veto Task")
                .currentState(WorkflowState.PHASE_6_CODE_REVIEW)
                .reviewLoopCount(0)
                .build();

        when(workflowRepository.findById("wf-council-veto")).thenReturn(Optional.of(workflow));

        WorkflowInstance updated = orchestratorService.handleCouncilReviewResult(
                "wf-council-veto",
                true,  // Architect
                false, // Security VETO
                true,  // Performance
                85.0,
                "SAST mendeteksi OWASP A01 broken access control"
        );

        assertEquals(WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION, updated.getCurrentState());
        assertTrue(updated.isArchitectApproved());
        assertFalse(updated.isSecurityApproved());
        assertTrue(updated.isPerformanceApproved());
        assertEquals(1, updated.getReviewLoopCount());
    }
}
