# SPRING BOOT & LANGCHAIN4J ORCHESTRATOR DESIGN

## 1. State Machine Definition
Workflow memiliki state enum:
- `PHASE_0_REQUIREMENT_DISCOVERY`
- `PHASE_0_WAITING_HUMAN_SIGN_OFF`
- `PHASE_1_ARCHITECTURE_AND_DESIGN`
- `PHASE_2_BACKLOG_GENERATION`
- `PHASE_3_TASK_DISPATCHING`
- `PHASE_4_TDD_PLANNING`
- `PHASE_5_DEVELOPMENT_IMPLEMENTATION`
- `PHASE_6_CODE_REVIEW` (Loop counter: 0..3)
- `PHASE_6_DEADLOCK_WAITING_HUMAN`
- `PHASE_7_QA_VERIFICATION`
- `PHASE_8_READY_FOR_PRODUCTION_GATE`
- `PHASE_9_RELEASED`

## 2. SSE Controller
Orchestrator menyediakan endpoint `/api/workflow/stream` berbasis Server-Sent Events (SSE) agar antarmuka Next.js dapat menampilkan live logs and state transitions secara real-time.
