# DAFTAR SKILL, TOOLS REGISTRY, DAN METODE AUDIT KELAYAKAN SDLC

In Google Antigravity, **Skills** mengikuti format terbuka `SKILL.md`. Dokumen ini bertindak sebagai panduan penalaran LLM (*reasoning*), eksekusi perkakas (*tool execution*), dan batas pengaman (*guardrails*).

---

## 1. Skill Matrix Across the 10 SDLC Domains

| # | Objective / Domain | Core Skills (`SKILL.md`) | Combined / Synergistic Skills | Agent Role or External Service? |
| --- | --- | --- | --- | --- |
| **1** | **Sentence to Requirement to Backlog** | `spec-writer`, `prd-synthesizer`, `user-story-generator` | `acceptance-criteria-generator` (Gherkin), `domain-driven-design` | **Agent** (Orchestrates requirement parsing directly in prompt context) |
| **2** | **Anti-Slop UI / UX** | `ui-ux-pro-max`, `frontend-design`, `web-design-guidelines` | `tailwind-design-system`, `accessibility-wcag`, `design-tokens` | **Agent + Local Tool** (`data/` CSV + `scripts/search.py` BM25 design engine) |
| **3** | **Coding Tasks (Multi-language)** | `spring-boot-enterprise`, `clean-architecture`, `typescript-fullstack`, `python-patterns` | `refactor`, `database-optimizer` (ORM/SQL tuning) | **Agent** (In-file code generation and AST edits) |
| **4** | **Testing & QA** | `tdd-orchestrator`, `test-coverage-analyst`, `jest-vitest-pro`, `spring-test-junit5` | `e2e-playwright`, `mutation-testing-expert` | **Agent** for test writing; **Service** for running headless runs (Playwright runner) |
| **5** | **System Design Architecture** | `system-design-architect`, `c4-model-designer`, `baoyu-diagram` | `distributed-systems-patterns`, `event-driven-messaging` (Kafka/RabbitMQ) | **Agent** (Outputs architecture decision records and diagrams) |
| **6** | **Security Auditing** | `appsec-auditor`, `auth-security-reviewer` | `stride-threat-model`, `secrets-scanner` | **Agent** for AST audit; **Service** (Snyk / Trivy MCP) for CVE scanning |
| **7** | **Code Review** | `code-review`, `senior-reviewer-critique` | `security-audit`, `performance-profiler` | **Agent** (Pre-commit diff analyzer, PR evaluation) |
| **8** | **CI/CD** | `github-actions-architect`, `docker-compose-pro` | `helm-kubernetes-deploy`, `semantic-release` | **Agent** for manifests; **Service** (GitHub Actions / GitLab Runner) for execution |
| **9** | **Penetration Testing / Hacking** | `dast-pentest-scout`, `api-fuzzing-suite` | `owasp-top10-verifier`, `jwt-auth-pentest` | **Service / MCP** (ZAP / Nuclei / Burp daemon); Agent only generates vectors |
| **10** | **Observability** | `otel-instrumentor`, `telemetry-architect` | `prometheus-grafana-metrics`, `structured-logger-pro` | **Agent** for instrumentation code; **Service** (Jaeger / Datadog / Signoz collector) |

---

## 2. Arsitektur Instalasi Skill: Agent vs. Service vs. Local Data

Ada kesalahpahaman umum bahwa "instalasi skill" selalu berupa `npm install` atau biner yang harus di-*compile*. Dalam Antigravity, terdapat 2 tipe skill:

### Tipe A: Cognitive Instruction Skills (Pure Rules)
* **Contoh**: `spec-writer`, `senior-code-reviewer`, `system-design-architect`.
* **Cara Kerja**: Cukup berkas `SKILL.md` dengan frontmatter YAML. Begitu folder diletakkan di `.agents/skills/<nama-skill>/`, Antigravity **secara instan melakukan auto-discovery** dan menyuntikkan instruksinya ke context agent tanpa perlu instalasi tambahan.

### Tipe B: Data & Tool-Backed Skills (Rich Tooling)
* **Contoh**: `ui-ux-pro-max` (dari komunitas GitHub: `nextlevelbuilder/ui-ux-pro-max-skill`).
* **Komponen yang Terpasang di Lokal**:
  1. `SKILL.md` (16 KB panduan komprehensif 10 kategori aturan UI/UX).
  2. `data/`: 17 berkas CSV dataset lokal (79 styles, 192 product palettes, 74 font pairings, 119 UX guidelines, 105 icons).
  3. `scripts/`: Mesin pencari Python BM25 (`search.py` & `core.py`) untuk mencocokkan palette dan gaya secara deterministik tanpa halusinasi LLM.
  4. `references/`: Quick references dan WCAG checklist.

**Lokasi Terpasang di Proyek Ini**:
* `.agents/skills/ui-ux-pro-max/` (Workspace Customizations Root)
* `.agent/skills/ui-ux-pro-max/` (Backward-compatibility Mirror)

---

## 3. Bagaimana Cara Mengetahui Bahwa Setiap Skill Benar-benar Diterapkan di Setiap Fase?

