package com.agentic.platform.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory_transactions")
public class StockTransaction {

    public enum Type {
        INBOUND,
        OUTBOUND_REGULAR,
        OUTBOUND_EMERGENCY
    }

    public enum Status {
        PENDING_SUPERVISOR,
        PENDING_MANAGER,
        APPROVED,
        REJECTED
    }

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String transactionNumber;

    @Column(nullable = false, length = 64)
    private String itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Type type;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String emergencyJustification;

    @Column(length = 64)
    private String requestedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public StockTransaction() {}

    public StockTransaction(String id, String transactionNumber, String itemId, Type type, int quantity,
                            Status status, String emergencyJustification, String requestedBy) {
        this.id = id;
        this.transactionNumber = transactionNumber;
        this.itemId = itemId;
        this.type = type;
        this.quantity = quantity;
        this.status = status;
        this.emergencyJustification = emergencyJustification;
        this.requestedBy = requestedBy;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTransactionNumber() { return transactionNumber; }
    public void setTransactionNumber(String transactionNumber) { this.transactionNumber = transactionNumber; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getEmergencyJustification() { return emergencyJustification; }
    public void setEmergencyJustification(String emergencyJustification) { this.emergencyJustification = emergencyJustification; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
