package com.agentic.platform.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory_approval_audit_logs")
public class ApprovalAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String transactionId;

    @Column(nullable = false, length = 32)
    private String decision; // "APPROVED" or "REJECTED"

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, length = 64)
    private String approver;

    @Column(nullable = false, length = 32)
    private String approverRole; // "SUPERVISOR" or "MANAGER"

    @Column(nullable = false, updatable = false)
    private Instant decidedAt;

    public ApprovalAuditLog() {}

    public ApprovalAuditLog(String transactionId, String decision, String notes, String approver, String approverRole) {
        this.transactionId = transactionId;
        this.decision = decision;
        this.notes = notes;
        this.approver = approver;
        this.approverRole = approverRole;
        this.decidedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (decidedAt == null) decidedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getDecision() { return decision; }
    public String getNotes() { return notes; }
    public String getApprover() { return approver; }
    public String getApproverRole() { return approverRole; }
    public Instant getDecidedAt() { return decidedAt; }
}
