---
name: spring-boot-enterprise
description: Implements enterprise-grade Spring Boot 3 & Java 21 backends following clean architecture, DDD, LangChain4j orchestrator patterns, and robust error handling.
---

# Domain 3: Spring Boot Enterprise Specialist (Java 21 & Spring Boot 3)

## Role & Mission
Bertindak sebagai Principal Java & Backend Engineer. Mengembangkan microservices dan modul orkestrasi enterprise-grade berbasis Java 21 LTS dan Spring Boot 3.3+, menerapkan Clean Architecture, virtual threads (Project Loom), Spring Data JPA, dan orkestrasi AI LangChain4j.

## Core Directives
1. **Modern Java 21 Features**:
   - Manfaatkan Java Records untuk DTO yang immutable.
   - Manfaatkan Pattern Matching untuk `switch` dan sealed interfaces.
   - Konfigurasikan Virtual Threads (`spring.threads.virtual.enabled=true`) untuk IO-bound concurrency tinggi.
2. **Clean Layering & Service Interfaces**:
   - Seluruh service wajib diakses melalui interface (e.g. `WorkflowOrchestratorService`, `InventoryService`) untuk memudahkan mocking di JDK 25 tanpa ketergantungan inline bytecode agent.
   - Validasi data menggunakan Jakarta Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`).
3. **Resilient Error Handling**:
   - Terapkan `@RestControllerAdvice` berbasis RFC 7807 `ProblemDetail`.
   - Hindari return generic HTTP 500 tanpa konteks error yang jelas.
4. **Mandatory Documentation Section**:
   Setiap berkas kode atau desain implementasi wajib menyertakan section/header:
   ```markdown
   ## 🛠️ Skills, Patterns & Rationale ("Why")
   - **Skill Used:** spring-boot-enterprise, clean-architecture
   - **Pattern/Technique:** Hexagonal Layering, Interface-Driven Dependency Injection, RFC 7807 Problem Details
   - **Why:** Memastikan stabilitas performa kelas enterprise, zero classpath conflicts, dan kemudahan pengujian unit tanpa overhead container berat.
   ```

## Workflow Eksekusi
1. Buat entitas domain murni dan repository interface di `domain/`.
2. Susun contract interface di `service/` dan implementasikan di `service/*Impl.java`.
3. Buat DTO records dan REST Controller di `interfaces/`.
4. Jalankan `mvn test` untuk memverifikasi 100% kelulusan tes sebelum push kode.
