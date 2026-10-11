# TASK 04: CODE REVIEW, QA & RELEASE GATE (PHASE 8 - 11)

## Objective
Melakukan audit konsorsium, perbaikan berulang (maksimal 3x), pengujian QA, verifikasi keamanan dinamis & ketahanan beban (Web & Mobile), serta konfirmasi akhir sebelum rilis ke production.

## Langkah Eksekusi Agent

### 1. Code Reviewer Consortium (Phase 8 & 9)
- Lakukan static analysis, security code audit (SAST), dan code quality check.
- Jika ada temuan, berikan feedback perbaikan ke Developer Agent.
- **Loop Limit**: Maksimal perulangan perbaikan adalah 3 kali.
- Jika setelah 3 kali masih belum disetujui, pemicu **DEADLOCK** aktif: minta keputusan manual dari manusia via dashboard / Telegram.

---

### 2. Pre-Production Verification Gate (Phase 8.5 - Staging Environment)
Setelah Code Review disetujui, deploy build ke Ephemeral Staging Sandbox untuk pengujian dinamis:

#### Jalur A: Web & Backend API
1. **Dynamic Security Assessment (DAST) - Security Specialist Agent:**
   - Jalankan pemindaian otomatis terhadap endpoint API (OWASP ZAP baseline scan).
   - Validasi konfigurasi keamanan transport: CORS policy, Content Security Policy (CSP), proteksi CSRF, dan sanitasi header HTTP.
   - Uji penanganan token sesi/JWT terhadap skenario kadaluwarsa dan signature invalid.
2. **Stress & Load Testing - Performance Specialist Agent:**
   - Jalankan uji beban terukur (skrip k6/JMeter) untuk memverifikasi kapasitas rate limiting dan auto-scaling.
   - Evaluasi metrik: P95 response time < 200ms pada beban puncak, 0% memory leak pada connection pool database.

#### Jalur B: Mobile Client (Android / iOS / Flutter)
1. **Mobile Application Security Assessment (MAST) - Security Specialist Agent:**
   - **Secret & Credential Audit:** Pemindaian statis pada bundle/APK/IPA untuk memastikan tidak ada API keys privat, hardcoded private keys, atau endpoint rahasia yang terekspos.
   - **Local Storage Security:** Verifikasi implementasi penyimpanan aman (wajib Android Keystore / iOS Keychain, melarang token disimpan mentah di SharedPreferences / LocalStorage).
   - **Network Security Configuration:** Verifikasi konfigurasi TLS minimum v1.3 dan validasi kesiapan Certificate Pinning.
2. **Client Performance & Footprint - QA Automation Agent:**
   - Validasi ukuran build binary, izin runtime (Android/iOS permissions) yang minimal sesuai prinsip *least privilege*, serta waktu startup awal (*cold start time*).

---

### 3. QA Automation & Functional E2E (Phase 10)
- Jalankan skenario E2E di sandbox:
  - **Web:** Playwright Headless testing pada alur pengguna kritis (checkout, login, form submission).
  - **Mobile:** Smoke testing alur UI via framework testing client.
- Jika ditemukan kegagalan/cacat:
  - Terbitkan `workspace/BUG_REPORT.md` lengkap dengan severity (Minor, Major, Critical).
  - Jika severity **Critical/Blocker**, kembalikan status ke Developer Agent.

---

### 4. Human Production Gate (Phase 11)
- Jika seluruh verifikasi Pre-Production (DAST, Load Test, MAST) dan QA berstatus **GREEN** (0 blocker):
  - Ubah status workflow menjadi `READY_FOR_PRODUCTION`.
  - Kirim notifikasi konfirmasi akhir ke human decision maker via Telegram & Dashboard UI.
  - Deployment ke production hanya dipicu setelah persetujuan manual diberikan.