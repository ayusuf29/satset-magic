package com.agentic.platform.service;

import com.agentic.platform.domain.model.Item;
import com.agentic.platform.domain.model.StockTransaction;
import com.agentic.platform.domain.repository.ApprovalAuditLogRepository;
import com.agentic.platform.domain.repository.ItemRepository;
import com.agentic.platform.domain.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private StockTransactionRepository transactionRepository;

    @Mock
    private ApprovalAuditLogRepository auditLogRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(itemRepository, transactionRepository, auditLogRepository);
        lenient().when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Pengajuan barang keluar reguler harus melakukan reservasi stok (Soft Reservation)")
    void shouldReserveStockOnOutboundRegular() {
        Item item = new Item("item-1", "VALVE-01", "Ball Valve", "Pcs", "Pcs", 20, 0, 5);
        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));

        StockTransaction tx = inventoryService.requestTransaction(
                "item-1",
                StockTransaction.Type.OUTBOUND_REGULAR,
                5,
                null,
                "Operator Gudang"
        );

        assertNotNull(tx);
        assertEquals(StockTransaction.Status.PENDING_SUPERVISOR, tx.getStatus());
        assertEquals(5, item.getReservedStock());
        assertEquals(15, item.getAvailableStock());
    }

    @Test
    @DisplayName("Pengajuan barang keluar darurat harus mencantumkan justifikasi dan langsung eskalasi ke Manager")
    void shouldEscalateEmergencyOutboundToManager() {
        Item item = new Item("item-2", "PIPE-01", "High Pressure Pipe", "Perpipaan", "Batang", 10, 0, 2);
        when(itemRepository.findById("item-2")).thenReturn(Optional.of(item));

        StockTransaction tx = inventoryService.requestTransaction(
                "item-2",
                StockTransaction.Type.OUTBOUND_EMERGENCY,
                2,
                "Kebocoran pipa pendingin kritis unit 2",
                "Teknisi Lapangan"
        );

        assertNotNull(tx);
        assertEquals(StockTransaction.Status.PENDING_MANAGER, tx.getStatus());
        assertEquals("Kebocoran pipa pendingin kritis unit 2", tx.getEmergencyJustification());
        assertEquals(2, item.getReservedStock());
    }

    @Test
    @DisplayName("Harus melempar exception jika pengajuan melebihi stok yang tersedia")
    void shouldThrowExceptionWhenStockInsufficient() {
        Item item = new Item("item-3", "MOTOR-01", "Servo Motor", "Unit", "Unit", 5, 3, 1);
        // Current: 5, Reserved: 3 => Available: 2
        when(itemRepository.findById("item-3")).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () ->
                inventoryService.requestTransaction("item-3", StockTransaction.Type.OUTBOUND_REGULAR, 3, null, "Staff")
        );
    }

    @Test
    @DisplayName("Persetujuan transaksi harus memotong stok fisik dan membersihkan reservasi")
    void shouldDeductStockUponApproval() {
        Item item = new Item("item-4", "VALVE-01", "Ball Valve", "Pcs", "Pcs", 20, 5, 5);
        when(itemRepository.findById("item-4")).thenReturn(Optional.of(item));

        StockTransaction tx = new StockTransaction(
                "tx-1", "TRX-001", "item-4", StockTransaction.Type.OUTBOUND_REGULAR, 5,
                StockTransaction.Status.PENDING_SUPERVISOR, null, "Staff"
        );
        when(transactionRepository.findById("tx-1")).thenReturn(Optional.of(tx));

        StockTransaction approved = inventoryService.processApproval("tx-1", "APPROVED", "Disetujui lengkap", "Supervisor", "SUPERVISOR");

        assertEquals(StockTransaction.Status.APPROVED, approved.getStatus());
        assertEquals(15, item.getCurrentStock());
        assertEquals(0, item.getReservedStock());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Penolakan transaksi harus melepaskan reservasi stok kembali ke status available")
    void shouldReleaseReservationUponRejection() {
        Item item = new Item("item-5", "VALVE-01", "Ball Valve", "Pcs", "Pcs", 20, 5, 5);
        when(itemRepository.findById("item-5")).thenReturn(Optional.of(item));

        StockTransaction tx = new StockTransaction(
                "tx-2", "TRX-002", "item-5", StockTransaction.Type.OUTBOUND_REGULAR, 5,
                StockTransaction.Status.PENDING_SUPERVISOR, null, "Staff"
        );
        when(transactionRepository.findById("tx-2")).thenReturn(Optional.of(tx));

        StockTransaction rejected = inventoryService.processApproval("tx-2", "REJECTED", "Stok fisik di rak rusak", "Supervisor", "SUPERVISOR");

        assertEquals(StockTransaction.Status.REJECTED, rejected.getStatus());
        assertEquals(20, item.getCurrentStock());
        assertEquals(0, item.getReservedStock());
        assertEquals(20, item.getAvailableStock());
    }
}
