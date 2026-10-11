# RAW INPUT REQUIREMENTS

Tempat menaruh deskripsi ide mentah, transkrip meeting, atau catatan kebutuhan klien.

---

## Kebutuhan Mentah Klien (Input Terkini):
"Buatkan aplikasi web untuk pencatatan inventory sederhana seperti: stock, barang masuk, barang keluar, barang keluar darurat. semua proses mesti melalui approval dari user dengan level lebih tinggi"

### Ringkasan Domain Awal:
1. **Manajemen Stock & Inventaris**:
   - Master data barang, SKU, kategori, lokasi rak/gudang, kuantitas saat ini, minimum stock threshold.
2. **Transaksi Mutasi Barang**:
   - **Barang Masuk (Inbound)**: Penerimaan dari supplier atau pengembalian.
   - **Barang Keluar (Outbound Regular)**: Pengeluaran barang terjual atau pemindahan rutin.
   - **Barang Keluar Darurat (Emergency Outbound)**: Pengeluaran mendesak (e.g. penggantian darurat, kerusakan kritis, bencana, expired) dengan flagging khusus & audit trail ketat.
3. **Mekanisme Approval & Role-Based Access Control (RBAC)**:
   - Level 1 (Operator Gudang / Staf): Input transaksi (draft / submit request).
   - Level 2 (Supervisor / Gudang Lead): Review & verifikasi stok fisik.
   - Level 3 (Manager / Kepala Cabang): Approval akhir, khususnya untuk pengeluaran darurat atau volume besar.
