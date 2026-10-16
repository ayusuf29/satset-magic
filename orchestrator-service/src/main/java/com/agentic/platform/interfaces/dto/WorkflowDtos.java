package com.agentic.platform.interfaces.dto;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.domain.model.WorkflowTransitionLog;

import java.time.Instant;
import java.util.List;

public class WorkflowDtos {

    public record CreateWorkflowRequest(
        String projectId,
        String clientId,
        String taskName,
        String description,
        String initialPayload
    ) {}

    public record HumanDecisionRequest(
        String decision, // "APPROVE" or "REJECT"
        String comment,
        String actor
    ) {}

    public record CouncilReviewResultRequest(
        boolean architectApproved,
        boolean securityApproved,
        boolean performanceApproved,
        double coveragePercentage,
        String feedback
    ) {
        public boolean isConsensusReached() {
            return architectApproved && securityApproved && performanceApproved;
        }
    }

    public record WorkflowSummaryResponse(
        String id,
        String projectId,
        String clientId,
        String taskName,
        String description,
        WorkflowState currentState,
        int reviewLoopCount,
        double coveragePercentage,
        boolean architectApproved,
        boolean securityApproved,
        boolean performanceApproved,
        String lastDecision,
        boolean waitingForHuman,
        boolean agentRunning,
        String currentAgentRole,
        String currentAgentActivity,
        int phaseProgressPercentage,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static WorkflowSummaryResponse from(WorkflowInstance entity) {
            return new WorkflowSummaryResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getClientId(),
                entity.getTaskName(),
                entity.getDescription(),
                entity.getCurrentState(),
                entity.getReviewLoopCount(),
                entity.getCoveragePercentage(),
                entity.isArchitectApproved(),
                entity.isSecurityApproved(),
                entity.isPerformanceApproved(),
                entity.getLastDecision(),
                entity.getCurrentState().isHumanGate(),
                entity.isAgentRunning(),
                entity.getCurrentAgentRole(),
                entity.getCurrentAgentActivity(),
                entity.getPhaseProgressPercentage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
            );
        }
    }

    public record WorkflowDetailResponse(
        String id,
        String projectId,
        String clientId,
        String taskName,
        String description,
        WorkflowState currentState,
        int reviewLoopCount,
        double coveragePercentage,
        boolean architectApproved,
        boolean securityApproved,
        boolean performanceApproved,
        String payloadJson,
        String lastDecision,
        boolean waitingForHuman,
        boolean agentRunning,
        String currentAgentRole,
        String currentAgentActivity,
        int phaseProgressPercentage,
        Instant createdAt,
        Instant updatedAt,
        List<WorkflowTransitionLog> history
    ) {
        public static WorkflowDetailResponse from(WorkflowInstance entity, List<WorkflowTransitionLog> history) {
            return new WorkflowDetailResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getClientId(),
                entity.getTaskName(),
                entity.getDescription(),
                entity.getCurrentState(),
                entity.getReviewLoopCount(),
                entity.getCoveragePercentage(),
                entity.isArchitectApproved(),
                entity.isSecurityApproved(),
                entity.isPerformanceApproved(),
                entity.getPayloadJson(),
                entity.getLastDecision(),
                entity.getCurrentState().isHumanGate(),
                entity.isAgentRunning(),
                entity.getCurrentAgentRole(),
                entity.getCurrentAgentActivity(),
                entity.getPhaseProgressPercentage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                history
            );
        }
    }
}
