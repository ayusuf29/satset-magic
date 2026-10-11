# PROJECT EXECUTION MODES

Panduan adaptasi workflow untuk 3 variasi skenario proyek:

## Mode 1: Development from Scratch
- Mulai dari Phase 0 (`docs/RAW_INPUT.md`).
- Architect menentukan stack penuh, database schema awal, dan setup repository baru.
- Eksekusi TDD penuh dari nol.

## Mode 2: System Maintenance (Add Feature / Version Upgrade)
- Langkah Awal: Agen membaca codebase eksisting dan menjalankan test suite yang ada.
- **Syarat Mutlak**: Seluruh existing test harus hijau sebelum kode baru ditulis.
- Menjalankan **Impact Analysis** untuk memetakan service atau file mana yang berisiko regresi.
- Menuliskan regression test tambahan sebelum menambahkan fitur baru.

## Mode 3: Codebase Rewrite / Migration
- Langkah Awal: Ekstraksi kontrak API (OpenAPI spec), event schemas, dan database DDL dari sistem lama.
- Membuat test harness berbasis **Dual-Run / Contract Testing**.
- Developer Agent menulis ulang kode di bahasa/framework baru dengan patokan output contract yang 100% identik.
