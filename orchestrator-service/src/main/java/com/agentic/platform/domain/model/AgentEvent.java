package com.agentic.platform.domain.model;

import java.time.Instant;

public record AgentEvent(
    String id,
    String workflowId,
    WorkflowState state,
    String type,
    String message,
    Instant timestamp
) {
    public static AgentEvent of(String workflowId, WorkflowState state, String type, String message) {
        return new AgentEvent(
            java.util.UUID.randomUUID().toString(),
            workflowId,
            state,
            type,
            message,
            Instant.now()
        );
    }
}
