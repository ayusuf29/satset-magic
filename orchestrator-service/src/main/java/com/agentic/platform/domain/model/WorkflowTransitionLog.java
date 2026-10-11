package com.agentic.platform.domain.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "workflow_transition_logs")
public class WorkflowTransitionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String workflowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private WorkflowState fromState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private WorkflowState toState;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public WorkflowTransitionLog() {
    }

    public WorkflowTransitionLog(Long id, String workflowId, WorkflowState fromState, WorkflowState toState,
                                 String actor, String message, Instant createdAt) {
        this.id = id;
        this.workflowId = workflowId;
        this.fromState = fromState;
        this.toState = toState;
        this.actor = actor;
        this.message = message;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public WorkflowState getFromState() {
        return fromState;
    }

    public void setFromState(WorkflowState fromState) {
        this.fromState = fromState;
    }

    public WorkflowState getToState() {
        return toState;
    }

    public void setToState(WorkflowState toState) {
        this.toState = toState;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String workflowId;
        private WorkflowState fromState;
        private WorkflowState toState;
        private String actor;
        private String message;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder workflowId(String workflowId) {
            this.workflowId = workflowId;
            return this;
        }

        public Builder fromState(WorkflowState fromState) {
            this.fromState = fromState;
            return this;
        }

        public Builder toState(WorkflowState toState) {
            this.toState = toState;
            return this;
        }

        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public WorkflowTransitionLog build() {
            return new WorkflowTransitionLog(
                    id,
                    workflowId,
                    fromState,
                    toState,
                    actor,
                    message,
                    createdAt != null ? createdAt : Instant.now()
            );
        }
    }
}