Agar kita tidak hanya "percaya pada janji LLM", platform menerapkan **4 Pilar Pembuktian & Auditibilitas (Deterministic Verification)**:

```
┌────────────────────────────────────────────────────────────────────────┐
│            4 PILAR VERIFIKASI PENERAPAN SKILL SECARA NYATA             │
├────────────────────────────────────────────────────────────────────────┤
│ 1. Proof-of-Artifact Signature (Bukti Fisik File & Format Wajib)       │
│ 2. Deterministic Code Gates (Aturan Keras di Spring Boot Backend)      │
│ 3. Real-Time Telemetry & Execution Log (Audit Trail Command & SSE)     │
│ 4. Council Triad Consensus Voting (Veto Multi-Pilar di Dashboard UI)   │
└────────────────────────────────────────────────────────────────────────┘
```

### Pilar 1: Proof-of-Artifact Signature (Bukti Fisik File Wajib per Fase)
Setiap skill **wajib meninggalkan artefak berformat spesifik** di dalam folder proyek (`workspace/projects/{projectId}/`). Jika berkas tidak ada atau tidak valid, fase tidak dapat disetujui:

| Fase | Skill Terkait | Bukti Fisik / Signature yang Wajib Ada | Cara Verifikasi oleh Manusia / CI |
| :---: | :--- | :--- | :--- |
| **0** | `spec-writer` | `backlog/SPRINT_BACKLOG.md` & `raw_input.md` berformat **Gherkin syntax** (`Given-When-Then`). | Cek apakah acceptance criteria memiliki skenario positif & negatif. |
| **1** | `system-design-architect` | `architecture/c4-model.md` berisi diagram Mermaid C4 Component & schema DDL. | Diagram C4 dapat dirender dan relasi database ternormalisasi. |
| **2** | `ui-ux-pro-max` | `design-system.md` atau `design-system/MASTER.md` yang menggunakan token dari hasil query `scripts/search.py`. | Tidak ada raw hardcoded color hex (`#ff0000`). Variabel token seperti `--grid-gap: 16px`, `--card-radius: 24px` diterapkan. |
| **4** | `tdd-orchestrator` | File tes unit & integrasi (`*Test.java` / `*.test.tsx`) dibuat **sebelum** implementasi bisnis. | Rasio test file vs logic file minimal 1:1. |
| **5** | `spring-boot-enterprise` | Source code backend mengikuti struktur Hexagonal/Clean Architecture (`domain/`, `service/`, `interfaces/`). | Tidak ada controller yang memanggil repository langsung (wajib lewat service interface). |
| **6** | `appsec-auditor` & `senior-code-reviewer` | `council-audits/phase6-review.md` dengan status SAST: **0 Critical OWASP vulnerabilities**. | Audit log memverifikasi sanitasi SQL injection, broken auth, dan CORS. |
| **8** | `dast-pentest-scout` & `docker-compose-pro` | `qa/PRE_PROD_VERIFICATION.md` berisi hasil benchmark k6 & DAST scan staging. | **P95 Latency $< 200\text{ms}$** & error rate $0\%$. |

---

