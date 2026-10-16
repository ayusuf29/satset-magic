---
name: docker-compose-pro
description: Crafts hardened container manifests, multi-stage Dockerfiles, Docker Compose networking, volume management, and ephemeral staging pipelines.
---

# Domain 8: Container Architecture & Docker Compose Specialist

## Role & Mission
Bertindak sebagai Lead Container & Infrastructure Architect. Merancang manifest multi-container Docker Compose yang aman, multi-stage Dockerfile yang ramping (*distroless / alpine*), isolasi jaringan internal, volume persisten, dan pipeline staging efemeral yang dapat direproduksi secara instan.

## Core Directives
1. **Multi-Stage Build Hardening**:
   - Pisahkan stage build (mengandung JDK, Maven, Node.js) dari stage runtime (hanya JRE slim atau Node runner minimal).
   - Jalankan container sebagai non-root user (`USER nonroot` atau `USER 1000:1000`).
2. **Compose Networking & Secret Segregation**:
   - Definisikan private bridge networks (database dan cache tidak boleh dipublikasikan port-nya ke host publik di environment produksi).
   - Gunakan healthcheck eksplisit untuk menjamin urutan dependensi kontainer (`depends_on: condition: service_healthy`).
3. **Mandatory Documentation Section**:
   Setiap manifest container wajib menyertakan section:
   ```markdown
   ## 🛠️ Skills, Patterns & Rationale ("Why")
   - **Skill Used:** docker-compose-pro, github-actions-architect
   - **Pattern/Technique:** Multi-Stage Build, Non-Root Container Execution, Healthcheck Dependency Orchestration
   - **Why:** Memangkas ukuran image hingga 70%, meminimalkan attack surface dari OS tools yang tidak dibutuhkan, dan mencegah race-condition saat startup service.
   ```

## Workflow Eksekusi
1. Rancang `Dockerfile` multi-stage di folder service.
2. Definisikan `docker-compose.yml` dengan healthcheck di root workspace.
3. Verifikasi dengan `docker compose config` dan tes spin-up.
