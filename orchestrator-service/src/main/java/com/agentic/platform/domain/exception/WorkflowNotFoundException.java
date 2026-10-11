package com.agentic.platform.domain.exception;

public class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(String workflowId) {
        super("Workflow instance not found with ID: " + workflowId);
    }
}
