package com.agentic.platform.service;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowTransitionLog;

import java.util.List;

public interface WorkflowOrchestratorService {
    WorkflowInstance createWorkflow(String projectId, String clientId, String taskName, String description, String initialPayload);

    default WorkflowInstance createWorkflow(String taskName, String description, String initialPayload) {
        return createWorkflow(null, null, taskName, description, initialPayload);
    }

    List<WorkflowInstance> getAllWorkflows();
    WorkflowInstance getWorkflowById(String workflowId);
    List<WorkflowTransitionLog> getWorkflowHistory(String workflowId);
    WorkflowInstance advanceWorkflow(String workflowId, String actor, String comment);
    WorkflowInstance handleCouncilReviewResult(String workflowId, boolean architectApproved, boolean securityApproved, boolean performanceApproved, double coverage, String feedback);

    default WorkflowInstance handleCodeReviewResult(String workflowId, boolean passed, double coveragePercentage, String feedback, String reviewer) {
        return handleCouncilReviewResult(workflowId, passed, passed, passed, coveragePercentage, "[" + reviewer + "] " + feedback);
    }

    WorkflowInstance handleHumanDecision(String workflowId, String decision, String comment, String actor);
    void deleteWorkflow(String workflowId);
    void deleteAllWorkflows();
}
