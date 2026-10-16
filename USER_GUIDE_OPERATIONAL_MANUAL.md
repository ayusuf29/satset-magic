# PANDUAN PENGGUNA & MANUAL OPERASIONAL SATSET ORCHESTRATOR
*(Human-in-the-Loop Operational Guide & End-to-End User Manual)*

---

## 1. Pendahuluan & Filosofi Sistem

Platform **Satset Orchestrator** adalah sistem rekayasa perangkat lunak otonom (*Autonomous Software Engineering*) berbasis **5-Team Topology** dan **10 SDLC Domain Skills**. 

### Pembagian Peran:
* **Autonomous AI Agents**: Melakukan 90% pekerjaan teknis yang repetitif dan berat (merumuskan PRD, merancang arsitektur C4, TDD test suite, penulisan kode Java & Next.js, audit keamanan SAST/DAST, dan benchmarking k6).
* **Human Operator (Anda / Manusia)**: Bertindak sebagai **Chief Gatekeeper & Decision Maker**. Sistem tidak akan pernah merilis fitur ke produksi tanpa persetujuan eksplisit Anda pada titik-titik gerbang kritis (*Governance Gates*).

---

## 2. Cara Menjalankan Platform

### Opsi A: Menggunakan Docker Compose (Direkomendasikan untuk Production & Staging)
Cukup jalankan satu perintah di root folder:
```bash
docker compose up -d --build
```
Stack container yang otomatis berjalan:
1. `agentic-postgres` (Port 5432) — Checkpointer state machine & audit log.
2. `agentic-redis` (Port 6379) — Realtime event queue & cache.
3. `agentic-orchestrator` (Port 8080) — Java 21 Spring Boot + LangChain4j engine.
4. `agentic-dashboard` (Port 3000) — Mobile-First Next.js Command Hub.

### Opsi B: Menjalankan Lokal (Development Mode)
1. **Jalankan Backend (Terminal 1)**:
   ```bash
   cd orchestrator-service
   java -jar target/orchestrator-service-1.0.0-SNAPSHOT.jar
   ```
   *(Backend aktif di `http://localhost:8080`)*
2. **Jalankan Dashboard (Terminal 2)**:
   ```bash
   cd dashboard-ui
   npm run start
   ```
   *(Dashboard aktif di `http://localhost:3000`)*

---

## 3. Peta Siklus 10 Fase SDLC & Tindakan Manusia

| Fase | Nama Fase | Keterlibatan Deep Council (Shared Enabling) | Tindakan yang Wajib Dilakukan Manusia (User) |
| :---: | :--- | :--- | :--- |
| **0** | **Requirement Discovery** | **AppSec + Architect**: Review threat surface & domain boundary awal di `workspace/projects/{projectId}/council-audits/phase0-threat-model.md`. | **[GERBANG MANUSIA 1]**: Tinjau hasil kurasi kebutuhan & audit threat surface. Klik **"Setujui & Lanjutkan"** jika sudah sesuai, atau isi catatan revisi lalu klik **"Tolak"**. |
| **1** | **Architecture & Design** | **System Architect**: C4 Component diagram, state machine checkpointer, & ADR. **AppSec**: STRIDE Threat Model. **Perf**: SLA latency budget. | Memantau diagram via dashboard. Sistem otomatis melangkah jika spesifikasi arsitektur lengkap. |
| **2** | **Backlog Generation** | **Enabling Team**: Menyelipkan acceptance criteria teknis (misal: OWASP token validation, database indexing) ke setiap task backlog. | Memeriksa estimasi beban kerja bila diperlukan. |
| **3** | **Task Dispatching** | Mengalokasikan tugas implementasi secara paralel ke worker agent (Backend, Frontend, QA). | Memantau pembagian beban di kartu 5-Team Topology. |
| **4** | **TDD Planning** | **TDD Orchestrator + Security**: Memastikan test suite mencakup security test (negative test, role bypass test) sebelum penulisan kode. | Tidak ada tindakan manual (otomatis oleh TDD Orchestrator). |
| **5** | **Development** | Menulis kode logika bisnis, REST controller, dan antarmuka komponen UI sesuai aturan arsitektur & secure coding. | Mengamati progress commit dan live telemetry terminal. |
| **6** | **Code Review Consortium** | **TRIAD COUNCIL VOTING GATE**: Konsensus wajib 3 pilar: $Consortium = Architect \land Security \land Performance$ plus Coverage $\ge 70\%$. | **[GERBANG MANUSIA 2 - CODE REVIEW & MERGE TO MAIN]**: Branch `main` diproteksi penuh dari bot otonom. Setelah Triad Council memberikan konsensus 3/3 PASS, pimpinan teknis (manusia) wajib memverifikasi dan memberikan otorisasi penggabungan PR ke `main` (via Dashboard atau tombol inline Telegram). Serta **[GERBANG DEADLOCK]** jika perbaikan gagal 3x berturut-turut. |
| **7** | **QA E2E Verification** | Menjalankan suite pengujian end-to-end sandbox. | Memverifikasi bug report (harus 0 blocker). |
| **8** | **Pre-Production Gate (DAST/MAST)** | **DAST/Pentest Scout**: Dynamic fuzzing OWASP ZAP, stress test k6 (P95 $< 200\text{ms}$), dan audit keamanan client mobile (MAST). | **[GERBANG MANUSIA 3 - RELEASE GATE]**: Tinjau laporan verifikasi di `workspace/projects/{projectId}/qa/PRE_PROD_VERIFICATION.md`. Berikan otorisasi rilis produksi. |
| **9** | **Released (Live Production)** | Menyelesaikan siklus rilis dan mengarsipkan audit trail terisolasi. | Fitur resmi aktif dan siap dikonsumsi pengguna akhir! |

