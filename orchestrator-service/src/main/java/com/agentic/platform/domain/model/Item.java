package com.agentic.platform.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory_items")
public class Item {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(length = 64)
    private String category;

    @Column(length = 32)
    private String unit;

    @Column(nullable = false)
    private int currentStock = 0;

    @Column(nullable = false)
    private int reservedStock = 0;

    @Column(nullable = false)
    private int minThreshold = 5;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Item() {}

    public Item(String id, String sku, String name, String category, String unit, int currentStock, int reservedStock, int minThreshold) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.currentStock = currentStock;
        this.reservedStock = reservedStock;
        this.minThreshold = minThreshold;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public int getAvailableStock() {
        return Math.max(0, currentStock - reservedStock);
    }

    public boolean isLowStock() {
        return getAvailableStock() <= minThreshold;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getReservedStock() { return reservedStock; }
    public void setReservedStock(int reservedStock) { this.reservedStock = reservedStock; }
    public int getMinThreshold() { return minThreshold; }
    public void setMinThreshold(int minThreshold) { this.minThreshold = minThreshold; }
    public Instant getCreatedAt() { return createdAt; }
}
