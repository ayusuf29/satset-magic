# 5-TEAM TOPOLOGY & CROSS-CUTTING COUNCIL ARCHITECTURE

## 1. Struktur Pembagian Tim
1. **Product & Design Team**:
   - Product Owner Agent (PRD Authoring)
   - Business Analyst Agent (Gherkin Scenarios)
   - UI/UX Designer Agent (Anti-Slop Design Tokens & Mobile First)
2. **Development Team**:
   - Frontend Worker Agent (Next.js / React)
   - Backend Worker Agent (Java 21 / Spring Boot)
   - Code Reviewer Agent (Clean Code & Refactoring)
   - QA Automation Agent (Playwright / E2E)
3. **CI/CD Team**:
   - DevOps & Release Agent (Docker Multi-stage)
   - Ephemeral Staging Runner
4. **Production Team**:
   - SRE Agent
   - Observability & Anomaly Monitor Agent (OTel / Micrometer)
5. **Shared / Enabling Team (The Triad Council)**:
   - **Enterprise Architect**: Memastikan modularitas, bounded context, DDD, dan C4 diagrams.
   - **Security & Compliance Specialist**: Memastikan Threat Modeling (STRIDE), SAST, secret leakage zero-tolerance, dan DAST/MAST.
   - **Database & Performance Specialist**: Menetapkan Latency Budget, EXPLAIN ANALYZE, query indexing, dan connection pool sizing.

---

## 2. Protokol Keterlibatan Deep Council di Setiap Fase (Council Triad Matrix)

Bahkan untuk kebutuhan aplikasi yang paling sederhana sekalipun, Council Wajib menyematkan audit di setiap tahap:

| Fase SDLC | Peran Architect (🏛️) | Peran Security (🛡️) | Peran Performance (⚡) |
| :--- | :--- | :--- | :--- |
| **Phase 0: Discovery** | Identifikasi batasan domain bisnis & integrasi | Identifikasi klasifikasi data (PII/Sensitif) | Estimasi proyeksi beban transaksi (RPS) |
| **Phase 1: Architecture** | Merancang C4 Container & Schema ERD | Menyusun Threat Model STRIDE & Auth flow | Menetapkan Latency Budget (P95 $< 200\text{ms}$) |
| **Phase 2: Backlog** | Memetakan dependency graph antar task | Menambahkan Security Acceptance Criteria | Menambahkan SLO/SLA benchmark criteria |
| **Phase 4: TDD** | Meninjau test isolation & hexagonal ports | Merancang Abuse-case & Negative Test suite | Merancang Concurrency & Race-condition tests |
| **Phase 5: Dev** | Menjaga struktur Clean Architecture | Mencegah hardcoded secrets & unvalidated input | Menghindari query N+1 & memory leak |
| **Phase 6: Review** | **Council Vote 1**: Kepatuhan arsitektur | **Council Vote 2**: SAST & OWASP compliance | **Council Vote 3**: Resource footprint & latency |
| **Phase 8.5: Gate** | Audit kesiapan environment rilis | Eksekusi DAST (OWASP ZAP) & MAST audit | Eksekusi Stress Testing (Grafana k6) |

---

## 3. Aturan Konsorsium Review (Phase 6 Triad Voting)
- Status kelulusan Phase 6 wajib memperoleh suara bulat dari ketiga anggota Council:
  $$\text{Consortium Verdict} = \text{Architect} \land \text{Security} \land \text{Performance} = \text{TRUE}$$
- Jika terdapat salah satu suara `REJECT`, loop review bertambah (+1).
- Jika loop mencapai 3x perulangan, pemicu **DEADLOCK** aktif dan sistem berhenti menunggu eskalasi keputusan manusia.
