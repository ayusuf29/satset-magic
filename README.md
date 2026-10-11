# Satset Orchestrator — Enterprise Multi-Agent Software Engineering Platform

Platform orkestrasi Autonomous Multi-Agent Software Engineering berstandar enterprise berbasis **Java 21 Spring Boot 3.3 (LangChain4j + Spring AI)** dengan antarmuka **Next.js 14 Mobile Command Dashboard** dan ekosistem task terstruktur untuk Google Antigravity.

---

## 📖 Panduan Lengkap Pengguna (Wajib Dibaca)
Panduan operasional interaksi manusia (*Human-in-the-Loop*) dari awal hingga rilis produksi tersedia di:
👉 **[USER_GUIDE_OPERATIONAL_MANUAL.md](file:///c:/project/satset-magic/USER_GUIDE_OPERATIONAL_MANUAL.md)**

---

## 🚀 Cara Menjalankan Cepat

### 1. Menggunakan Docker Compose (Stack Lengkap)
```bash
docker compose up -d --build
```
Stack container:
- **Dashboard UI**: `http://localhost:3000` (Next.js Mobile-First)
- **Orchestrator Backend**: `http://localhost:8080` (Spring Boot API & SSE Stream)
- **PostgreSQL Database**: `localhost:5432` (State checkpointer & audit log)
- **Redis Cache**: `localhost:6379` (Real-time event broker)

### 2. Menjalankan Lokal (Development Mode)
- **Backend**:
  ```bash
  cd orchestrator-service
  java -jar target/orchestrator-service-1.0.0-SNAPSHOT.jar
  ```
- **Frontend Dashboard**:
  ```bash
  cd dashboard-ui
  npm run start
  ```

---

## 📂 Struktur Direktori Proyek

```text
satset-magic/
├── .agent/skills/                         # 10 Antigravity SDLC Domain Skills
│   ├── spec-writer/SKILL.md               # Domain 1: PRD & Gherkin user stories
│   ├── ui-ux-pro-max/SKILL.md             # Domain 2: Anti-slop UI tokens
│   ├── spring-boot-enterprise/SKILL.md    # Domain 3: Clean architecture Java
│   ├── tdd-orchestrator/SKILL.md          # Domain 4: Test-first & >=70% coverage
│   ├── system-design-architect/SKILL.md   # Domain 5: C4 model & ADR
│   ├── appsec-auditor/SKILL.md            # Domain 6: SAST & secret audit
│   ├── senior-code-reviewer/SKILL.md      # Domain 7: Review lead & 3x retry deadlock
│   ├── docker-compose-pro/SKILL.md        # Domain 8: Container manifest
│   ├── dast-pentest-scout/SKILL.md        # Domain 9: DAST (ZAP) & k6 load test
│   └── otel-instrumentor/SKILL.md         # Domain 10: OTel metrics & tracing
│
├── .antigravity/                          # MCP server configuration
│   ├── mcp-config.json                    # Filesystem, GitHub, Postgres, Playwright
│   └── agent-runtime-rules.json
│
├── docker-compose.yml                     # Multi-service container runner
├── USER_GUIDE_OPERATIONAL_MANUAL.md       # Panduan operasional langkah demi langkah
├── docs/                                  # Knowledge base & panduan teknis
├── tasks/                                 # Sequential SDLC pipeline tasks
├── orchestrator-service/                  # Java 21 Spring Boot Backend
├── dashboard-ui/                          # Next.js 14 Mobile-First Web Dashboard
└── workspace/                             # Target output sandbox hasil kerja agen
```
