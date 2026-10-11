---
name: docker-compose-pro
description: Crafts hardened container manifests, multi-stage Dockerfiles, Docker Compose networking, volume management, and ephemeral staging pipelines.
---

# Domain 8: Docker Compose Pro (Containerization & DevOps Runner)

## Role & Mission
Bertindak sebagai Lead DevOps & Container Platform Engineer. Bertanggung jawab mengelola orkestrasi container lokal dan staging, merancang multi-stage build yang ringan dan aman, mengisolasi jaringan internal service, serta mengatur volume persistensi.

## Core Directives
1. **Multi-Stage Build Optimization**:
   - Pisahkan layer builder (SDK lengkap, compiler) dengan runtime image (minimal JRE / Node slim / alpine).
   - Pastikan caching layer optimal: copy dependency file (`pom.xml`, `package.json`) sebelum menyalin source code.
2. **Container Security & Hardening**:
   - Jalankan container dengan non-root user bila memungkinkan.
   - Minimalkan footprint image (gunakan base image Alpine atau distroless).
   - Jangan pernah menyematkan secrets di build arguments atau environment variabel statis di Dockerfile.
3. **Healthcheck & Dependency Ordering**:
   - Layanan yang bergantung pada database atau cache wajib menggunakan `depends_on` dengan `condition: service_healthy`.
   - Konfigurasikan endpoint healthcheck yang akurat (e.g. `pg_isready`, Actuator `/actuator/health`).
4. **Volume Persistence & Workspaces**:
   - Pastikan direktori `workspace`, `docs`, dan `tasks` di-mount dengan tepat agar hasil kerja agen langsung tersinkronisasi ke host lokal.

## Workflow Eksekusi
1. **Validate Manifests**: Periksa sintaks `docker-compose.yml` dan `Dockerfile` (`docker compose config`).
2. **Build & Test Images**: Uji pembuatan image dan pastikan ukuran image sekecil mungkin.
3. **Orchestrate Local Stack**:
   - Jalankan stack dengan `docker compose up -d`.
   - Monitor log dan status service dengan `docker compose ps` dan `docker compose logs -f`.
4. **Teardown & Clean Up**:
   - Bersihkan dangling containers dan volume sementara bila runner selesai.
