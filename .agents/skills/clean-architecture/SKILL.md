---
name: clean-architecture
description: Enforces Hexagonal / Clean Architecture boundaries, Ports & Adapters, Domain-Driven Design (DDD), and strict dependency inversion.
---

# Clean Architecture & DDD Specialist

## Role & Mission
Bertindak sebagai Lead Software Craftsman. Menegakkan disiplin Clean Architecture (Ports & Adapters / Hexagonal Architecture) dan Domain-Driven Design (DDD) untuk memastikan logika bisnis inti independen dari framework, database, UI, atau pustaka eksternal.

## Core Directives
1. **The Dependency Rule**:
   - Dependensi kode hanya boleh mengarah ke dalam (*inward*).
   - Domain Model dan Entities berada di pusat terdalam dan **tidak boleh bergantung pada library eksternal** (zero framework annotations di core entities bila memungkinkan, atau minimal isolasi dari Spring/JPA).
2. **Layering Boundaries**:
   - `domain/model/`: Entitas bisnis murni, value objects, domain exceptions.
   - `domain/repository/`: Antarmuka (*interfaces/ports*) tanpa implementasi database.
   - `service/` / `usecase/`: Orchestrator logika bisnis yang mengimplementasikan use case.
   - `interfaces/rest/` / `interfaces/dto/`: Adapter masukan (*inbound adapter*) yang menangani HTTP dan deserialisasi DTO.
   - `infrastructure/`: Adapter keluaran (*outbound adapter*) seperti implementasi database, HTTP client luar, atau messaging broker.
3. **Anti-Patterns yang Dilarang Keras**:
   - Controller memanggil Repository secara langsung tanpa melewati Use Case / Service.
   - Database Entity bocor langsung ke REST response JSON (wajib melalui DTO record).
   - Business logic diletakkan di dalam Controller atau View component.
4. **Mandatory Artifact Section**:
   ```markdown
   ## 🛠️ Skills, Patterns & Rationale ("Why")
   - **Skill Used:** clean-architecture, spring-boot-enterprise
   - **Pattern/Technique:** Ports and Adapters (Hexagonal Architecture), Repository Pattern, DTO Pattern
   - **Why:** Memudahkan unit testing dengan mocking interface, memungkinkan pergantian database/framework di masa depan tanpa mengubah satu baris pun logika bisnis.
   ```

## Workflow Eksekusi
1. Petakan boundary use case sebelum membuat file.
2. Definisikan entity dan interface repository terlebih dahulu.
3. Tulis service use-case berorientasi interface.
4. Hubungkan controller via DTO immutable (`record` di Java / `interface` di TypeScript).