### Pilar 2: Deterministic Programmatic Gates (Enforcement di Backend)
Penerapan skill bukan sekadar teks, melainkan **ditegakkan secara logika di Spring Boot**:
* **Enforcement Coverage ($\ge 70\%$)**:
  Di [`WorkflowOrchestratorServiceImpl.java`](file:///c:/project/satset-magic/orchestrator-service/src/main/java/com/agentic/platform/service/WorkflowOrchestratorServiceImpl.java):
  ```java
  boolean consensusReached = architectApproved && securityApproved && performanceApproved && (coverage >= minTestCoverage);
  if (!consensusReached) {
      // DITOLAK: Kembali ke fase Development atau Eskalasi Deadlock jika 3x gagal!
  }
  ```
* **Enforcement Human Gate**:
  Endpoint `/advance` otomatis melempar `InvalidWorkflowTransitionException` jika dipanggil saat workflow berada di gerbang persetujuan manusia.

---

### Pilar 3: Real-Time Telemetry & Execution Log (Audit Trail)
Setiap kali skill bekerja:
1. **Command Execution**: Terminal log streamer mencatat eksekusi tool nyata, seperti:
   * `mvn test` (menghasilkan JaCoCo XML report).
   * `python scripts/search.py "query" --domain style` (query dataset desain UI/UX).
   * `docker compose up -d` (deployment ephemeral sandbox).
2. **Database Transition Log**: Tabel `WorkflowTransitionLog` menyimpan catatan historis:
   * Kolom `actor`: mencatat agen spesifik (misal: `SYSTEM_ARCHITECT`, `APPSEC_AUDITOR`, `SENIOR_REVIEWER`).
   * Kolom `message`: memuat alasan teknis persetujuan atau poin veto.

---

### Pilar 4: Council Triad Consensus Voting (Transparansi di Dashboard UI)
Di Dashboard Command Hub (`http://localhost:3000`):
* Komponen **Council Triad Matrix** ([`CouncilTriadCard.tsx`](file:///c:/project/satset-magic/dashboard-ui/src/components/CouncilTriadCard.tsx)) secara visual memperlihatkan 3 lampu indikator:
  * 🏛️ **System Architect Vote**: (Status C4 boundary & Clean Code)
  * 🛡️ **AppSec Auditor Vote**: (Status SAST OWASP Top 10)
  * ⚡ **Performance Scout Vote**: (Status SLA Latency & Coverage $\ge 70\%$)
* Jika satu pilar belum menyetujui atau memberikan veto, Anda dapat langsung melihat badge penolakan dan membaca feedback auditnya di layar dashboard sebelum mengambil keputusan.

---

## 4. Checklist Status 17 Skills Lokal Terpasang & Siap Digunakan

Berikut adalah daftar lengkap 17 skills yang **seluruhnya telah terinstal dan terverifikasi di `.agents/skills/` (dan disinkronkan ke `.agent/skills/`)**:

| # | Nama Skill | Domain | Status Instalasi | Tipe & Fitur Lengkap | Lokasi File |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 1 | `spec-writer` | Requirement & BDD | ✅ **Installed** | YAML frontmatter + Gherkin BDD template + Rationale rule | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/spec-writer/SKILL.md) |
| 2 | `ui-ux-pro-max` | Design System | ✅ **Full Suite** | 16 KB `SKILL.md` + 17 CSV Datasets + Python BM25 Search Engine + References | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/ui-ux-pro-max/SKILL.md) |
| 3 | `spring-boot-enterprise` | Backend Java 21 | ✅ **Installed** | Spring Boot 3.3, Virtual Threads, Interface Services, ProblemDetail RFC 7807 | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/spring-boot-enterprise/SKILL.md) |
| 4 | `clean-architecture` | Architecture Patterns | ✅ **Installed** | Ports & Adapters (Hexagonal), Inward Dependency Rule, DTO Records | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/clean-architecture/SKILL.md) |
| 5 | `typescript-fullstack` | Frontend Next.js | ✅ **Installed** | Strict TypeScript, App Router, Discriminated Unions, Safe Touch 44px | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/typescript-fullstack/SKILL.md) |
| 6 | `tdd-orchestrator` | Testing Discipline | ✅ **Installed** | Red-Green-Refactor, Mock Isolation, JaCoCo Coverage $\ge 70\%$ Gate | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/tdd-orchestrator/SKILL.md) |
| 7 | `e2e-playwright` | E2E Browser Testing | ✅ **Installed** | Resilient User Locators, Zero-Sleep Auto-Waiting, Mobile/Desktop Matrix | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/e2e-playwright/SKILL.md) |
| 8 | `system-design-architect` | Enterprise Architecture | ✅ **Installed** | C4 Architecture Modeling, ADR Decisions, Idempotency State Machine | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/system-design-architect/SKILL.md) |
| 9 | `c4-model-designer` | System Diagramming | ✅ **Installed** | C4 Levels 1-4, Mermaid.js Visual Syntax, Bounded Context Decomposition | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/c4-model-designer/SKILL.md) |
| 10 | `appsec-auditor` | Security & SAST | ✅ **Installed** | OWASP Top 10 Prevention, Secret Leak Blocking, Veto Power Authority | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/appsec-auditor/SKILL.md) |
| 11 | `stride-threat-model` | Threat Analysis | ✅ **Installed** | Microsoft STRIDE Taxonomy, Trust Boundaries, Defense-in-Depth | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/stride-threat-model/SKILL.md) |
| 12 | `senior-code-reviewer` | Code Review | ✅ **Installed** | Triad Consortium Consensus, 3-Strike Loop Breaker, Anti-pattern Hunter | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/senior-code-reviewer/SKILL.md) |
| 13 | `docker-compose-pro` | Container & Staging | ✅ **Installed** | Multi-Stage Distroless Build, Non-Root Users, Isolated Bridge Networks | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/docker-compose-pro/SKILL.md) |
| 14 | `github-actions-architect` | CI/CD Pipelines | ✅ **Installed** | Hardened Permissions, Maven/npm Cache Layering, Automated Build Gates | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/github-actions-architect/SKILL.md) |
| 15 | `dast-pentest-scout` | DAST & Stress Testing | ✅ **Installed** | Runtime DAST OWASP ZAP, k6 Stress Test (P95 $<200\text{ms}$), MAST | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/dast-pentest-scout/SKILL.md) |
| 16 | `api-fuzzing-suite` | API Fuzzing | ✅ **Installed** | Mutation Fuzzing, Boundary Overflow, Injection Vectors, Staging Gates | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/api-fuzzing-suite/SKILL.md) |
| 17 | `otel-instrumentor` | Observability & SRE | ✅ **Installed** | W3C TraceContext, Micrometer Prometheus Metrics, Structured JSON Logs | [SKILL.md](file:///c:/project/satset-magic/.agents/skills/otel-instrumentor/SKILL.md) |