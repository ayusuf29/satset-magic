# JAVA AGENTIC ENGINE ARCHITECTURE

Spesifikasi runtime orkestrator yang dibangun menggunakan Java Spring Boot, Spring AI, dan LangChain4j.

## 1. Komponen Utama
1. **LangChain4j AI Services**:
   - Menangani declarative agent prompts, memory windowing, dan conversational chain.
   - Mengelola state looping dan evaluasi kondisi bersarang.
2. **Spring AI MCP Client**:
   - Menghubungkan engine Java ke berbagai MCP servers (Filesystem, GitHub, Database, Browser).
   - Menstandarkan execution tool calls ke dalam ekosistem Spring Framework.
3. **State Machine & Execution Graph**:
   - Stateful workflow engine yang mencatat checkpoint setiap transisi status task ke database PostgreSQL.
   - Menyediakan fungsi `pauseForHumanReview()` dan `resumeAfterApproval()`.