---

## 3.1 Cara Memastikan Council & Shared Enabling Terlibat Mendalam (Bahkan untuk Requirement Sederhana)

Banyak tim terjebak pada ilusi *"Requirement sederhana tidak butuh arsitek atau security"*. Contoh kasus inventory sederhana:
> *"Buatkan sistem inventory: stock, barang masuk, barang keluar, barang keluar darurat. Semua perlu approval user level lebih tinggi."*

Meskipun terdengar sederhana, **risiko bisnis dan keamanannya sangat tinggi**:
1. **Security Risk**: Apakah user biasa dapat mem-bypass approval dan melakukan transaksi "barang keluar darurat"? (OWASP A01: Broken Access Control).
2. **Architecture Risk**: Apakah transaksi stok konkuren rentan terhadap race-condition (misal: dua user mengeluarkan barang sisa 1 secara bersamaan)? Apakah state machine approval tercatat secara idempotent?
3. **Performance Risk**: Apakah ledger tabel mutasi stok memiliki index yang tepat saat transaksi mencapai puluhan ribu record?

### Checklist Tindakan Operator Manusia:
1. **Di Fase 0 (Sebelum Sign-off Kebutuhan)**:
   - Jangan langsung klik *"Setujui"*. Buka dashboard dan periksa tab arsitektur & security.
   - Pastikan agent PO telah memasukkan non-functional requirements (NFR): otorisasi role, audit trail mutasi, dan idempotency key.
2. **Di Fase 1 (Review Desain Arsitektur)**:
   - Periksa file `workspace/projects/{projectId}/architecture/c4-model.md`. Pastikan model relasi data memisahkan `InventoryItem` dari `InventoryApprovalRequest` untuk mencegah bypass status.
3. **Di Fase 6 (Consensus Ballot di Dashboard)**:
   - Dashboard sekarang menyediakan widget **"Cross-Cutting Council Triad Matrix"** dengan 3 tombol approval mandiri:
     - 🏛️ **System Architect**: Memvalidasi modular boundary dan zero anti-patterns.
     - 🛡️ **AppSec Auditor**: Memvalidasi hasil SAST scanner (OWASP Top 10 = 0 critical).
     - ⚡ **Perf & DAST Scout**: Memvalidasi efisiensi query & test coverage $\ge 70\%$.
   - Jika salah satu pilar menolak (veto), sistem otomatis mengulang fase pengembangan (maksimal 3 kali sebelum eskalasi Deadlock).

---

## 3.2 Dukungan Multi-Project & Multi-Tenant Terisolasi

Sistem kini tidak lagi menimpa berkas global `docs/RAW_INPUT.md` atau `docs/REQUIREMENT.md`. Setiap kali Anda memulai task melalui Dashboard atau API:
1. Anda dapat menyertakan `Project ID` (misal: `PRJ-INVENTORY-PROD`) dan `Client / Tenant` (misal: `CLIENT-ACME`).
2. Orchestrator secara otomatis mengalokasikan direktori terisolasi:
   ```text
   workspace/
   └── projects/
       └── PRJ-INVENTORY-PROD/
           ├── raw_input.md
           ├── architecture/
           ├── council-audits/
           └── backlog/
   ```
3. Setiap proyek memiliki riwayat audit trail, ADR, dan hasil review council masing-masing tanpa tumpang tindih.

---

## 4. Panduan Operasional Manusia: Langkah Demi Langkah

### Langkah 1: Memasukkan Ide / Kebutuhan Mentah
1. Buka file `docs/RAW_INPUT.md` atau klik tombol **"+ Task Baru"** di pojok kanan atas Dashboard (`http://localhost:3000`).
2. Masukkan deskripsi kebutuhan Anda dalam bahasa sehari-hari. Contoh:
   > *"Buatkan aplikasi web untuk pencatatan inventory sederhana seperti: stock, barang masuk, barang keluar, barang keluar darurat. Semua proses mesti melalui approval dari user dengan level lebih tinggi."*
3. Klik **"Eksekusi Pipeline"**. Agen PO/BA akan langsung bekerja menyusun PRD.

---

### Langkah 2: Menghadapi Gerbang Persetujuan (Human-in-the-Loop)
Saat workflow mencapai status **Waiting Human Sign-off**, akan muncul kartu berbingkai emas/kuning di dashboard Anda:

