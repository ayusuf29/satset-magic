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

| Fase | Nama Fase | Apa yang Dilakukan Agen | Tindakan yang Wajib Dilakukan Manusia (User) |
| :---: | :--- | :--- | :--- |
| **0** | **Requirement Discovery** | Menganalisis ide mentah di `docs/RAW_INPUT.md`, menyusun user stories dan acceptance criteria format Gherkin di `docs/REQUIREMENT.md`. | **[GERBANG MANUSIA 1]**: Tinjau hasil kurasi kebutuhan. Klik **"Setujui & Lanjutkan"** jika sudah sesuai, atau isi catatan revisi lalu klik **"Tolak"**. |
| **1** | **Architecture & Design** | Merancang diagram C4, ERD database, schema DDL, dan Architectural Decision Record (ADR) di `workspace/architecture/`. | Memantau diagram via dashboard. Sistem otomatis melangkah jika spesifikasi arsitektur lengkap. |
| **2** | **Backlog Generation** | Memecah arsitektur menjadi daftar backlog terukur (*Sprint Backlog*) lengkap dengan estimasi poin di `workspace/backlog/`. | Memeriksa estimasi beban kerja bila diperlukan. |
| **3** | **Task Dispatching** | Mengalokasikan tugas implementasi secara paralel ke worker agent (Backend, Frontend, QA). | Memantau pembagian beban di kartu 5-Team Topology. |
| **4** | **TDD Planning** | Menyusun skenario pengujian unit & integrasi terlebih dahulu (*Test-First Discipline*) dengan target coverage $\ge 70\%$. | Tidak ada tindakan manual (otomatis oleh TDD Orchestrator). |
| **5** | **Development** | Menulis kode logika bisnis, REST controller, dan antarmuka komponen UI. | Mengamati progress commit dan live telemetry terminal. |
| **6** | **Code Review Consortium** | Melakukan audit statis (SAST), cek kebocoran secret, dan verifikasi clean architecture. | **[GERBANG KONDISIONAL - DEADLOCK]**: Jika perbaikan kode gagal 3x berturut-turut, sistem memicu **DEADLOCK**. Anda wajib memilih: Override (*Loloskan*) atau kembalikan ke Dev dengan arahan baru. |
| **7** | **QA E2E Verification** | Menjalankan suite pengujian end-to-end sandbox. | Memverifikasi bug report (harus 0 blocker). |
| **8** | **Pre-Production Gate (DAST/MAST)** | Menjalankan DAST (OWASP ZAP), stress test k6 (P95 $< 200\text{ms}$), dan audit keamanan client mobile (MAST). | **[GERBANG MANUSIA 2 - RELEASE GATE]**: Tinjau laporan verifikasi di `workspace/qa/PRE_PROD_VERIFICATION.md`. Berikan otorisasi rilis produksi. |
| **9** | **Released (Live Production)** | Menyelesaikan siklus rilis dan mengarsipkan audit trail. | Fitur resmi aktif dan siap dikonsumsi pengguna akhir! |

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
* **Spesifikasi & Gherkin**: `docs/REQUIREMENT.md` dan `workspace/specs/`
* **Arsitektur C4 & Schema Database**: `workspace/architecture/C4_ARCHITECTURE_AND_SCHEMA.md`
* **Sprint Backlog**: `workspace/backlog/SPRINT_BACKLOG.md`
* **TDD & Test Suites**: `workspace/testing/TDD_PLAN_AND_SUITE.md`
* **Laporan Pengujian Keamanan & Uji Beban**: `workspace/qa/PRE_PROD_VERIFICATION.md`
* **Source Code Backend**: `orchestrator-service/src/`
* **Source Code Frontend**: `dashboard-ui/src/`
