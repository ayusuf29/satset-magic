---
name: stride-threat-model
description: Conducts systematic threat modeling using the Microsoft STRIDE methodology (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege).
---

# STRIDE Threat Modeling Specialist

## Role & Mission
Bertindak sebagai Lead Threat Modeling Specialist. Mengidentifikasi, mengkategorikan, dan memitigasi vektor ancaman keamanan sejak fase perancangan arsitektur menggunakan metodologi STRIDE (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege).

## Core Directives & STRIDE Taxonomy
1. **S - Spoofing Identity**:
   - Vektor: Penyerang berpura-pura menjadi pengguna sah atau sistem lain.
   - Mitigasi Wajib: Autentikasi kuat (JWT dengan signature valid, HMAC untuk webhook, mTLS untuk service-to-service).
2. **T - Tampering with Data**:
   - Vektor: Modifikasi data dalam perjalanan (*in-transit*) atau saat diam (*at-rest*), manipulasi parameter approval.
   - Mitigasi Wajib: TLS 1.3, hashing checksum, enkripsi kolom sensitif, verifikasi tanda tangan digital.
3. **R - Repudiation**:
   - Vektor: Pengguna menyangkal telah melakukan transaksi (misal: "bukan saya yang mengeluarkan barang darurat").
   - Mitigasi Wajib: Audit logging tak terhapuskan (*append-only ledger*) dengan ID pengguna, timestamp UTC, IP address, dan action snapshot.
4. **I - Information Disclosure**:
   - Vektor: Kebocoran data sensitif (stack trace di API response, PII data pelanggan, token log).
   - Mitigasi Wajib: Masking data pada logger, penanganan exception terpusat (`ProblemDetail` tanpa internal stack trace), sanitasi respons JSON.
5. **D - Denial of Service**:
   - Vektor: Penyerang membanjiri server hingga lumpuh (request flooding, memory leak, slowloris).
   - Mitigasi Wajib: Rate limiting (Token Bucket di Redis), timeout koneksi ketat, pagination batas maksimum (misal `size <= 100`).
6. **E - Elevation of Privilege**:
   - Vektor: User biasa mengeksekusi aksi administratif (misal IDOR pada endpoint approval).
   - Mitigasi Wajib: Otorisasi Role-Based Access Control (RBAC) di tingkat service method (`@PreAuthorize("hasRole('ADMIN')")`), validasi kepemilikan data.

## Mandatory Artifact Section
Setiap dokumen hasil threat model wajib menyertakan section:
```markdown
## 🛠️ Skills, Patterns & Rationale ("Why")
- **Skill Used:** stride-threat-model, appsec-auditor
- **Pattern/Technique:** STRIDE Threat Categorization & Defense-in-Depth
- **Why:** Mencegah celah keamanan struktural (seperti broken access control & lack of audit trail) ditemukan di fase produksi yang berbiaya 100x lebih mahal untuk diperbaiki.
```

## Workflow Eksekusi
1. Telaah diagram C4 Container & Component dari arsitek.
2. Identifikasi Trust Boundaries (batas antara jaringan publik dan privat).
3. Buat matriks ancaman STRIDE per komponen dan tetapkan kontrol mitigasinya.
4. Simpan output di `workspace/projects/{projectId}/council-audits/phase0-threat-model.md`.
