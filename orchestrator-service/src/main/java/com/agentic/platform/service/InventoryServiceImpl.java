package com.agentic.platform.service;

import com.agentic.platform.domain.model.ApprovalAuditLog;
import com.agentic.platform.domain.model.Item;
import com.agentic.platform.domain.model.StockTransaction;
import com.agentic.platform.domain.repository.ApprovalAuditLogRepository;
import com.agentic.platform.domain.repository.ItemRepository;
import com.agentic.platform.domain.repository.StockTransactionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ItemRepository itemRepository;
    private final StockTransactionRepository transactionRepository;
    private final ApprovalAuditLogRepository auditLogRepository;

    public InventoryServiceImpl(
            ItemRepository itemRepository,
            StockTransactionRepository transactionRepository,
            ApprovalAuditLogRepository auditLogRepository) {
        this.itemRepository = itemRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @PostConstruct
    public void seedInitialData() {
        if (itemRepository.count() == 0) {
            log.info("Seeding initial inventory master items...");
            createItem("VALVE-SS-01", "Ball Valve Stainless 2 Inch", "Sparepart Mesin", "Pcs", 25, 5);
            createItem("PIPE-HDPE-10", "Pipa HDPE High Pressure 10m", "Perpipaan", "Batang", 40, 8);
            createItem("SEAL-VITON-04", "Gasket Seal Ring Viton Ø100mm", "Sealing", "Pcs", 15, 3);
            createItem("MOTOR-SERV-22", "Servo Motor 750W 220V Emergency Unit", "Elektrikal", "Unit", 6, 2);
        }
    }

    @Override
    @Transactional
    public Item createItem(String sku, String name, String category, String unit, int initialStock, int minThreshold) {
        String id = "item-" + UUID.randomUUID().toString().substring(0, 8);
        Item item = new Item(id, sku, name, category, unit, initialStock, 0, minThreshold);
        return itemRepository.save(item);
    }

    @Override
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    @Override
    public Item getItemById(String id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Barang tidak ditemukan dengan ID: " + id));
    }

    @Override
    @Transactional
    public StockTransaction requestTransaction(String itemId, StockTransaction.Type type, int quantity, String emergencyJustification, String requestedBy) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Jumlah kuantitas harus lebih dari 0.");
        }

        Item item = getItemById(itemId);

        StockTransaction.Status initialStatus;
        if (type == StockTransaction.Type.INBOUND) {
            initialStatus = StockTransaction.Status.PENDING_SUPERVISOR;
        } else if (type == StockTransaction.Type.OUTBOUND_REGULAR) {
            if (item.getAvailableStock() < quantity) {
                throw new IllegalArgumentException(String.format(
                        "Stok tidak mencukupi! Kuantitas diminta: %d, Stok tersedia: %d",
                        quantity, item.getAvailableStock()
                ));
            }
            // Soft reservation
            item.setReservedStock(item.getReservedStock() + quantity);
            itemRepository.save(item);
            initialStatus = StockTransaction.Status.PENDING_SUPERVISOR;
        } else if (type == StockTransaction.Type.OUTBOUND_EMERGENCY) {
            if (item.getAvailableStock() < quantity) {
                throw new IllegalArgumentException(String.format(
                        "Stok fisik darurat tidak mencukupi! Kuantitas diminta: %d, Stok tersedia: %d",
                        quantity, item.getAvailableStock()
                ));
            }
            if (emergencyJustification == null || emergencyJustification.trim().isBlank()) {
                throw new IllegalArgumentException("Alasan darurat (justification) wajib diisi untuk transaksi keluar darurat!");
            }
            // Soft reservation & direct escalation to Manager
            item.setReservedStock(item.getReservedStock() + quantity);
            itemRepository.save(item);
            initialStatus = StockTransaction.Status.PENDING_MANAGER;
        } else {
            throw new IllegalArgumentException("Tipe transaksi tidak valid.");
        }

        String txId = "tx-" + UUID.randomUUID().toString().substring(0, 8);
        String txNumber = "TRX-" + System.currentTimeMillis();
        StockTransaction transaction = new StockTransaction(
                txId, txNumber, itemId, type, quantity, initialStatus, emergencyJustification, requestedBy
        );

        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public StockTransaction processApproval(String transactionId, String decision, String notes, String approver, String approverRole) {
        StockTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan dengan ID: " + transactionId));

        if (tx.getStatus() != StockTransaction.Status.PENDING_SUPERVISOR && tx.getStatus() != StockTransaction.Status.PENDING_MANAGER) {
            throw new IllegalStateException("Transaksi ini sudah diproses sebelumnya (Status: " + tx.getStatus() + ")");
        }

        Item item = getItemById(tx.getItemId());
        boolean isApproved = "APPROVED".equalsIgnoreCase(decision);

        if (isApproved) {
            if (tx.getType() == StockTransaction.Type.INBOUND) {
                item.setCurrentStock(item.getCurrentStock() + tx.getQuantity());
            } else {
                // Outbound regular or emergency
                item.setCurrentStock(item.getCurrentStock() - tx.getQuantity());
                item.setReservedStock(Math.max(0, item.getReservedStock() - tx.getQuantity()));
            }
            tx.setStatus(StockTransaction.Status.APPROVED);
        } else {
            // Rejected
            if (tx.getType() != StockTransaction.Type.INBOUND) {
                // Release soft reservation
                item.setReservedStock(Math.max(0, item.getReservedStock() - tx.getQuantity()));
            }
            tx.setStatus(StockTransaction.Status.REJECTED);
        }

        itemRepository.save(item);
        StockTransaction savedTx = transactionRepository.save(tx);

        ApprovalAuditLog auditLog = new ApprovalAuditLog(
                transactionId,
                isApproved ? "APPROVED" : "REJECTED",
                notes,
                approver != null ? approver : "USER",
                approverRole != null ? approverRole : "SUPERVISOR"
        );
        auditLogRepository.save(auditLog);

        return savedTx;
    }

    @Override
    public List<StockTransaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<StockTransaction> getPendingTransactions() {
        List<StockTransaction> p1 = transactionRepository.findByStatusOrderByCreatedAtDesc(StockTransaction.Status.PENDING_SUPERVISOR);
        List<StockTransaction> p2 = transactionRepository.findByStatusOrderByCreatedAtDesc(StockTransaction.Status.PENDING_MANAGER);
        p1.addAll(p2);
        return p1;
    }

    @Override
    public List<ApprovalAuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByDecidedAtDesc();
    }
}
