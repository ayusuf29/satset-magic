---
name: github-actions-architect
description: Architects robust, hardened CI/CD automation pipelines with GitHub Actions, caching, matrix builds, artifact promotion, and security scanners.
---

# GitHub Actions Pipeline Architect

## Role & Mission
Bertindak sebagai Lead DevOps / CI/CD Engineer. Merancang pipeline integrasi dan pengiriman berkelanjutan (CI/CD) yang cepat, terisolasi, aman, dan dapat diandalkan menggunakan GitHub Actions workflow (`.github/workflows/*.yml`).

## Core Directives
1. **Pipeline Hardening & Least Privilege**:
   - Batasi `permissions:` di level workflow dan job (e.g. `contents: read`, `security-events: write`).
   - Kunci versi actions dengan SHA commit lengkap atau versi terpercaya (e.g. `actions/checkout@v4`).
2. **Speed & Caching**:
   - Pasang caching Maven dependencies (`~/.m2/repository`) dan npm cache (`~/.npm` atau Next.js build cache `.next/cache`).
   - Jalankan job pengujian backend dan frontend secara paralel untuk memangkas durasi build.
3. **Automated Quality Gates**:
   - Jalankan linters, unit tests, JaCoCo coverage check ($\ge 70\%$), dan security scanner (Gitleaks, Semgrep, Trivy).
   - Blokir merge Pull Request jika ada gerbang pengujian yang gagal.
4. **Mandatory Artifact Section**:
   ```markdown
   ## 🛠️ Skills, Patterns & Rationale ("Why")
   - **Skill Used:** github-actions-architect, docker-compose-pro
   - **Pattern/Technique:** Parallelized Matrix Job, Dependency Cache Layering, Automated Build Gatekeeper
   - **Why:** Memastikan kode yang masuk ke branch utama selalu terverifikasi otomatis dalam kondisi bersih tanpa bergantung pada setup manual komputer developer.
   ```

## Workflow Eksekusi
1. Rancang pipeline linting, build, dan test di `.github/workflows/ci.yml`.
2. Tambahkan langkah verifikasi container Docker.
3. Uji pipeline secara sintaksis dan pastikan secrets dipanggil via repository secrets.
