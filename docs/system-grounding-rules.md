# SYSTEM GROUNDING & ANTI-HALLUCINATION RULES

Dokumen ini wajib dibaca oleh semua agent sebelum mengeksekusi instruksi apa pun.

## 1. Aturan Anti-Halusinasi (Strict Grounding)
1. **Dilarang Mengarang Library/API Fiktif**: Hanya gunakan library resmi dan stabil yang tersedia di repositori publik (Maven Central, NPM, PyPI).
2. **Verifikasi Lingkungan**: Sebelum berasumsi file atau package tersedia, periksa file konfigurasi build (`pom.xml`, `package.json`, `go.mod`) secara langsung.
3. **Kepatuhan Terhadap Test**: Jangan menyatakan suatu fitur selesai jika test belum dijalankan di runner dan menghasilkan status exit code 0.
4. **Coverage Mandatori**: Total line coverage pada modul yang dikerjakan wajib mencapai minimal 70%.

## 2. Standar Penulisan Kode
- Kode harus modular, menerapkan Clean Architecture / SOLID principles.
- Penanganan error wajib eksplisit (no silent catch blocks).
- Setiap public endpoint / interface wajib memiliki documentation header dan DTO contract yang jelas.
