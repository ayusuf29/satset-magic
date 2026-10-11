package com.agentic.platform.service;

import com.agentic.platform.domain.model.ApprovalAuditLog;
import com.agentic.platform.domain.model.Item;
import com.agentic.platform.domain.model.StockTransaction;

import java.util.List;

public interface InventoryService {
    Item createItem(String sku, String name, String category, String unit, int initialStock, int minThreshold);
    List<Item> getAllItems();
    Item getItemById(String id);
    StockTransaction requestTransaction(String itemId, StockTransaction.Type type, int quantity, String emergencyJustification, String requestedBy);
    StockTransaction processApproval(String transactionId, String decision, String notes, String approver, String approverRole);
    List<StockTransaction> getAllTransactions();
    List<StockTransaction> getPendingTransactions();
    List<ApprovalAuditLog> getAuditLogs();
}
