package com.agentic.platform.service.lifecycle;

import com.agentic.platform.domain.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class WorkflowStateCheckpointerObserver implements PhaseLifecycleObserver {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStateCheckpointerObserver.class);

    private final WorkflowRepository workflowRepository;

    public WorkflowStateCheckpointerObserver(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public void onPreTask(PhaseContext context) {
        workflowRepository.findById(context.getWorkflowId()).ifPresent(wf -> {
            wf.setAgentRunning(true);
            wf.setCurrentAgentRole(context.getAgentRole());
            wf.setCurrentAgentActivity("Starting phase pre-task execution...");
            wf.setPhaseProgressPercentage(20);
            workflowRepository.save(wf);
            log.info("[CHECKPOINTER OBSERVER: PRE-TASK] Locked UI & checkpointer state for workflow: {}", wf.getId());
        });
    }

    @Override
    public void onTaskProgress(PhaseContext context, int progressPercentage, String currentActivity) {
        workflowRepository.findById(context.getWorkflowId()).ifPresent(wf -> {
            wf.setPhaseProgressPercentage(progressPercentage);
            wf.setCurrentAgentActivity(currentActivity);
            workflowRepository.save(wf);
        });
    }

    @Override
    public void onPostTask(PhaseContext context, PhaseResult result) {
        workflowRepository.findById(context.getWorkflowId()).ifPresent(wf -> {
            wf.setAgentRunning(false);
            wf.setPhaseProgressPercentage(100);
            wf.setCurrentAgentActivity(result.getOutputSummary());
            if (result.getCoveragePercentage() != null) {
                wf.setCoveragePercentage(result.getCoveragePercentage());
            }
            workflowRepository.save(wf);
            log.info("[CHECKPOINTER OBSERVER: POST-TASK] Checkpointed 100% completion for workflow: {}", wf.getId());
        });
    }

    @Override
    public void onTaskFailed(PhaseContext context, Throwable throwable) {
        workflowRepository.findById(context.getWorkflowId()).ifPresent(wf -> {
            wf.setAgentRunning(false);
            wf.setCurrentAgentActivity("Error: " + throwable.getMessage());
            workflowRepository.save(wf);
        });
    }
}
