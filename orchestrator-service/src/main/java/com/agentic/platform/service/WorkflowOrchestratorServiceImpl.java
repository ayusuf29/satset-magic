package com.agentic.platform.service;

import com.agentic.platform.domain.exception.InvalidWorkflowTransitionException;
import com.agentic.platform.domain.exception.WorkflowNotFoundException;
import com.agentic.platform.domain.model.AgentEvent;
import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.domain.model.WorkflowTransitionLog;
import com.agentic.platform.domain.repository.WorkflowRepository;
import com.agentic.platform.domain.repository.WorkflowTransitionLogRepository;
import com.agentic.platform.service.lifecycle.PhaseContext;
import com.agentic.platform.service.lifecycle.PhaseExecutionManager;
import com.agentic.platform.service.lifecycle.PhaseResult;
import com.agentic.platform.service.phase.PhaseAgentHandler;
import com.agentic.platform.service.phase.SdlcJournalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Refactored Workflow Orchestrator Engine adhering to SOLID principles (SRP, OCP, LSP, ISP, DIP).
 * Acts as a lean coordinator delegating phase executions to specialized PhaseAgentHandler implementations.
 */
@Service
public class WorkflowOrchestratorServiceImpl implements WorkflowOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowOrchestratorServiceImpl.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowTransitionLogRepository transitionLogRepository;
    private final WorkflowEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final PhaseExecutionManager phaseExecutionManager;
    private final SdlcJournalService journalService;
    private final Map<WorkflowState, PhaseAgentHandler> phaseHandlerMap = new EnumMap<>(WorkflowState.class);

    @Value("${agentic.workflow.max-review-loops:3}")
    private int maxReviewLoops = 3;

    @Value("${agentic.workflow.min-test-coverage:70}")
    private double minTestCoverage = 70.0;

    public WorkflowOrchestratorServiceImpl(
            WorkflowRepository workflowRepository,
            WorkflowTransitionLogRepository transitionLogRepository,
            WorkflowEventPublisher eventPublisher,
            NotificationService notificationService,
            PhaseExecutionManager phaseExecutionManager,
            SdlcJournalService journalService,
            List<PhaseAgentHandler> phaseHandlers) {
        this.workflowRepository = workflowRepository;
        this.transitionLogRepository = transitionLogRepository;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.phaseExecutionManager = phaseExecutionManager;
        this.journalService = journalService;

        for (PhaseAgentHandler handler : phaseHandlers) {
            phaseHandlerMap.put(handler.getHandledPhase(), handler);
            log.info("Registered SOLID PhaseAgentHandler [{}] for state: {}",
                    handler.getClass().getSimpleName(), handler.getHandledPhase());
        }
    }

    @Override
    @Transactional
    public WorkflowInstance createWorkflow(String projectId, String clientId, String taskName, String description, String initialPayload) {
        String workflowId = "wf-" + UUID.randomUUID().toString().substring(0, 8);
        String finalProjectId = (projectId != null && !projectId.isBlank()) ? projectId : "PRJ-" + workflowId.toUpperCase();
        String finalClientId = (clientId != null && !clientId.isBlank()) ? clientId : "CLIENT-INTERNAL";

        WorkflowInstance instance = WorkflowInstance.builder()
                .id(workflowId)
                .projectId(finalProjectId)
                .clientId(finalClientId)
                .taskName(taskName)
                .description(description)
                .currentState(WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY)
                .reviewLoopCount(0)
                .coveragePercentage(0.0)
                .architectApproved(false)
                .securityApproved(false)
                .performanceApproved(false)
                .agentRunning(true)
                .currentAgentRole("PO / Spec Writer Agent")
                .currentAgentActivity("Analyzing requirements & extracting domain entities...")
                .phaseProgressPercentage(15)
                .payloadJson(initialPayload)
                .build();

        WorkflowInstance saved = workflowRepository.saveAndFlush(instance);

        // Auto-provision isolated workspace directory for this project
        journalService.provisionProjectWorkspace(finalProjectId, taskName, description);

        recordTransition(saved, null, WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY, "SYSTEM",
                "Workflow initialized for Project: " + finalProjectId);

        eventPublisher.publishEvent(AgentEvent.of(
                saved.getId(),
                saved.getCurrentState(),
                "WORKFLOW_CREATED",
                String.format("[%s][%s] Workflow initiated: %s", finalClientId, finalProjectId, taskName)));

        // Trigger Phase 0 autonomously
        dispatchAutonomousExecution(WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY, saved.getId());

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

        if (workflow.isAgentRunning()) {
            throw new InvalidWorkflowTransitionException(
                    currentState,
                    currentState.getNextDefaultState(),
                    String.format("Agent is actively executing [%s: %s (%d%%)]. Manual interruption prevented to preserve flow integrity.",
                            workflow.getCurrentAgentRole(), workflow.getCurrentAgentActivity(), workflow.getPhaseProgressPercentage()));
        }

        if (currentState.isHumanGate()) {
            throw new InvalidWorkflowTransitionException(
                    currentState,
                    currentState.getNextDefaultState(),
                    "Workflow is awaiting human authorization gate. Use /approve or /reject endpoints.");
        }

        if (currentState == WorkflowState.PHASE_9_RELEASED || currentState == WorkflowState.FAILED) {
            throw new InvalidWorkflowTransitionException(currentState, currentState, "Workflow is already in a terminal state.");
        }

        WorkflowState nextState = currentState.getNextDefaultState();
        WorkflowInstance transitioned = transitionToState(workflow, nextState, actor != null ? actor : "ORCHESTRATOR", comment);
        dispatchAutonomousExecution(nextState, workflowId);
        return transitioned;
    }

    @Override
    @Transactional
    public WorkflowInstance handleCouncilReviewResult(
            String workflowId,
            boolean architectApproved,
            boolean securityApproved,
            boolean performanceApproved,
            double coverage,
            String feedback) {

        WorkflowInstance workflow = getWorkflowById(workflowId);

        if (workflow.getCurrentState() != WorkflowState.PHASE_6_CODE_REVIEW) {
            throw new InvalidWorkflowTransitionException(
                    workflow.getCurrentState(),
                    WorkflowState.PHASE_7_QA_VERIFICATION,
                    "Council review is only valid in PHASE_6_CODE_REVIEW.");
        }

        workflow.setCoveragePercentage(coverage);
        workflow.setArchitectApproved(architectApproved);
        workflow.setSecurityApproved(securityApproved);
        workflow.setPerformanceApproved(performanceApproved);

        boolean consensusReached = architectApproved && securityApproved && performanceApproved && (coverage >= minTestCoverage);

        if (consensusReached) {
            log.info("Triad Council Consensus reached for workflow: {}. Coverage: {}%", workflowId, coverage);
            WorkflowInstance transitioned = transitionToState(
                    workflow,
                    WorkflowState.PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE,
                    "TRIAD_COUNCIL_CONSORTIUM",
                    String.format("Council Consensus Passed (Arch: OK, Sec: OK, Perf: OK, Coverage: %.1f%%). Awaiting Human Tech Lead merge authorization. Feedback: %s",
                            coverage, feedback));
            notificationService.sendHumanApprovalRequiredAlert(transitioned,
                    String.format("Triad Council Consensus Passed (Coverage: %.1f%%). Authorization to merge to protected main branch required.", coverage));
            return transitioned;
        } else {
            int loops = workflow.getReviewLoopCount() + 1;
            workflow.setReviewLoopCount(loops);

            String rejectionSummary = String.format("Council Vetoed: [Arch: %s, Sec: %s, Perf: %s, Cov: %.1f%%/%.1f%%]. Reason: %s",
                    architectApproved ? "PASS" : "REJECT",
                    securityApproved ? "PASS" : "REJECT",
                    performanceApproved ? "PASS" : "REJECT",
                    coverage, minTestCoverage, feedback);

            if (loops >= maxReviewLoops) {
                log.warn("DEADLOCK DETECTED! Workflow {} failed council review {} times. Escalating to Human Lead.", workflowId, loops);
                WorkflowInstance deadlockWf = transitionToState(
                        workflow,
                        WorkflowState.PHASE_6_DEADLOCK_WAITING_HUMAN,
                        "TRIAD_COUNCIL_CONSORTIUM",
                        "DEADLOCK: Review failed " + loops + " consecutive times. " + rejectionSummary);
                notificationService.sendHumanApprovalRequiredAlert(deadlockWf,
                        "DEADLOCK DETECTED: Council review failed " + loops + "x consecutive times. Human decision required.");
                return deadlockWf;
            } else {
                log.info("Council Review REJECTED for workflow: {}. Loop: {}/3. Returning to Development.", workflowId, loops);
                WorkflowInstance returned = transitionToState(
                        workflow,
                        WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION,
                        "TRIAD_COUNCIL_CONSORTIUM",
                        "Review rejected (Iteration " + loops + "/3). " + rejectionSummary);
                dispatchAutonomousExecution(WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION, workflowId);
                return returned;
            }
        }
    }

    @Override
    @Transactional
    public WorkflowInstance handleHumanDecision(String workflowId, String decision, String comment, String actor, WorkflowState targetPhase) {
        WorkflowInstance workflow = getWorkflowById(workflowId);
        WorkflowState currentState = workflow.getCurrentState();

        if (workflow.isAgentRunning() && !currentState.isHumanGate()) {
            throw new InvalidWorkflowTransitionException(
                    currentState,
                    currentState,
                    String.format("Agent is still executing [%s: %s (%d%%)]. Please wait for completion before submitting a decision.",
                            workflow.getCurrentAgentRole(), workflow.getCurrentAgentActivity(), workflow.getPhaseProgressPercentage()));
        }

        if (!currentState.isHumanGate()) {
            throw new InvalidWorkflowTransitionException(currentState, currentState, "Current phase does not require human authorization.");
        }

        boolean isApprove = "APPROVE".equalsIgnoreCase(decision) || "SETUJUI".equalsIgnoreCase(decision);
        workflow.setLastDecision(isApprove ? "APPROVED" : "RETURNED");
        if (isApprove) {
            workflow.setRevisionFeedback(null);
        } else {
            workflow.setRevisionFeedback(comment);
        }

        if (isApprove) {
            WorkflowState nextState = currentState.getNextDefaultState();
            String approvalComment = "Disetujui oleh " + (actor != null ? actor : "HUMAN_OPERATOR") + ": " + (comment != null && !comment.isBlank() ? comment : "Setuju & lanjutkan");
            WorkflowInstance transitioned = transitionToState(workflow, nextState, actor != null ? actor : "HUMAN_OPERATOR", approvalComment);

            eventPublisher.publishEvent(AgentEvent.of(workflowId, currentState, "HUMAN_DECISION",
                    String.format("[%s] Gate %s disetujui. Melanjutkan ke %s.", actor, currentState, nextState)));

            if (nextState != WorkflowState.PHASE_9_RELEASED && nextState != WorkflowState.FAILED) {
                dispatchAutonomousExecution(nextState, workflowId);
            }
            return transitioned;
        } else {
            // RETURN / KEMBALIKAN (Refinement)
            workflow.setReviewLoopCount(workflow.getReviewLoopCount() + 1);

            WorkflowState rewindState;
            if (targetPhase != null) {
                rewindState = targetPhase;
            } else {
                rewindState = switch (currentState) {
                    case PHASE_0_WAITING_HUMAN_SIGN_OFF -> WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY;
                    case PHASE_1_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN;
                    case PHASE_2_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_2_BACKLOG_GENERATION;
                    case PHASE_3_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_3_TASK_DISPATCHING;
                    case PHASE_4_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_4_TDD_PLANNING;
                    case PHASE_5_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION;
                    case PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE, PHASE_6_DEADLOCK_WAITING_HUMAN -> WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION;
                    case PHASE_7_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION;
                    case PHASE_8_READY_FOR_PRODUCTION_GATE -> WorkflowState.PHASE_7_QA_VERIFICATION;
                    default -> WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION;
                };
            }

            String returnComment = "Dikembalikan untuk revisi oleh " + (actor != null ? actor : "HUMAN_OPERATOR") + " ke " + rewindState.name() + ": " + (comment != null && !comment.isBlank() ? comment : "Perlu penyempurnaan");
            workflow.setAgentRunning(true);
            workflow.setPhaseProgressPercentage(10);
            WorkflowInstance transitioned = transitionToState(workflow, rewindState, actor != null ? actor : "HUMAN_OPERATOR", returnComment);

            eventPublisher.publishEvent(AgentEvent.of(workflowId, currentState, "WORKFLOW_RETURNED",
                    String.format("[%s] Workflow dikembalikan ke %s untuk refine. Catatan: %s", actor, rewindState, comment)));

            dispatchAutonomousExecution(rewindState, workflowId);
            return transitioned;
        }
    }

    @Override
    @Transactional
    public WorkflowInstance handleHumanDecision(String workflowId, String decision, String comment, String actor) {
        return handleHumanDecision(workflowId, decision, comment, actor, null);
    }

    @Override
    @Transactional
    public WorkflowInstance revertWorkflowToPhase(String workflowId, WorkflowState targetPhase, String reason, String actor) {
        WorkflowInstance workflow = getWorkflowById(workflowId);
        WorkflowState fromState = workflow.getCurrentState();

        workflow.setCurrentState(targetPhase);
        workflow.setLastDecision("REVERTED_TO_" + targetPhase.name());
        workflow.setRevisionFeedback(reason);

        if (targetPhase.isHumanGate()) {
            workflow.setAgentRunning(false);
            workflow.setCurrentAgentRole("Human Supervisor / Tech Lead");
            workflow.setCurrentAgentActivity("Awaiting human authorization for gate: " + targetPhase.name());
            workflow.setPhaseProgressPercentage(100);
            WorkflowInstance saved = workflowRepository.saveAndFlush(workflow);
            recordTransition(saved, fromState, targetPhase, actor != null ? actor : "HUMAN_OPERATOR",
                    "Workflow reverted: " + reason);
            eventPublisher.publishEvent(AgentEvent.of(workflowId, targetPhase, "STATE_REVERTED",
                    String.format("[%s] Workflow reverted from %s to %s. Reason: %s", actor, fromState, targetPhase, reason)));
            notificationService.sendHumanApprovalRequiredAlert(saved,
                    "Workflow reverted to " + targetPhase.name() + ". Reason: " + reason);
            return saved;
        } else {
            workflow.setAgentRunning(true);
            workflow.setPhaseProgressPercentage(0);
            WorkflowInstance saved = workflowRepository.saveAndFlush(workflow);
            recordTransition(saved, fromState, targetPhase, actor != null ? actor : "HUMAN_OPERATOR",
                    "Workflow reverted and execution restarted: " + reason);
            eventPublisher.publishEvent(AgentEvent.of(workflowId, targetPhase, "STATE_REVERTED",
                    String.format("[%s] Workflow reverted to %s and execution restarted.", actor, targetPhase)));
            dispatchAutonomousExecution(targetPhase, workflowId);
            return saved;
        }
    }

    @Override
    @Transactional
    public WorkflowInstance retryCurrentPhase(String workflowId, String reason, String actor) {
        WorkflowInstance workflow = getWorkflowById(workflowId);
        WorkflowState currentState = workflow.getCurrentState();

        WorkflowState targetState;
        if (currentState.isHumanGate()) {
            targetState = switch (currentState) {
                case PHASE_0_WAITING_HUMAN_SIGN_OFF -> WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY;
                case PHASE_1_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN;
                case PHASE_2_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_2_BACKLOG_GENERATION;
                case PHASE_3_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_3_TASK_DISPATCHING;
                case PHASE_4_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_4_TDD_PLANNING;
                case PHASE_5_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION;
                case PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE, PHASE_6_DEADLOCK_WAITING_HUMAN -> WorkflowState.PHASE_6_CODE_REVIEW;
                case PHASE_7_WAITING_HUMAN_APPROVAL -> WorkflowState.PHASE_7_QA_VERIFICATION;
                case PHASE_8_READY_FOR_PRODUCTION_GATE -> WorkflowState.PHASE_8_READY_FOR_PRODUCTION_GATE;
                default -> currentState;
            };
            workflow.setCurrentState(targetState);
        } else {
            targetState = currentState;
        }

        workflow.setAgentRunning(true);
        workflow.setPhaseProgressPercentage(10);
        workflow.setRevisionFeedback(reason);
        WorkflowInstance saved = workflowRepository.saveAndFlush(workflow);

        recordTransition(saved, currentState, targetState, actor != null ? actor : "HUMAN_OPERATOR",
                "Phase execution retried: " + reason);

        eventPublisher.publishEvent(AgentEvent.of(workflowId, targetState, "PHASE_RETRIED",
                String.format("[%s] Execution of %s retried. Reason: %s", actor, targetState, reason)));

        dispatchAutonomousExecution(targetState, workflowId);
        return saved;
    }

    @Override
    @Transactional
    public void deleteWorkflow(String workflowId) {
        log.info("Deleting workflow {} and transition logs...", workflowId);
        transitionLogRepository.deleteByWorkflowId(workflowId);
        workflowRepository.deleteById(workflowId);
        log.info("Successfully deleted workflow {}", workflowId);
    }

    @Override
    @Transactional
    public void deleteAllWorkflows() {
        log.info("Deleting all workflows and transition logs...");
        transitionLogRepository.deleteAll();
        workflowRepository.deleteAll();
        log.info("Successfully deleted all workflows and transition logs.");
    }

    private WorkflowInstance getWorkflowWithRetry(String workflowId, int maxRetries, long delayMillis) {
        for (int i = 0; i < maxRetries; i++) {
            Optional<WorkflowInstance> opt = workflowRepository.findById(workflowId);
            if (opt.isPresent()) {
                return opt.get();
            }
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return getWorkflowById(workflowId);
    }

    private void dispatchAutonomousExecution(WorkflowState state, String workflowId) {
        PhaseAgentHandler handler = phaseHandlerMap.get(state);
        if (handler == null) {
            log.debug("No PhaseAgentHandler registered for state: {}. Waiting for external action.", state);
            Optional<WorkflowInstance> optWf = workflowRepository.findById(workflowId);
            optWf.ifPresent(w -> {
                w.setAgentRunning(false);
                workflowRepository.save(w);
            });
            return;
        }

        Runnable triggerTask = () -> CompletableFuture.runAsync(() -> {
            try {
                WorkflowInstance workflow = getWorkflowWithRetry(workflowId, 5, 200);
                PhaseContext context = PhaseContext.of(
                        workflowId,
                        workflow.getProjectId(),
                        workflow.getTaskName(),
                        workflow.getDescription(),
                        state,
                        handler.getAgentRole(),
                        handler.getInstalledSkills(),
                        handler.getPatternsAndTechniques(),
                        workflow.getRevisionFeedback()
                );

                PhaseResult result = phaseExecutionManager.executePhase(context, progress -> {
                    try {
                        return handler.execute(workflow, context, progress);
                    } catch (Exception ex) {
                        log.error("Error executing handler for state {}: {}", state, ex.getMessage(), ex);
                        throw new RuntimeException(ex);
                    }
                });

                handlePhaseCompletion(workflowId, handler, result);

            } catch (Exception ex) {
                log.error("Error in autonomous execution for state {} in workflow {}: {}", state, workflowId, ex.getMessage(), ex);
            }
        });

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    triggerTask.run();
                }
            });
        } else {
            triggerTask.run();
        }
    }

    private void handlePhaseCompletion(String workflowId, PhaseAgentHandler handler, PhaseResult result) {
        workflowRepository.findById(workflowId).ifPresent(wf -> {
            WorkflowState nextState = handler.getNextPhaseOnSuccess();

            if (nextState == null) {
                log.info("Phase {} completed without target next state.", handler.getHandledPhase());
                return;
            }

            if (handler.isAutoAdvance()) {
                // Auto-advance to the next state
                transitionToState(wf, nextState, "AUTONOMOUS_ORCHESTRATOR",
                        "Phase " + handler.getHandledPhase().name() + " successfully completed.");
                dispatchAutonomousExecution(nextState, workflowId);
            } else {
                // Phase transitions to a Human Gate
                transitionToState(wf, nextState, "AUTONOMOUS_ORCHESTRATOR",
                        "Phase " + handler.getHandledPhase().name() + " completed. Awaiting Human Authorization Gate.");

                // Set agent status to idle / waiting
                wf.setAgentRunning(false);
                wf.setCurrentAgentRole("Human Supervisor / Tech Lead");
                wf.setCurrentAgentActivity("Awaiting human authorization for gate: " + nextState.name());
                wf.setPhaseProgressPercentage(100);
                workflowRepository.saveAndFlush(wf);

                notificationService.sendHumanApprovalRequiredAlert(wf,
                        "Phase " + handler.getHandledPhase().name() + " completed. Awaiting authorization for " + nextState.name());
                eventPublisher.publishEvent(AgentEvent.of(workflowId, nextState, "HUMAN_GATE_TRIGGERED",
                        String.format("[%s] Automated checks passed. Awaiting human authorization to proceed.", nextState.name())));
            }
        });
    }

    private WorkflowInstance transitionToState(WorkflowInstance workflow, WorkflowState targetState, String actor, String comment) {
        WorkflowState fromState = workflow.getCurrentState();
        workflow.setCurrentState(targetState);
        WorkflowInstance saved = workflowRepository.saveAndFlush(workflow);

        recordTransition(saved, fromState, targetState, actor, comment);

        eventPublisher.publishEvent(AgentEvent.of(
                saved.getId(),
                targetState,
                "STATE_TRANSITION",
                String.format("[%s] %s -> %s: %s", actor, fromState, targetState, comment)));

        return saved;
    }

    private void recordTransition(WorkflowInstance workflow, WorkflowState fromState, WorkflowState toState, String actor, String comment) {
        WorkflowTransitionLog logEntry = WorkflowTransitionLog.builder()
                .workflowId(workflow.getId())
                .fromState(fromState)
                .toState(toState)
                .actor(actor)
                .message(comment)
                .build();
        transitionLogRepository.save(logEntry);
    }
}
