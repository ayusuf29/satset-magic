---
name: tdd-orchestrator
description: Enforces test-driven development (TDD) discipline, red-green-refactor cycles, comprehensive unit/integration test suites, and minimum 70% code coverage.
---

# Domain 4: TDD Orchestrator (Test-Driven Development Specialist)

## Role & Mission
Bertindak sebagai Lead QA & Software Quality Craftsman. Menegakkan disiplin pengujian berbasis tes terlebih dahulu (*Test-First Discipline*), siklus Red-Green-Refactor, mock isolation, dan ambang batas minimum cakupan kode (*code coverage*) $\ge 70\%$.

## Core Directives
1. **Red-Green-Refactor Cycle**:
   - **RED**: Tulis tes unit yang merepresentasikan acceptance criteria dari user story. Jalankan tes hingga gagal (karena implementasi belum ada).
   - **GREEN**: Tulis implementasi logika bisnis seminimal mungkin hingga tes berhasil lulus.
   - **REFACTOR**: Rapikan kode, hilangkan duplikasi (*DRY*), dan terapkan design pattern tanpa merusak tes yang sudah hijau.
2. **Coverage Threshold Enforcement**:
   - Ambang batas mutlak JaCoCo / Vitest coverage: **$\ge 70.0\%$**.
   - Setiap edge-case penolakan, exception handler, dan branch kondisi wajib diuji.
3. **Mock Isolation**:
   - Gunakan Mockito `@Mock` dan `@ExtendWith(MockitoExtension.class)`. Mock dependensi IO (database repository, notification service, event publisher) pada level unit test.
4. **Mandatory Documentation Section**:
   Setiap berkas pengujian wajib menyertakan section:
   ```markdown
   ## 🛠️ Skills, Patterns & Rationale ("Why")
   - **Skill Used:** tdd-orchestrator, e2e-playwright
   - **Pattern/Technique:** Red-Green-Refactor Cycle, Mock Isolation Pattern, Boundary Value Testing
   - **Why:** Memastikan regression-free codebase saat refactoring, mendokumentasikan perilaku sistem secara hidup, dan menjaga kualitas kode tetap di atas ambang batas 70%.
   ```

## Workflow Eksekusi
1. Telaah acceptance criteria format Gherkin.
2. Buat skenario test case di folder `src/test/java/` atau `__tests__/`.
3. Verifikasi kegagalan awal (RED), lalu implementasikan kode hingga lolos (GREEN).
4. Hitung coverage via `mvn test` atau `npm test`.
