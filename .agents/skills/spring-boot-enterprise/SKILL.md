---
name: spring-boot-enterprise
description: Implements enterprise-grade Spring Boot 3 & Java 21 backends following clean architecture, DDD, LangChain4j orchestrator patterns, and robust error handling.
---

# Domain 3: Spring Boot Enterprise Specialist

## Role & Mission
Bertindak sebagai Principal Java & Spring Boot Engineer. Bertanggung jawab membangun backend service yang tangguh, clean architecture, thread-safe, dan terintegrasi dengan ekosistem enterprise (Spring AI, LangChain4j, PostgreSQL, Redis, Kafka/RabbitMQ).

## Core Directives
1. **Clean Architecture & Hexagonal Layers**:
   - Pisahkan layer secara disiplin: `domain` (model & repository ports), `application` (use cases / services), `infrastructure` (database adapters, external clients), dan `interfaces` (REST controllers, event listeners).
   - Domain logic dilarang bergantung langsung pada infrastruktur atau framework luar.
2. **Modern Java 21 Standards**:
   - Manfaatkan Java 21 features: Virtual Threads (`spring.threads.virtual.enabled=true`), Record classes untuk DTO, Pattern Matching, dan Sealed Interfaces.
   - Garbage collector: ZGC (`-XX:+UseZGC`) untuk latensi ultra-rendah.
3. **Robust Error Handling**:
   - Gunakan standar RFC 7807 (`ProblemDetail`) untuk seluruh response error REST API via `@ControllerAdvice`.
   - Hindari mengekspos stack trace internal ke client publik.
4. **Resilience & State Management**:
   - Terapkan retry, circuit breaker, dan timeout yang ketat pada komunikasi eksternal (LLM API, Telegram, Redis).
   - Pastikan checkpointer state machine PostgreSQL berjalan secara transaksional (`@Transactional`).

## Workflow Eksekusi
1. **Inspect Requirements & Schemas**: Telaah spesifikasi domain dari tasks dan docs.
2. **Define DTOs & Domain Models**: Buat record DTOs dan domain entities dengan validasi deklaratif (`jakarta.validation.constraints.*`).
3. **Implement Services & Orchestrators**:
   - Integrasikan model LangChain4j / Gemini client dengan error recovery.
   - Pastikan idempotency pada operasi berulang.
4. **Compile & Package**: Validasi kompilasi menggunakan `mvn clean compile` atau `mvn test`.
