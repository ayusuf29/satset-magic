# 5-TEAM TOPOLOGY ARCHITECTURE

Struktur pembagian tim agen:

1. **Product & Design Team**:
   - Product Owner Agent
   - Business Analyst Agent
   - UI/UX Designer Agent
2. **Development Team**:
   - Frontend Worker Agent
   - Backend Worker Agent
   - Code Reviewer Agent
   - QA Automation Agent
3. **CI/CD Team**:
   - DevOps & Release Agent
   - Ephemeral Staging Runner
4. **Production Team**:
   - SRE Agent
   - Observability & Anomaly Monitor Agent
5. **Shared / Enabling Team**:
   - Enterprise Architect Agent
   - Security & Compliance Specialist
   - Database & Performance Specialist

## Protokol Pengujian Multi-Platform (Web & Mobile)

1. **Shared Team (Security & Performance Specialist):**
   - Bertanggung jawab merancang skenario audit otomatis (skrip k6 untuk API, aturan rule MobSF/ZAP untuk keamanan client & server).
   - Menetapkan batas toleransi kegagalan (SLO): batas maksimum latensi API dan ketiadaan kerentanan severity *High/Critical*.

2. **CI/CD Team (DevOps Runner):**
   - Menyediakan runner terisolasi:
     - Ephemeral container staging untuk menjalankan backend service target uji beban.
     - Build runner untuk kompilasi APK/IPA staging sebelum diaudit oleh Security Specialist.

3. **Development Team (Frontend, Backend, Mobile Worker):**
   - Wajib memperbaiki temuan keamanan atau kegagalan uji beban yang diterbitkan ke dalam `workspace/BUG_REPORT.md` sebelum branch diizinkan masuk ke gerbang approval rilis produksi.
