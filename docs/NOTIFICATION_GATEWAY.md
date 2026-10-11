# NOTIFICATION GATEWAY (TELEGRAM, WEBSOCKET, EMAIL)

## 1. Telegram Bot Integration
- Bot mengirimkan pesan ketika workflow memasuki status `WAITING_HUMAN_APPROVAL`.
- Pesan dilengkapi **Inline Keyboard Buttons**:
  - `[✅ APPROVE]` -> Callback data: `approve:<task_id>`
  - `[❌ REJECT]` -> Callback data: `reject:<task_id>`
  - `[📝 VIEW DETAIL]` -> Link langsung ke Next.js Dashboard.

## 2. WebSocket STOMP
- Endpoint: `/ws/agent-events`
- Topic: `/topic/agent-activities` dan `/topic/hitl-alerts`

## 3. JavaMail Emergency Alert
- Mengirimkan email jika terjadi kegagalan build berturut-turut atau eskalasi deadlock review tingkat kritis.
