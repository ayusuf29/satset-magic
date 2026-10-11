# CURATED REQUIREMENT DOCUMENT (PHASE 0 OUTPUT)

<!-- STATUS: PENDING_HUMAN_APPROVAL -->
<!-- Ubah status di atas menjadi APPROVED_BY_HUMAN setelah Anda meninjau dokumen ini -->

## 1. Executive Summary & Objective
Aplikasi web pencatatan inventaris cerdas (*Smart Inventory Management*) berbasis hierarki persetujuan bertingkat (*Multi-Level Approval Workflow*). Sistem menjamin setiap perpindahan barang (masuk, keluar reguler, dan keluar darurat) tercatat secara transparan dengan jejak audit (*audit trail*) yang tidak dapat dimanipulasi.

---

## 2. Scope Fitur Terverifikasi

### Modul A: Master Data & Stock Tracking
1. **Pencatatan Master Barang**:
   - Kode Barang (SKU unik), Nama Barang, Kategori, Satuan Unit (Pcs, Box, Kg), Lokasi Rak/Gudang.
   - Ambang Batas Minimum (*Safety Stock Threshold*) dengan peringatan otomatis bila stok menipis.
2. **Real-time Stock Ledger**:
   - Menampilkan stok on-hand, stok yang dialokasikan (*reserved for pending approval*), dan stok tersedia (*available*).

### Modul B: Transaksi Mutasi & Tipe Pengeluaran
1. **Barang Masuk (Inbound Transaction)**:
   - Pencatatan asal supplier/PO, nomor surat jalan, kuantitas masuk, tanggal kedatangan.
   - Status: `DRAFT` $\rightarrow$ `SUBMITTED` $\rightarrow$ `APPROVED` $\rightarrow$ `STOCK_ADDED`.
2. **Barang Keluar Reguler (Standard Outbound)**:
   - Pencatatan keperluan distribusi/penjualan/transfer divisi.
   - Pengecekan ketersediaan stok sebelum submit (mencegah *overselling/negative stock*).
   - Status: `SUBMITTED` $\rightarrow$ `SUPERVISOR_APPROVED` $\rightarrow$ `STOCK_DEDUCTED`.
3. **Barang Keluar Darurat (Emergency Outbound)**:
   - Ditujukan untuk skenario mendesak: perbaikan mesin kritis, insiden darurat, bencana, atau penggantian garansi mendesak.
   - Wajib mencantumkan alasan darurat (*Justification Note*), penanggung jawab insiden, dan estimasi dampak.
   - Notifikasi prioritas tinggi (*High Priority Push*) ke Manager.
   - Memiliki audit tag khusus `[EMERGENCY_DISPATCH]`.

### Modul C: Multi-Level Approval Workflow & RBAC
1. **Peran Pengguna (Roles)**:
   - **Staff / Gudang Operator**: Membuat draf transaksi mutasi barang.
   - **Supervisor Gudang**: Verifikasi kuantitas fisik dan persetujuan level 1 (Barang Masuk & Keluar Reguler).
   - **Manager / Kepala Cabang**: Persetujuan level tertinggi, wajib untuk **Barang Keluar Darurat** dan mutasi bernilai tinggi.
2. **Alur Persetujuan**:
   - Transaksi berstatus `PENDING_APPROVAL` membekukan stok sementara (*Reserved*).
   - Approver dapat memilih: `[APPROVE]`, `[REJECT dengan Alasan]`, atau `[REQUEST CLARIFICATION]`.
   - Update stok fisik ke ledger hanya dieksekusi setelah status approval final `APPROVED`.

---

## 3. Acceptance Criteria (Format Gherkin)

```gherkin
Feature: Mutasi Barang Keluar Darurat dengan Approval Bertingkat

  Scenario: Staff mengajukan barang keluar darurat
    Given Staff login ke dashboard inventory
    And Stok barang "SPAREPART-VALVE-01" tersedia 10 unit
    When Staff membuat transaksi "Barang Keluar Darurat" sejumlah 2 unit dengan alasan "Kebocoran pipa pendingin lini 1"
    Then Transaksi tersimpan dengan status "PENDING_MANAGER_APPROVAL"
    And Stok reserved bertambah menjadi 2 unit dan stok available menjadi 8 unit
    And Manager menerima notifikasi darurat

  Scenario: Manager menyetujui pengeluaran darurat
    Given Terdapat transaksi pengeluaran darurat berstatus "PENDING_MANAGER_APPROVAL"
    When Manager meninjau detail alasan dan mengklik tombol "Approve"
    Then Status transaksi berubah menjadi "APPROVED"
    And Total stok fisik berkurang 2 unit menjadi 8 unit
    And Catatan audit trail mencatat persetujuan oleh Manager lengkap dengan timestamp

  Scenario: Supervisor menolak pengeluaran karena stok fisik tidak cocok
    Given Terdapat transaksi barang keluar reguler berstatus "PENDING_SUPERVISOR_APPROVAL"
    When Supervisor mengklik "Reject" dengan catatan "Fisik barang di rak rusak"
    Then Status transaksi berubah menjadi "REJECTED"
    And Stok reserved dilepaskan kembali ke stok available
    And Staff pembuat transaksi menerima notifikasi penolakan
```

---

## 4. Out-of-Scope (Excluded Features untuk Fase Awal)
- Integrasi sistem akuntansi / invoicing perpajakan (akan dijadwalkan pada rilis lanjutan).
- Barcode scanning kamera perangkat mobile hardware Zebra/Honeywell (menggunakan manual entry / input teks terlebih dahulu).

---

## 5. Persetujuan Manusia (Sign-off)
- **Reviewer**: Product Lead / Human Decision Maker
- **Tanggal**: 2026-10-11
- **Status Keputusan**: `PENDING_HUMAN_APPROVAL`
