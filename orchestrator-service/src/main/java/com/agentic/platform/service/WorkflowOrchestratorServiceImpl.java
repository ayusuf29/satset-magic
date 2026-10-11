package com.agentic.platform.service;

import com.agentic.platform.domain.exception.InvalidWorkflowTransitionException;
import com.agentic.platform.domain.exception.WorkflowNotFoundException;
import com.agentic.platform.domain.model.AgentEvent;
import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.domain.model.WorkflowTransitionLog;
import com.agentic.platform.domain.repository.WorkflowRepository;
import com.agentic.platform.domain.repository.WorkflowTransitionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowOrchestratorServiceImpl implements WorkflowOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowOrchestratorServiceImpl.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowTransitionLogRepository transitionLogRepository;
    private final WorkflowEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final AgentExecutionService agentExecutionService;

    @Value("${agentic.workflow.max-review-loops:3}")
    private int maxReviewLoops = 3;

    @Value("${agentic.workflow.min-test-coverage:70}")
    private double minTestCoverage = 70.0;

    public WorkflowOrchestratorServiceImpl(
            WorkflowRepository workflowRepository,
            WorkflowTransitionLogRepository transitionLogRepository,
            WorkflowEventPublisher eventPublisher,
            NotificationService notificationService,
            AgentExecutionService agentExecutionService) {
        this.workflowRepository = workflowRepository;
        this.transitionLogRepository = transitionLogRepository;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.agentExecutionService = agentExecutionService;
    }

    @Override
    @Transactional
    public WorkflowInstance createWorkflow(String taskName, String description, String initialPayload) {
        String workflowId = "wf-" + UUID.randomUUID().toString().substring(0, 8);
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(workflowId)
                .taskName(taskName)
                .description(description)
                .currentState(WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY)
                .reviewLoopCount(0)
                .coveragePercentage(0.0)
                .payloadJson(initialPayload)
                .build();

        WorkflowInstance saved = workflowRepository.save(instance);

        recordTransition(saved, null, WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY, "SYSTEM", "Workflow initialized");
        eventPublisher.publishEvent(AgentEvent.of(
                saved.getId(),
                saved.getCurrentState(),
                "WORKFLOW_CREATED",
                "Workflow baru dimulai: " + taskName
        ));

        return saved;
    }

    @Override
    public List<WorkflowInstance> getAllWorkflows() {
        return workflowRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public WorkflowInstance getWorkflowById(String workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }

    @Override
    public List<WorkflowTransitionLog> getWorkflowHistory(String workflowId) {
        return transitionLogRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId);
    }

    @Override
    @Transactional
    public WorkflowInstance advanceWorkflow(String workflowId, String actor, String comment) {
        WorkflowInstance workflow = getWorkflowById(workflowId);
        WorkflowState currentState = workflow.getCurrentState();

        if (currentState.isHumanGate()) {
            throw new InvalidWorkflowTransitionException(
                    currentState,
                    currentState.getNextDefaultState(),
                    "Workflow sedang menunggu persetujuan manusia. Gunakan endpoint /approve atau /reject."
            );
        }

        if (currentState == WorkflowState.PHASE_9_RELEASED || currentState == WorkflowState.FAILED) {
            throw new InvalidWorkflowTransitionException(currentState, currentState, "Workflow sudah berada di fase akhir.");
        }

        WorkflowState nextState = currentState.getNextDefaultState();
        return transitionToState(workflow, nextState, actor != null ? actor : "ORCHESTRATOR", comment);
    }

    @Override
    @Transactional
    public WorkflowInstance handleCodeReviewResult(String workflowId, boolean passed, double coverage, String feedback, String reviewer) {
        WorkflowInstance workflow = getWorkflowById(workflowId);

        if (workflow.getCurrentState() != WorkflowState.PHASE_6_CODE_REVIEW) {
            throw new InvalidWorkflowTransitionException(
                    workflow.getCurrentState(),
                    WorkflowState.PHASE_7_QA_VERIFICATION,
                    "Code review hanya valid dijalankan pada fase PHASE_6_CODE_REVIEW."
            );
        }

        workflow.setCoveragePercentage(coverage);

        if (passed && coverage >= minTestCoverage) {
            log.info("Code review APPROVED for workflow: {}. Coverage: {}%", workflowId, coverage);
            return transitionToState(
                    workflow,
                    WorkflowState.PHASE_7_QA_VERIFICATION,
                    reviewer != null ? reviewer : "CODE_REVIEWER_CONSORTIUM",
                    "Code Review disetujui dengan test coverage: " + coverage + "%. Feedback: " + feedback
            );
        } else {
            int loops = workflow.getReviewLoopCount() + 1;
            workflow.setReviewLoopCount(loops);

            if (loops >= maxReviewLoops) {
                log.warn("DEADLOCK DETECTED! Workflow {} has failed code review {} times. Escalating to human decision.", workflowId, loops);
                return transitionToState(
                        workflow,
                        WorkflowState.PHASE_6_DEADLOCK_WAITING_HUMAN,
                        "CODE_REVIEWER_CONSORTIUM",
                        "DEADLOCK: Review gagal sebanyak " + loops + "x berturut-turut. Coverage: " + coverage + "%. Alasan: " + feedback
                );
            } else {
                log.info("Code review REJECTED for workflow: {}. Loop count: {}/3. Returning to Development.", workflowId, loops);
                return transitionToState(
                        workflow,
                        WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION,
                        reviewer != null ? reviewer : "CODE_REVIEWER_CONSORTIUM",
                        "Review belum disetujui (Iterasi " + loops + "/3). Perbaiki temuan: " + feedback
                );
            }
        }
    }

    @Override
    @Transactional
    public WorkflowInstance handleHumanDecision(String workflowId, String decision, String comment, String actor) {
        WorkflowInstance workflow = getWorkflowById(workflowId);
        WorkflowState currentState = workflow.getCurrentState();

        if (!currentState.isHumanGate()) {
            throw new InvalidWorkflowTransitionException(
                    currentState,
                    currentState,
                    "Fase saat ini tidak memerlukan persetujuan manusia."
            );
        }

        boolean isApprove = "APPROVE".equalsIgnoreCase(decision);
        workflow.setLastDecision(isApprove ? "APPROVED" : "REJECTED");

        if (currentState == WorkflowState.PHASE_0_WAITING_HUMAN_SIGN_OFF) {
            if (isApprove) {
                return transitionToState(workflow, WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN, actor, "Sign-off kebutuhan disetujui: " + comment);
            } else {
                return transitionToState(workflow, WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY, actor, "Kebutuhan ditolak, minta klarifikasi ulang: " + comment);
            }
        } else if (currentState == WorkflowState.PHASE_6_DEADLOCK_WAITING_HUMAN) {
            if (isApprove) {
                return transitionToState(workflow, WorkflowState.PHASE_7_QA_VERIFICATION, actor, "Deadlock di-override oleh manusia (Disetujui): " + comment);
            } else {
                workflow.setReviewLoopCount(0);
                return transitionToState(workflow, WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION, actor, "Deadlock ditolak. Dev mengulang dengan arahan manusia: " + comment);
            }
        } else if (currentState == WorkflowState.PHASE_8_READY_FOR_PRODUCTION_GATE) {
            if (isApprove) {
                return transitionToState(workflow, WorkflowState.PHASE_9_RELEASED, actor, "Deployment rilis produksi disetujui: " + comment);
            } else {
                return transitionToState(workflow, WorkflowState.FAILED, actor, "Rilis produksi dibatalkan: " + comment);
            }
        }

        return workflow;
    }

    private WorkflowInstance transitionToState(WorkflowInstance workflow, WorkflowState targetState, String actor, String message) {
        WorkflowState previousState = workflow.getCurrentState();
        workflow.setCurrentState(targetState);
        WorkflowInstance updated = workflowRepository.save(workflow);

        recordTransition(updated, previousState, targetState, actor, message);

        String eventType = targetState.isHumanGate() ? "HUMAN_GATE_TRIGGERED" : "STATE_TRANSITION";
        eventPublisher.publishEvent(AgentEvent.of(
                updated.getId(),
                targetState,
                eventType,
                message != null ? message : ("Transisi status ke " + targetState.name())
        ));

        if (targetState.isHumanGate()) {
            notificationService.sendHumanApprovalRequiredAlert(updated, message);
        }

        return updated;
    }

    private void recordTransition(WorkflowInstance workflow, WorkflowState from, WorkflowState to, String actor, String message) {
        WorkflowTransitionLog logEntry = WorkflowTransitionLog.builder()
                .workflowId(workflow.getId())
                .fromState(from != null ? from : to)
                .toState(to)
                .actor(actor != null ? actor : "SYSTEM")
                .message(message)
                .build();
        transitionLogRepository.save(logEntry);
    }
}
