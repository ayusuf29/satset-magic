package com.agentic.platform.interfaces.rest;

import com.agentic.platform.domain.model.ApprovalAuditLog;
import com.agentic.platform.domain.model.Item;
import com.agentic.platform.domain.model.StockTransaction;
import com.agentic.platform.interfaces.dto.InventoryDtos.*;
import com.agentic.platform.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/items")
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<ItemResponse> items = inventoryService.getAllItems().stream()
                .map(ItemResponse::from)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/items")
    public ResponseEntity<ItemResponse> createItem(@RequestBody CreateItemRequest request) {
        Item item = inventoryService.createItem(
                request.sku(),
                request.name(),
                request.category(),
                request.unit(),
                request.initialStock(),
                request.minThreshold()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.from(item));
    }

    @PostMapping("/transactions")
    public ResponseEntity<StockTransaction> requestTransaction(@RequestBody RequestTransactionRequest request) {
        StockTransaction tx = inventoryService.requestTransaction(
                request.itemId(),
                request.type(),
                request.quantity(),
                request.emergencyJustification(),
                request.requestedBy()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(tx);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<StockTransaction>> getAllTransactions() {
        return ResponseEntity.ok(inventoryService.getAllTransactions());
    }

    @GetMapping("/transactions/pending")
    public ResponseEntity<List<StockTransaction>> getPendingTransactions() {
        return ResponseEntity.ok(inventoryService.getPendingTransactions());
    }

    @PostMapping("/transactions/{id}/approve")
    public ResponseEntity<StockTransaction> approveTransaction(
            @PathVariable String id,
            @RequestBody ProcessApprovalRequest request) {
        StockTransaction tx = inventoryService.processApproval(
                id,
                request.decision() != null ? request.decision() : "APPROVED",
                request.notes(),
                request.approver(),
                request.approverRole()
        );
        return ResponseEntity.ok(tx);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<ApprovalAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(inventoryService.getAuditLogs());
    }
}
