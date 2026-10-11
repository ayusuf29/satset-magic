# TASK 00: REQUIREMENT DISCOVERY & HUMAN SIGN-OFF (PHASE 0)

## Objective
Mengekstrak ide mentah dari `docs/RAW_INPUT.md`, mengidentifikasi ambiguitas, merumuskan fitur inti, dan mendapatkan konfirmasi eksplisit dari manusia sebelum arsitektur dirancang.

## Langkah Eksekusi Agent
1. Baca berkas `docs/RAW_INPUT.md`.
2. Analisis requirement:
   - Identifikasi kebutuhan bisnis utama.
   - Buat daftar asumsi dan pertanyaan klarifikasi jika ada bagian yang ambigu.
3. Susun dokumen terstruktur di `docs/REQUIREMENT.md`.
4. Berikan notifikasi ke pengguna untuk melakukan review.
5. **STOP & WAIT**: Dilarang melanjutkan ke `tasks/01_architecture_and_backlog.md` sebelum manusia mengubah status di `docs/REQUIREMENT.md` menjadi `APPROVED_BY_HUMAN`.
