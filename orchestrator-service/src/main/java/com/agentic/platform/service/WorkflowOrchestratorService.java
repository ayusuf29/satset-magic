package com.agentic.platform.service;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowTransitionLog;

import java.util.List;

public interface WorkflowOrchestratorService {
    WorkflowInstance createWorkflow(String taskName, String description, String initialPayload);
    List<WorkflowInstance> getAllWorkflows();
    WorkflowInstance getWorkflowById(String workflowId);
    List<WorkflowTransitionLog> getWorkflowHistory(String workflowId);
    WorkflowInstance advanceWorkflow(String workflowId, String actor, String comment);
    WorkflowInstance handleCodeReviewResult(String workflowId, boolean passed, double coverage, String feedback, String reviewer);
    WorkflowInstance handleHumanDecision(String workflowId, String decision, String comment, String actor);
}
