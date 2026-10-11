# SETUP AND PROPERTIES GUIDE

Panduan konfigurasi awal sebelum menjalankan autonomous agent workflow di Google Antigravity.

## 1. Environment Variables (.env)
Buat file `.env` di root folder dengan variabel berikut:

```env
# LLM Providers
GEMINI_API_KEY=your_google_ai_studio_api_key
OPENAI_API_KEY=optional_openai_key_if_used
ANTHROPIC_API_KEY=optional_anthropic_key_if_used

# Telegram Bot (Human-in-the-Loop Alerts)
TELEGRAM_BOT_TOKEN=your_telegram_bot_token
TELEGRAM_CHAT_ID=your_telegram_chat_id_or_group_id

# Mail Alert (Optional)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Database Checkpointer & Cache
DATABASE_URL=jdbc:postgresql://localhost:5432/agentic_db
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379

# Target Workspace
TARGET_WORKSPACE_PATH=./workspace
```

## 2. Konfigurasi MCP (Model Context Protocol) di Antigravity
Antigravity akan membaca konfigurasi MCP dari file `.antigravity/mcp-config.json`.
Server MCP yang wajib aktif:
- `filesystem`: Akses baca/tulis ke direktori `workspace/`, `docs/`, dan `tasks/`.
- `github`: Manajemen git branch, commits, diffs, dan pull requests.
- `postgres`: Audit dan migrasi skema database relasional.
- `playwright`: Menjalankan pengujian E2E headless untuk QA agent.

## 3. Menjalankan Antigravity Agent
1. Buka Antigravity dan pilih model **Gemini 2.5/3.0 Flash** (atau Gemini 2.0 Pro untuk task architecture reasoning).
2. Set instruksi sistem awal merujuk pada `docs/system-grounding-rules.md` dan `agents.md`.
3. Mulai siklus eksekusi dengan memerintahkan agent membaca `tasks/00_requirement_discovery.md`.
