package com.agentic.platform.interfaces.dto;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.domain.model.WorkflowTransitionLog;

import java.time.Instant;
import java.util.List;

public class WorkflowDtos {

    public record CreateWorkflowRequest(
        String taskName,
        String description,
        String initialPayload
    ) {}

    public record HumanDecisionRequest(
        String decision, // "APPROVE" or "REJECT"
        String comment,
        String actor
    ) {}

    public record CodeReviewResultRequest(
        boolean passed,
        double coveragePercentage,
        String feedback,
        String reviewer
    ) {}

    public record WorkflowSummaryResponse(
        String id,
        String taskName,
        String description,
        WorkflowState currentState,
        int reviewLoopCount,
        double coveragePercentage,
        String lastDecision,
        boolean waitingForHuman,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static WorkflowSummaryResponse from(WorkflowInstance entity) {
            return new WorkflowSummaryResponse(
                entity.getId(),
                entity.getTaskName(),
                entity.getDescription(),
                entity.getCurrentState(),
                entity.getReviewLoopCount(),
                entity.getCoveragePercentage(),
                entity.getLastDecision(),
                entity.getCurrentState().isHumanGate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
            );
        }
    }

    public record WorkflowDetailResponse(
        String id,
        String taskName,
        String description,
        WorkflowState currentState,
        int reviewLoopCount,
        double coveragePercentage,
        String payloadJson,
        String lastDecision,
        boolean waitingForHuman,
        Instant createdAt,
        Instant updatedAt,
        List<WorkflowTransitionLog> history
    ) {
        public static WorkflowDetailResponse from(WorkflowInstance entity, List<WorkflowTransitionLog> history) {
            return new WorkflowDetailResponse(
                entity.getId(),
                entity.getTaskName(),
                entity.getDescription(),
                entity.getCurrentState(),
                entity.getReviewLoopCount(),
                entity.getCoveragePercentage(),
                entity.getPayloadJson(),
                entity.getLastDecision(),
                entity.getCurrentState().isHumanGate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                history
            );
        }
    }
}
