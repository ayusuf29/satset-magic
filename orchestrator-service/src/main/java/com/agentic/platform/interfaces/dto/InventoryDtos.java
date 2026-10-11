package com.agentic.platform.interfaces.dto;

import com.agentic.platform.domain.model.Item;
import com.agentic.platform.domain.model.StockTransaction;

public class InventoryDtos {

    public record CreateItemRequest(
        String sku,
        String name,
        String category,
        String unit,
        int initialStock,
        int minThreshold
    ) {}

    public record RequestTransactionRequest(
        String itemId,
        StockTransaction.Type type,
        int quantity,
        String emergencyJustification,
        String requestedBy
    ) {}

    public record ProcessApprovalRequest(
        String decision, // "APPROVED" or "REJECTED"
        String notes,
        String approver,
        String approverRole
    ) {}

    public record ItemResponse(
        String id,
        String sku,
        String name,
        String category,
        String unit,
        int currentStock,
        int reservedStock,
        int availableStock,
        int minThreshold,
        boolean lowStock
    ) {
        public static ItemResponse from(Item item) {
            return new ItemResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getCategory(),
                item.getUnit(),
                item.getCurrentStock(),
                item.getReservedStock(),
                item.getAvailableStock(),
                item.getMinThreshold(),
                item.isLowStock()
            );
        }
    }
}