```text
┌────────────────────────────────────────────────────────────────────────┐
│ ⚠️ SIGN-OFF KEBUTUHAN (PHASE 0): Persetujuan User Stories             │
│ ID: wf-48a3b761                                                       │
│                                                                        │
│ Spesifikasi bisnis dan kriteria penerimaan Gherkin telah disusun       │
│ oleh PO/BA agent. Mohon tinjau untuk lanjut ke arsitektur.             │
│                                                                        │
│ [ Input Catatan / Arahan Revisi Khusus .............................. ]│
│                                                                        │
│   [ ✅ Setujui & Lanjutkan ]       [ ❌ Tolak dengan Catatan ]         │
└────────────────────────────────────────────────────────────────────────┘
```

* **Jika Setuju**: Anda dapat langsung mengklik tombol hijau **"Setujui & Lanjutkan"**.
* **Jika Ada yang Kurang**: Tuliskan catatan Anda di kolom teks (misal: *"Tambahkan validasi tidak boleh ada stok negatif"*), lalu klik **"Tolak dengan Catatan"**. Agen akan secara otonom merevisi dokumen PRD sesuai instruksi Anda!

---

### Langkah 3: Penanganan Situasi Khusus (Deadlock Review 3x)
Jika Developer Agent dan Code Reviewer mengalami kebuntuan kualitas kode sebanyak 3 kali perulangan:
1. Banner merah berkedip **DEADLOCK (3x GAGAL)** akan muncul.
2. Anda menerima notifikasi darurat.
3. Sebagai pimpinan teknis, Anda memiliki 2 opsi:
   - **Override & Loloskan**: Jika Anda menilai temuan review bersifat minor dan aman dilanjutkan ke QA.
   - **Tolak dengan Catatan**: Memberikan instruksi arsitektural spesifik agar developer agent keluar dari kebuntuan.

---

### Langkah 4: Otorisasi Rilis Produksi (Phase 8.5 Release Gate)
Sebelum kode dideploy ke server produksi, kartu **GERBANG PRODUKSI** akan muncul.
1. Periksa laporan DAST & Stress test di terminal atau file `workspace/qa/PRE_PROD_VERIFICATION.md`.
2. Pastikan metrik memenuhi standar:
   - Kerentanan High/Critical: **0**
   - Latensi P95: **$< 200\text{ ms}$**
   - Error Rate: **0%**
3. Klik **"Setujui & Lanjutkan"**. Sistem akan menandai status rilis akhir: `PHASE_9_RELEASED`.

---

## 5. Fitur Akses & Telemetri Tambahan

### A. Akses Melalui Smartphone (Mobile-First)
* Buka browser di smartphone Anda dan arahkan ke alamat dashboard (`http://<ip-lokal>:3000`).
* Gunakan **Bottom Navigation Bar** di bagian bawah layar untuk berpindah antara:
  - **Overview**: Melihat seluruh task dan kartu approval.
  - **Pipeline**: Tracking posisi fase task secara horizontal.
  - **Teams**: Melihat tim agen mana yang sedang aktif bekerja (*Active/Idle*).
  - **Console**: Membaca streaming log terminal secara real-time.

### B. Integrasi Notifikasi Telegram Bot
Jika Anda mengonfigurasikan environment variable `TELEGRAM_BOT_TOKEN` dan `TELEGRAM_CHAT_ID`:
* Bot Telegram akan mengirimkan pesan otomatis saat approval dibutuhkan.
* Anda dapat menekan tombol inline langsung di aplikasi Telegram:
  - `[✅ APPROVE]` $\rightarrow$ Menyetujui dari jauh.
  - `[❌ REJECT]` $\rightarrow$ Menolak dari jauh.
  - `[📝 VIEW DETAIL]` $\rightarrow$ Membuka tautan dashboard web.

---

## 6. Lokasi Berkas Output Kerja Agen

Semua artefak yang dihasilkan agen tersimpan rapi dan dapat Anda buka kapan saja di folder workspace:
* **Standar Rationale & Patterns**: [`workspace/PHASE_EXECUTION_STANDARD.md`](file:///c:/project/satset-magic/workspace/PHASE_EXECUTION_STANDARD.md)
* **Checklist 17 Skills & Tools Registry**: [`docs/SKILL_LIST_TOOLS.md`](file:///c:/project/satset-magic/docs/SKILL_LIST_TOOLS.md)
* **Spesifikasi & Gherkin**: `docs/REQUIREMENT.md` dan `workspace/projects/{projectId}/backlog/`
* **Arsitektur C4 & Schema Database**: `workspace/projects/{projectId}/architecture/c4-model.md`
* **Sprint Backlog**: `workspace/projects/{projectId}/backlog/SPRINT_BACKLOG.md`
* **TDD & Test Suites**: `workspace/projects/{projectId}/testing/TDD_PLAN_AND_SUITE.md`
* **Laporan Pengujian Keamanan & Uji Beban**: `workspace/projects/{projectId}/qa/PRE_PROD_VERIFICATION.md`
* **Source Code Backend**: `orchestrator-service/src/`
* **Source Code Frontend**: `dashboard-ui/src/`

