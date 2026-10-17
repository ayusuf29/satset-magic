package com.agentic.platform.domain.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "workflow_instances")
public class WorkflowInstance {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 64)
    private String projectId;

    @Column(length = 64)
    private String clientId;

    @Column(nullable = false)
    private String taskName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private WorkflowState currentState;

    @Column(nullable = false)
    private int reviewLoopCount = 0;

    @Column(nullable = false)
    private double coveragePercentage = 0.0;

    // The Triad Council Consensus
    @Column(nullable = false)
    private boolean architectApproved = false;

    @Column(nullable = false)
    private boolean securityApproved = false;

    @Column(nullable = false)
    private boolean performanceApproved = false;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    @Column(length = 64)
    private String lastDecision;

    @Column(columnDefinition = "TEXT")
    private String revisionFeedback;

    @Column(nullable = false)
    private boolean agentRunning = false;

    @Column(length = 128)
    private String currentAgentRole;

    @Column(columnDefinition = "TEXT")
    private String currentAgentActivity;

    @Column(nullable = false)
    private int phaseProgressPercentage = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public WorkflowInstance() {
    }

    public WorkflowInstance(String id, String projectId, String clientId, String taskName, String description,
                            WorkflowState currentState, int reviewLoopCount, double coveragePercentage,
                            boolean architectApproved, boolean securityApproved, boolean performanceApproved,
                            String payloadJson, String lastDecision, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.projectId = projectId != null ? projectId : "PRJ-" + id;
        this.clientId = clientId != null ? clientId : "DEFAULT-CLIENT";
        this.taskName = taskName;
        this.description = description;
        this.currentState = currentState;
        this.reviewLoopCount = reviewLoopCount;
        this.coveragePercentage = coveragePercentage;
        this.architectApproved = architectApproved;
        this.securityApproved = securityApproved;
        this.performanceApproved = performanceApproved;
        this.payloadJson = payloadJson;
        this.lastDecision = lastDecision;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (currentState == null) currentState = WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY;
        if (projectId == null && id != null) projectId = "PRJ-" + id;
        if (clientId == null) clientId = "DEFAULT-CLIENT";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isCouncilConsensusReached() {
        return architectApproved && securityApproved && performanceApproved;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public WorkflowState getCurrentState() { return currentState; }
    public void setCurrentState(WorkflowState currentState) { this.currentState = currentState; }
    public int getReviewLoopCount() { return reviewLoopCount; }
    public void setReviewLoopCount(int reviewLoopCount) { this.reviewLoopCount = reviewLoopCount; }
    public double getCoveragePercentage() { return coveragePercentage; }
    public void setCoveragePercentage(double coveragePercentage) { this.coveragePercentage = coveragePercentage; }
    public boolean isArchitectApproved() { return architectApproved; }
    public void setArchitectApproved(boolean architectApproved) { this.architectApproved = architectApproved; }
    public boolean isSecurityApproved() { return securityApproved; }
    public void setSecurityApproved(boolean securityApproved) { this.securityApproved = securityApproved; }
    public boolean isPerformanceApproved() { return performanceApproved; }
    public void setPerformanceApproved(boolean performanceApproved) { this.performanceApproved = performanceApproved; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getLastDecision() { return lastDecision; }
    public void setLastDecision(String lastDecision) { this.lastDecision = lastDecision; }
    public String getRevisionFeedback() { return revisionFeedback; }
    public void setRevisionFeedback(String revisionFeedback) { this.revisionFeedback = revisionFeedback; }
    public boolean isAgentRunning() { return agentRunning; }
    public void setAgentRunning(boolean agentRunning) { this.agentRunning = agentRunning; }
    public String getCurrentAgentRole() { return currentAgentRole; }
    public void setCurrentAgentRole(String currentAgentRole) { this.currentAgentRole = currentAgentRole; }
    public String getCurrentAgentActivity() { return currentAgentActivity; }
    public void setCurrentAgentActivity(String currentAgentActivity) { this.currentAgentActivity = currentAgentActivity; }
    public int getPhaseProgressPercentage() { return phaseProgressPercentage; }
    public void setPhaseProgressPercentage(int phaseProgressPercentage) { this.phaseProgressPercentage = phaseProgressPercentage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String projectId;
        private String clientId;
        private String taskName;
        private String description;
        private WorkflowState currentState = WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY;
        private int reviewLoopCount = 0;
        private double coveragePercentage = 0.0;
        private boolean architectApproved = false;
        private boolean securityApproved = false;
        private boolean performanceApproved = false;
        private String payloadJson;
        private String lastDecision;
        private String revisionFeedback;
        private boolean agentRunning = false;
        private String currentAgentRole;
        private String currentAgentActivity;
        private int phaseProgressPercentage = 0;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder projectId(String projectId) { this.projectId = projectId; return this; }
        public Builder clientId(String clientId) { this.clientId = clientId; return this; }
        public Builder taskName(String taskName) { this.taskName = taskName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder currentState(WorkflowState currentState) { this.currentState = currentState; return this; }
        public Builder reviewLoopCount(int reviewLoopCount) { this.reviewLoopCount = reviewLoopCount; return this; }
        public Builder coveragePercentage(double coveragePercentage) { this.coveragePercentage = coveragePercentage; return this; }
        public Builder architectApproved(boolean architectApproved) { this.architectApproved = architectApproved; return this; }
        public Builder securityApproved(boolean securityApproved) { this.securityApproved = securityApproved; return this; }
        public Builder performanceApproved(boolean performanceApproved) { this.performanceApproved = performanceApproved; return this; }
        public Builder payloadJson(String payloadJson) { this.payloadJson = payloadJson; return this; }
        public Builder lastDecision(String lastDecision) { this.lastDecision = lastDecision; return this; }
        public Builder revisionFeedback(String revisionFeedback) { this.revisionFeedback = revisionFeedback; return this; }
        public Builder agentRunning(boolean agentRunning) { this.agentRunning = agentRunning; return this; }
        public Builder currentAgentRole(String currentAgentRole) { this.currentAgentRole = currentAgentRole; return this; }
        public Builder currentAgentActivity(String currentAgentActivity) { this.currentAgentActivity = currentAgentActivity; return this; }
        public Builder phaseProgressPercentage(int phaseProgressPercentage) { this.phaseProgressPercentage = phaseProgressPercentage; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public WorkflowInstance build() {
            Instant now = Instant.now();
            WorkflowInstance instance = new WorkflowInstance(
                    id, projectId, clientId, taskName, description,
                    currentState != null ? currentState : WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY,
                    reviewLoopCount, coveragePercentage,
                    architectApproved, securityApproved, performanceApproved,
                    payloadJson, lastDecision,
                    createdAt != null ? createdAt : now,
                    updatedAt != null ? updatedAt : now
            );
            instance.setRevisionFeedback(revisionFeedback);
            instance.setAgentRunning(agentRunning);
            instance.setCurrentAgentRole(currentAgentRole);
            instance.setCurrentAgentActivity(currentAgentActivity);
            instance.setPhaseProgressPercentage(phaseProgressPercentage);
            return instance;
        }
    }
}
