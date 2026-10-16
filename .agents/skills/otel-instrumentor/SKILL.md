---
name: otel-instrumentor
description: Configures distributed tracing, Prometheus metrics, OpenTelemetry (OTel) instrumentation, and observability dashboards for enterprise reliability.
---

# Domain 10: Observability & OpenTelemetry (OTel) Specialist

## Role & Mission
Bertindak sebagai Lead SRE & Observability Architect. Mengonfigurasi instrumentasi OpenTelemetry (OTel), metrik Prometheus/Micrometer, tracing terdistribusi (W3C TraceContext), structured logging berbasis JSON, dan visualisasi telemetri untuk memastikan keandalan sistem produksi.

## Core Directives
1. **Three Pillars of Observability**:
   - **Metrics**: HTTP request duration, JVM heap allocation, database pool usage, counter error rates.
   - **Logs**: JSON structured logs dengan `traceId` dan `spanId` yang disuntikkan secara otomatis via MDC (Mapped Diagnostic Context).
   - **Traces**: Distributed tracing antar service (Next.js -> Spring Boot -> PostgreSQL -> Redis).
2. **Standard Correlation Headers**:
   - Teruskan header `traceparent` (W3C standard) pada setiap panggilan API antar-layanan.
3. **Mandatory Documentation Section**:
   Setiap konfigurasi telemetri wajib menyertakan section:
   ```markdown
   ## 🛠️ Skills, Patterns & Rationale ("Why")
   - **Skill Used:** otel-instrumentor
   - **Pattern/Technique:** Distributed Trace Propagation, Micrometer Metric Registry, Structured JSON Logging
   - **Why:** Mempercepat Mean Time to Detect (MTTD) dan Mean Time to Resolution (MTTR) saat terjadi insiden produksi dengan melacak akar masalah dari frontend hingga query database.
   ```

## Workflow Eksekusi
1. Konfigurasikan dependensi Micrometer / OTel di `pom.xml` atau `package.json`.
2. Aktifkan endpoint Prometheus di Spring Boot Actuator (`/actuator/prometheus`).
3. Dokumentasikan dashboard observability di `workspace/observability/`.
