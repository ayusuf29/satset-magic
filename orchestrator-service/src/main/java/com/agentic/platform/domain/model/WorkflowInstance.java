package com.agentic.platform.domain.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "workflow_instances")
public class WorkflowInstance {

    @Id
    @Column(length = 64)
    private String id;

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

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    @Column(length = 64)
    private String lastDecision;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public WorkflowInstance() {
    }

    public WorkflowInstance(String id, String taskName, String description, WorkflowState currentState,
                            int reviewLoopCount, double coveragePercentage, String payloadJson,
                            String lastDecision, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.taskName = taskName;
        this.description = description;
        this.currentState = currentState;
        this.reviewLoopCount = reviewLoopCount;
        this.coveragePercentage = coveragePercentage;
        this.payloadJson = payloadJson;
        this.lastDecision = lastDecision;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (currentState == null) {
            currentState = WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkflowState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(WorkflowState currentState) {
        this.currentState = currentState;
    }

    public int getReviewLoopCount() {
        return reviewLoopCount;
    }

    public void setReviewLoopCount(int reviewLoopCount) {
        this.reviewLoopCount = reviewLoopCount;
    }

    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getLastDecision() {
        return lastDecision;
    }

    public void setLastDecision(String lastDecision) {
        this.lastDecision = lastDecision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String taskName;
        private String description;
        private WorkflowState currentState = WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY;
        private int reviewLoopCount = 0;
        private double coveragePercentage = 0.0;
        private String payloadJson;
        private String lastDecision;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder currentState(WorkflowState currentState) {
            this.currentState = currentState;
            return this;
        }

        public Builder reviewLoopCount(int reviewLoopCount) {
            this.reviewLoopCount = reviewLoopCount;
            return this;
        }

        public Builder coveragePercentage(double coveragePercentage) {
            this.coveragePercentage = coveragePercentage;
            return this;
        }

        public Builder payloadJson(String payloadJson) {
            this.payloadJson = payloadJson;
            return this;
        }

        public Builder lastDecision(String lastDecision) {
            this.lastDecision = lastDecision;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public WorkflowInstance build() {
            Instant now = Instant.now();
            return new WorkflowInstance(
                    id,
                    taskName,
                    description,
                    currentState != null ? currentState : WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY,
                    reviewLoopCount,
                    coveragePercentage,
                    payloadJson,
                    lastDecision,
                    createdAt != null ? createdAt : now,
                    updatedAt != null ? updatedAt : now
            );
        }
    }
}
