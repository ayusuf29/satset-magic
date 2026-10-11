# JAVA AGENT GROUNDING & CODING STANDARDS

Aturan khusus untuk pengembangan dan modifikasi di dalam folder `orchestrator-service/`:

1. Gunakan **Java 21 LTS** dengan fitur modern (Records, Pattern Matching, Virtual Threads).
2. Gunakan **Spring Boot 3.3+**.
3. Gunakan **Spring AI 1.0.0-M* / LangChain4j 0.35+**.
4. Logging terstruktur menggunakan SLF4J + Logback dengan correlation ID / trace ID.
5. Gunakan JUnit 5, Mockito, dan Testcontainers untuk automated verification.
