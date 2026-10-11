package com.agentic.platform.domain.exception;

import com.agentic.platform.domain.model.WorkflowState;

public class InvalidWorkflowTransitionException extends RuntimeException {
    public InvalidWorkflowTransitionException(WorkflowState from, WorkflowState to, String reason) {
        super(String.format("Invalid transition from %s to %s. Reason: %s", from, to, reason));
    }
}
