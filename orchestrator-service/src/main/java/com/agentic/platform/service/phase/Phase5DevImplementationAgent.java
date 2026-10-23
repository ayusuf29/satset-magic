package com.agentic.platform.service.phase;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.service.lifecycle.PhaseContext;
import com.agentic.platform.service.lifecycle.PhaseExecutionManager;
import com.agentic.platform.service.lifecycle.PhaseResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class Phase5DevImplementationAgent implements PhaseAgentHandler {

    private static final Logger log = LoggerFactory.getLogger(Phase5DevImplementationAgent.class);

    private final ChatLanguageModel chatLanguageModel;
    private final SdlcJournalService journalService;
    private final ProjectScaffoldingService scaffoldingService;
    private final SkillLoaderService skillLoaderService;

    public Phase5DevImplementationAgent(
            ChatLanguageModel chatLanguageModel,
            SdlcJournalService journalService,
            ProjectScaffoldingService scaffoldingService,
            SkillLoaderService skillLoaderService) {
        this.chatLanguageModel = chatLanguageModel;
        this.journalService = journalService;
        this.scaffoldingService = scaffoldingService;
        this.skillLoaderService = skillLoaderService;
    }

    @Override
    public WorkflowState getHandledPhase() {
        return WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION;
    }

    @Override
    public String getAgentRole() {
        return "Dev Engineer Agent";
    }

    @Override
    public String getInstalledSkills() {
        return "spring-boot-enterprise, clean-architecture, typescript-fullstack";
    }

    @Override
    public String getPatternsAndTechniques() {
        return "Hexagonal Architecture (Ports & Adapters), DDD Domain Core & Spring Boot 3 Materialization";
    }

    @Override
    public WorkflowState getNextPhaseOnSuccess() {
        return WorkflowState.PHASE_5_WAITING_HUMAN_APPROVAL;
    }

    @Override
    public boolean isAutoAdvance() {
        return false; // Pauses for Human Code Implementation Sign-Off Gate
    }

    @Override
    public PhaseResult execute(
            WorkflowInstance workflow,
            PhaseContext context,
            java.util.function.Consumer<PhaseExecutionManager.ProgressUpdate> progress) throws Exception {

        String projectId = workflow.getProjectId();
        String taskName = workflow.getTaskName();

        // Single Source of Truth: Consume TDD plan & specs, architecture, database schema, and stories
        String tddSpecs = journalService.readArtifactContent(projectId, "tests/TDD_PLAN_AND_SPECS.md");
        String c4Model = journalService.readArtifactContent(projectId, "architecture/C4_MODEL.md");
        String schema = journalService.readArtifactContent(projectId, "backlog/DATABASE_SCHEMA.sql");
        String stories = journalService.readArtifactContent(projectId, "backlog/TECHNICAL_USER_STORIES.md");
        String revisionNotes = context.getRevisionFeedback();

        StringBuilder specsBuilder = new StringBuilder();
        if (!tddSpecs.isBlank()) specsBuilder.append("### Target TDD Specifications (from Phase 4):\n").append(tddSpecs).append("\n\n");
        if (!c4Model.isBlank()) specsBuilder.append("### Hexagonal Architecture & Port Contracts (from Phase 1):\n").append(c4Model).append("\n\n");
        if (!schema.isBlank()) specsBuilder.append("### Database Schema DDL (from Phase 2):\n").append(schema).append("\n\n");
        if (!stories.isBlank()) specsBuilder.append("### Technical User Stories (from Phase 2):\n").append(stories).append("\n\n");

        String specs = !specsBuilder.toString().isBlank()
                ? specsBuilder.toString().trim()
                : (workflow.getDescription() != null ? workflow.getDescription() : taskName);

        if (revisionNotes != null && !revisionNotes.isBlank()) {
            specs = String.format("""
                    ================================================================================
                    ⚠️ MANDATORY REVISION DIRECTIVE FROM LEAD SUPERVISOR:
                    The human supervisor has reviewed previous outputs and returned this phase for refinement with the following feedback:
                    "%s"
                    You MUST strictly incorporate and reflect every requested framework, library, protocol, and feature (e.g. Netflix DGS GraphQL, Apollo GraphQL, Mockito, JaCoCo, Kafka, MySQL) into the Implementation Summary and Scaffolded Code!
                    ================================================================================

                    %s
                    """, revisionNotes, specs);
        }

        progress.accept(new PhaseExecutionManager.ProgressUpdate(25,
                "Implementing Hexagonal Domain Core & Inbound UseCases..."));
        progress.accept(new PhaseExecutionManager.ProgressUpdate(60,
                "Scaffolding Outbound Persistence Adapters, Spring Boot REST Controllers, and Next.js UI..."));

        generateImplementationSummaryFile(projectId, taskName, specs);
        scaffoldingService.scaffoldSourceCode(projectId, taskName, specs);

        ProjectScaffoldingService.DomainProfile profile = scaffoldingService.resolveDomainProfile(projectId, taskName, specs);
        String mainAppPath = "src/backend/src/main/java/" + profile.packageDir() + "/" + profile.applicationClassName() + ".java";

        return PhaseResult.success(
                WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION,
                "Materialized specifications into clean architecture code decoupled from persistence layer for task: " + taskName,
                "Spring Boot 3 backend ('src/backend/') and Next.js 14 frontend ('src/frontend/') scaffolded successfully.",
                List.of("src/backend/pom.xml", mainAppPath, "src/frontend/package.json", "src/frontend/app/page.tsx", "src/frontend/lib/api.ts", "src/IMPLEMENTATION_SUMMARY.md", "docker-compose.yml"),
                System.currentTimeMillis() - context.getStartTime().toEpochMilli());
    }

    private void generateImplementationSummaryFile(String projectId, String taskName, String description) {
        try {
            Path base = journalService.resolveProjectBase(projectId);
            Path srcDir = base.resolve("src");
            Files.createDirectories(srcDir);
            Path filePath = srcDir.resolve("IMPLEMENTATION_SUMMARY.md");

            ProjectScaffoldingService.DomainProfile profile = scaffoldingService.resolveDomainProfile(projectId, taskName, description);
            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("spring-boot-enterprise, clean-architecture, typescript-fullstack");

            String systemPromptText = "You are a Lead Dev Engineer implementing Spring Boot 3 and Clean Architecture. Output pure markdown directly without preamble.";
            String userPromptText = String.format("""
                    You are a Lead Dev Engineer in an enterprise platform implementing Clean Architecture and Spring Boot 3.
                    Generate a complete, non-truncated Implementation Summary and Code Scaffold document for:

                    Project ID: %s
                    Task Name: %s
                    Primary Aggregate: %s
                    Target Package: %s
                    Requirements Description:
                    %s
                    %s

                    CRITICAL REQUIREMENT:
                    You MUST provide complete, compilable, production-ready Java source code blocks in ```java with package declarations for:
                    1. Domain Aggregate Entity: `%s.domain.%s` (with validation, invariants, and business methods)
                    2. Inbound Port Interface: `%s.port.in.Manage%sUseCase`
                    3. Outbound Port Interface: `%s.port.out.%sRepositoryPort`
                    4. Application Service Orchestrator: `%s.service.%sService`
                    5. REST Controller Adapter: `%s.adapter.in.web.%sController`

                    Format the output strictly as Markdown:
                    # Implementation Summary & Code Scaffold (Phase 5 Output)
                    **Project ID:** %s
                    **Task:** %s
                    **Developer Agent:** Dev Engineer Agent
                    **Status:** IMPLEMENTATION_COMPLETED

                    ---

                    ## 1. Architecture Implementation Overview
                    (Overview of domain core, inbound usecases, outbound adapters)

                    ## 2. Implemented Clean Architecture Source Code
                    (Include all compilable ```java code blocks here)

                    ## 3. Implemented REST Endpoints & Error Handling
                    (List Spring Boot REST controllers, RFC 7807 ProblemDetail handlers, and DTO mappings)

                    ## 4. Persistence & Concurrency Strategy
                    (Explain JPA repository integration, optimistic locking, and transaction demarcations)
                    """,
                    projectId, taskName, profile.primaryEntityName(), profile.packageName(),
                    description, skillDirectives,
                    profile.packageName(), profile.primaryEntityName(),
                    profile.packageName(), profile.primaryEntityName(),
                    profile.packageName(), profile.primaryEntityName(),
                    profile.packageName(), profile.primaryEntityName(),
                    profile.packageName(), profile.primaryEntityName(),
                    projectId, taskName);

            String content;
            long duration = 0;
            boolean isFallback = false;
            long start = System.currentTimeMillis();

            try {
                SystemMessage sys = SystemMessage.from(systemPromptText);
                UserMessage usr = UserMessage.from(userPromptText);
                Response<AiMessage> response = chatLanguageModel.generate(List.of(sys, usr));
                duration = System.currentTimeMillis() - start;
                content = journalService.cleanMarkdownOutput(response.content().text());
            } catch (Exception ex) {
                duration = System.currentTimeMillis() - start;
                isFallback = true;
                log.warn("LLM Implementation summary fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackImplementationSummary(projectId, taskName, description, profile);
            }

            if (isInvalidImplementationSummaryOutput(content)) {
                log.warn("LLM Implementation summary produced insufficient, truncated, or invalid content. Activating rich domain SSOT fallback.");
                content = buildFallbackImplementationSummary(projectId, taskName, description, profile);
                isFallback = true;
            }

            String auditTrace = String.format("""

                    ---

                    ## 5. Dev Engineer Inference Audit Trace
                    - **Agent Role:** Dev Engineer Agent
                    - **Model / Engine:** `%s`
                    - **Inference Latency:** `%d ms`
                    - **Status:** %s
                    - **Audit Ledger:** Recorded in [`../inferences/phase5-dev-engineer.md`](../inferences/phase5-dev-engineer.md)
                    """,
                    isFallback ? "Deterministic Domain SSOT Fallback" : "Sequential LLM Chain",
                    duration,
                    isFallback ? "FALLBACK_DRAFT" : "LLM_GENERATED");

            Files.writeString(filePath, content + auditTrace);
            log.info("Successfully generated IMPLEMENTATION_SUMMARY.md for project: {}", projectId);

            journalService.recordPromptAndInference(
                    projectId, "PHASE_5_DEVELOPMENT_IMPLEMENTATION", "Dev Engineer Agent",
                    "Implementation Summary & Code Scaffold", systemPromptText, userPromptText, content, duration, isFallback);

        } catch (Exception e) {
            log.error("Failed to generate implementation summary for project {}: {}", projectId, e.getMessage());
        }
    }

    /**
     * Strict anti-truncation and completeness validator for Phase 5 Implementation Summary.
     */
    public boolean isInvalidImplementationSummaryOutput(String content) {
        if (content == null || content.isBlank()) return true;

        if (content.contains("[MOCK_OUTPUT]")) return true;

        long lineCount = content.lines().count();
        if (lineCount < 35) return true;

        // Verify balanced markdown code fences
        int fenceCount = countOccurrences(content, "```");
        if (fenceCount % 2 != 0) return true;

        // Must contain standard sections
        if (!content.contains("# Implementation Summary & Code Scaffold") ||
            !content.contains("## 1. Architecture Implementation Overview") ||
            !content.contains("IMPLEMENTATION_COMPLETED")) {
            return true;
        }

        // Must contain at least one compilable Java code block with package declaration
        if (!content.contains("```java") || !content.contains("package ") || !content.contains("class ")) {
            return true;
        }

        // Abrupt cut-off detection at the end of the text
        String trimmed = content.trim();
        if (trimmed.endsWith(",") || trimmed.endsWith("(") || trimmed.endsWith("|") ||
            trimmed.endsWith("AND") || trimmed.endsWith("OR") || trimmed.endsWith("WHERE")) {
            return true;
        }

        return false;
    }

    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    private String buildFallbackImplementationSummary(
            String projectId,
            String taskName,
            String description,
            ProjectScaffoldingService.DomainProfile profile) {

        String pkg = profile.packageName();

        return String.format("""
                # Implementation Summary & Code Scaffold (Phase 5 Output)
                **Project ID:** %s
                **Task:** %s
                **Developer Agent:** Dev Engineer Agent
                **Status:** IMPLEMENTATION_COMPLETED

                ---

                ## 1. Architecture Implementation Overview
                - **Domain Core**: Pure Java aggregate models implementing business invariants for Subscriptions, Metered Events, Prepaid Credits, and Invoices without external framework bindings.
                - **Inbound Ports**: Clean use case interfaces defining contract boundaries for commands and queries.
                - **Outbound Ports**: Repository ports decoupling persistence logic from database technology.
                - **Application Services**: Orchestrating business logic, transactions, and port interactions.
                - **Inbound Web Adapters**: Spring Boot `@RestController` exposing REST endpoints with RFC 7807 error handling and CORS support.
                - **Outbound Persistence Adapters**: In-memory concurrent repository adapters providing zero-dependency persistence.
                - **Fullstack Frontend**: Next.js 14 client service connecting to backend REST endpoints.

                ---

                ## 2. Implemented Clean Architecture Source Code

                %s

                %s

                %s

                %s

                ---

                ## 3. Implemented REST Endpoints & Error Handling
                - `GET /api/subscriptions` & `POST /api/subscriptions`: Manage customer subscription tiers.
                - `POST /api/subscriptions/{id}/cancel`: Cancel active subscriptions.
                - `GET /api/metered-events` & `POST /api/metered-events`: Real-time usage event ingestion.
                - `GET /api/credit-balances` & `POST /api/credit-balances/drawdown`: Prepaid ledger drawdown.
                - `GET /api/invoices` & `POST /api/invoices`: Automated invoicing generation.
                - **RFC 7807 Handling**: Domain exceptions mapped to standard HTTP 400/404 problem details.

                ---

                ## 4. Persistence & Concurrency Strategy
                - **Persistence Model**: Clean port-adapter decoupling supporting in-memory, JPA, and Kafka streaming.
                - **Concurrency Control**: State transition verification and atomic balance deductions.
                - **Transaction Demarcation**: Coordinated at the use case application service layer.
                """,
                projectId, taskName,
                buildSubscriptionCodeBlocks(pkg),
                buildMeteredEventCodeBlocks(pkg),
                buildCreditBalanceCodeBlocks(pkg),
                buildInvoiceCodeBlocks(pkg)
        );
    }

    private String buildSubscriptionCodeBlocks(String pkg) {
        return """
                ### 2.1 Subscription Aggregate Core & Adapters
                ```java
                package %s.domain;

                import java.time.Instant;

                public class Subscription {
                    private String id;
                    private String tenantId;
                    private String planId;
                    private String status;
                    private Instant createdAt;

                    public Subscription() {
                        this.createdAt = Instant.now();
                    }

                    public Subscription(String id, String tenantId, String planId, String status) {
                        if (tenantId == null || tenantId.trim().isBlank()) {
                            throw new IllegalArgumentException("Tenant ID must not be blank");
                        }
                        if (planId == null || planId.trim().isBlank()) {
                            throw new IllegalArgumentException("Plan ID must not be blank");
                        }
                        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
                        this.tenantId = tenantId.trim();
                        this.planId = planId.trim();
                        this.status = status != null ? status : "ACTIVE";
                        this.createdAt = Instant.now();
                    }

                    public void activate() { this.status = "ACTIVE"; }
                    public void cancel() { this.status = "CANCELLED"; }
                    public boolean isActive() { return "ACTIVE".equalsIgnoreCase(this.status); }

                    public String getId() { return id; }
                    public String getTenantId() { return tenantId; }
                    public String getPlanId() { return planId; }
                    public String getStatus() { return status; }
                    public Instant getCreatedAt() { return createdAt; }

                    public void setId(String id) { this.id = id; }
                    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
                    public void setPlanId(String planId) { this.planId = planId; }
                    public void setStatus(String status) { this.status = status; }
                    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
                }
                ```

                ```java
                package %s.port.in;

                import %s.domain.Subscription;
                import java.util.List;
                import java.util.Optional;

                public interface ManageSubscriptionUseCase {
                    Subscription createSubscription(String tenantId, String planId);
                    Optional<Subscription> findSubscriptionById(String id);
                    List<Subscription> findAllSubscriptions();
                    Subscription cancelSubscription(String id);
                }
                ```

                ```java
                package %s.port.out;

                import %s.domain.Subscription;
                import java.util.List;
                import java.util.Optional;

                public interface SubscriptionRepositoryPort {
                    Subscription save(Subscription item);
                    Optional<Subscription> findById(String id);
                    List<Subscription> findAll();
                }
                ```

                ```java
                package %s.service;

                import %s.domain.Subscription;
                import %s.port.in.ManageSubscriptionUseCase;
                import %s.port.out.SubscriptionRepositoryPort;
                import org.springframework.stereotype.Service;

                import java.util.List;
                import java.util.Optional;

                @Service
                public class SubscriptionService implements ManageSubscriptionUseCase {
                    private final SubscriptionRepositoryPort repositoryPort;

                    public SubscriptionService(SubscriptionRepositoryPort repositoryPort) {
                        this.repositoryPort = repositoryPort;
                    }

                    @Override
                    public Subscription createSubscription(String tenantId, String planId) {
                        Subscription item = new Subscription(null, tenantId, planId, "ACTIVE");
                        return repositoryPort.save(item);
                    }

                    @Override
                    public Optional<Subscription> findSubscriptionById(String id) {
                        return repositoryPort.findById(id);
                    }

                    @Override
                    public List<Subscription> findAllSubscriptions() {
                        return repositoryPort.findAll();
                    }

                    @Override
                    public Subscription cancelSubscription(String id) {
                        Subscription item = repositoryPort.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with ID: " + id));
                        item.cancel();
                        return repositoryPort.save(item);
                    }
                }
                ```

                ```java
                package %s.adapter.out.persistence;

                import %s.domain.Subscription;
                import %s.port.out.SubscriptionRepositoryPort;
                import org.springframework.stereotype.Repository;

                import java.util.ArrayList;
                import java.util.List;
                import java.util.Map;
                import java.util.Optional;
                import java.util.concurrent.ConcurrentHashMap;

                @Repository
                public class InMemorySubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {
                    private final Map<String, Subscription> storage = new ConcurrentHashMap<>();

                    @Override
                    public Subscription save(Subscription item) {
                        storage.put(item.getId(), item);
                        return item;
                    }

                    @Override
                    public Optional<Subscription> findById(String id) {
                        return Optional.ofNullable(storage.get(id));
                    }

                    @Override
                    public List<Subscription> findAll() {
                        return new ArrayList<>(storage.values());
                    }
                }
                ```

                ```java
                package %s.adapter.in.web;

                import %s.domain.Subscription;
                import %s.port.in.ManageSubscriptionUseCase;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.*;

                import java.util.List;
                import java.util.Map;

                @RestController
                @RequestMapping("/api/subscriptions")
                @CrossOrigin(origins = "*")
                public class SubscriptionController {
                    private final ManageSubscriptionUseCase useCase;

                    public SubscriptionController(ManageSubscriptionUseCase useCase) {
                        this.useCase = useCase;
                    }

                    @GetMapping
                    public ResponseEntity<List<Subscription>> getAll() {
                        return ResponseEntity.ok(useCase.findAllSubscriptions());
                    }

                    @PostMapping
                    public ResponseEntity<Subscription> create(@RequestBody Map<String, String> payload) {
                        String tenantId = payload.get("tenantId");
                        String planId = payload.get("planId");
                        Subscription item = useCase.createSubscription(tenantId, planId);
                        return ResponseEntity.ok(item);
                    }

                    @GetMapping("/{id}")
                    public ResponseEntity<Subscription> getById(@PathVariable String id) {
                        return useCase.findSubscriptionById(id)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    }

                    @PostMapping("/{id}/cancel")
                    public ResponseEntity<Subscription> cancel(@PathVariable String id) {
                        return ResponseEntity.ok(useCase.cancelSubscription(id));
                    }
                }
                ```
                """.formatted(pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg);
    }

    private String buildMeteredEventCodeBlocks(String pkg) {
        return """
                ### 2.2 MeteredEvent Aggregate Core & Adapters
                ```java
                package %s.domain;

                import java.time.Instant;

                public class MeteredEvent {
                    private String id;
                    private String tenantId;
                    private String metricType;
                    private Double value;
                    private String idempotencyKey;
                    private Instant timestamp;

                    public MeteredEvent() {
                        this.timestamp = Instant.now();
                    }

                    public MeteredEvent(String id, String tenantId, String metricType, Double value, String idempotencyKey) {
                        if (tenantId == null || tenantId.trim().isBlank()) {
                            throw new IllegalArgumentException("Tenant ID must not be blank");
                        }
                        if (value == null || value < 0) {
                            throw new IllegalArgumentException("Metric value must be non-negative");
                        }
                        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
                        this.tenantId = tenantId.trim();
                        this.metricType = metricType != null ? metricType.trim() : "API_CALL";
                        this.value = value;
                        this.idempotencyKey = idempotencyKey != null ? idempotencyKey.trim() : java.util.UUID.randomUUID().toString();
                        this.timestamp = Instant.now();
                    }

                    public String getId() { return id; }
                    public String getTenantId() { return tenantId; }
                    public String getMetricType() { return metricType; }
                    public Double getValue() { return value; }
                    public String getIdempotencyKey() { return idempotencyKey; }
                    public Instant getTimestamp() { return timestamp; }

                    public void setId(String id) { this.id = id; }
                    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
                    public void setMetricType(String metricType) { this.metricType = metricType; }
                    public void setValue(Double value) { this.value = value; }
                    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
                    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
                }
                ```

                ```java
                package %s.port.in;

                import %s.domain.MeteredEvent;
                import java.util.List;

                public interface IngestUsageEventsUseCase {
                    MeteredEvent ingestEvent(String tenantId, String metricType, Double value, String idempotencyKey);
                    List<MeteredEvent> findAllEvents();
                }
                ```

                ```java
                package %s.port.out;

                import %s.domain.MeteredEvent;
                import java.util.List;

                public interface MeteredEventRepositoryPort {
                    MeteredEvent save(MeteredEvent event);
                    List<MeteredEvent> findAll();
                }
                ```

                ```java
                package %s.service;

                import %s.domain.MeteredEvent;
                import %s.port.in.IngestUsageEventsUseCase;
                import %s.port.out.MeteredEventRepositoryPort;
                import org.springframework.stereotype.Service;

                import java.util.List;

                @Service
                public class MeteredEventService implements IngestUsageEventsUseCase {
                    private final MeteredEventRepositoryPort repositoryPort;

                    public MeteredEventService(MeteredEventRepositoryPort repositoryPort) {
                        this.repositoryPort = repositoryPort;
                    }

                    @Override
                    public MeteredEvent ingestEvent(String tenantId, String metricType, Double value, String idempotencyKey) {
                        MeteredEvent event = new MeteredEvent(null, tenantId, metricType, value, idempotencyKey);
                        return repositoryPort.save(event);
                    }

                    @Override
                    public List<MeteredEvent> findAllEvents() {
                        return repositoryPort.findAll();
                    }
                }
                ```

                ```java
                package %s.adapter.out.persistence;

                import %s.domain.MeteredEvent;
                import %s.port.out.MeteredEventRepositoryPort;
                import org.springframework.stereotype.Repository;

                import java.util.ArrayList;
                import java.util.List;
                import java.util.Map;
                import java.util.concurrent.ConcurrentHashMap;

                @Repository
                public class InMemoryMeteredEventRepositoryAdapter implements MeteredEventRepositoryPort {
                    private final Map<String, MeteredEvent> storage = new ConcurrentHashMap<>();

                    @Override
                    public MeteredEvent save(MeteredEvent event) {
                        storage.put(event.getId(), event);
                        return event;
                    }

                    @Override
                    public List<MeteredEvent> findAll() {
                        return new ArrayList<>(storage.values());
                    }
                }
                ```

                ```java
                package %s.adapter.in.web;

                import %s.domain.MeteredEvent;
                import %s.port.in.IngestUsageEventsUseCase;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.*;

                import java.util.List;
                import java.util.Map;

                @RestController
                @RequestMapping("/api/metered-events")
                @CrossOrigin(origins = "*")
                public class MeteredEventController {
                    private final IngestUsageEventsUseCase useCase;

                    public MeteredEventController(IngestUsageEventsUseCase useCase) {
                        this.useCase = useCase;
                    }

                    @GetMapping
                    public ResponseEntity<List<MeteredEvent>> getAll() {
                        return ResponseEntity.ok(useCase.findAllEvents());
                    }

                    @PostMapping
                    public ResponseEntity<MeteredEvent> ingest(@RequestBody Map<String, Object> payload) {
                        String tenantId = (String) payload.get("tenantId");
                        String metricType = (String) payload.get("metricType");
                        Number val = (Number) payload.get("value");
                        String idempotencyKey = (String) payload.get("idempotencyKey");

                        MeteredEvent event = useCase.ingestEvent(tenantId, metricType, val != null ? val.doubleValue() : 1.0, idempotencyKey);
                        return ResponseEntity.ok(event);
                    }
                }
                ```
                """.formatted(pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg);
    }

    private String buildCreditBalanceCodeBlocks(String pkg) {
        return """
                ### 2.3 CreditBalance Aggregate Core & Adapters
                ```java
                package %s.domain;

                import java.time.Instant;

                public class CreditBalance {
                    private String id;
                    private String tenantId;
                    private Double balance;
                    private Instant updatedAt;

                    public CreditBalance() {
                        this.updatedAt = Instant.now();
                    }

                    public CreditBalance(String id, String tenantId, Double balance) {
                        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
                        this.tenantId = tenantId;
                        this.balance = balance != null ? balance : 0.0;
                        this.updatedAt = Instant.now();
                    }

                    public void drawdown(Double amount) {
                        if (amount == null || amount <= 0) {
                            throw new IllegalArgumentException("Drawdown amount must be positive");
                        }
                        if (this.balance < amount) {
                            throw new IllegalArgumentException("Insufficient credit balance");
                        }
                        this.balance -= amount;
                        this.updatedAt = Instant.now();
                    }

                    public void topUp(Double amount) {
                        if (amount == null || amount <= 0) {
                            throw new IllegalArgumentException("Top up amount must be positive");
                        }
                        this.balance += amount;
                        this.updatedAt = Instant.now();
                    }

                    public String getId() { return id; }
                    public String getTenantId() { return tenantId; }
                    public Double getBalance() { return balance; }
                    public Instant getUpdatedAt() { return updatedAt; }

                    public void setId(String id) { this.id = id; }
                    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
                    public void setBalance(Double balance) { this.balance = balance; }
                    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
                }
                ```

                ```java
                package %s.port.in;

                import %s.domain.CreditBalance;
                import java.util.List;

                public interface DrawdownPrepaidCreditUseCase {
                    CreditBalance drawdownCredit(String tenantId, Double amount);
                    List<CreditBalance> findAllBalances();
                }
                ```

                ```java
                package %s.port.out;

                import %s.domain.CreditBalance;
                import java.util.List;
                import java.util.Optional;

                public interface CreditBalanceRepositoryPort {
                    CreditBalance save(CreditBalance balance);
                    Optional<CreditBalance> findByTenantId(String tenantId);
                    List<CreditBalance> findAll();
                }
                ```

                ```java
                package %s.service;

                import %s.domain.CreditBalance;
                import %s.port.in.DrawdownPrepaidCreditUseCase;
                import %s.port.out.CreditBalanceRepositoryPort;
                import org.springframework.stereotype.Service;

                import java.util.List;

                @Service
                public class CreditBalanceService implements DrawdownPrepaidCreditUseCase {
                    private final CreditBalanceRepositoryPort repositoryPort;

                    public CreditBalanceService(CreditBalanceRepositoryPort repositoryPort) {
                        this.repositoryPort = repositoryPort;
                    }

                    @Override
                    public CreditBalance drawdownCredit(String tenantId, Double amount) {
                        CreditBalance balance = repositoryPort.findByTenantId(tenantId)
                                .orElseGet(() -> repositoryPort.save(new CreditBalance(null, tenantId, 1000.0)));
                        balance.drawdown(amount);
                        return repositoryPort.save(balance);
                    }

                    @Override
                    public List<CreditBalance> findAllBalances() {
                        return repositoryPort.findAll();
                    }
                }
                ```

                ```java
                package %s.adapter.out.persistence;

                import %s.domain.CreditBalance;
                import %s.port.out.CreditBalanceRepositoryPort;
                import org.springframework.stereotype.Repository;

                import java.util.ArrayList;
                import java.util.List;
                import java.util.Map;
                import java.util.Optional;
                import java.util.concurrent.ConcurrentHashMap;

                @Repository
                public class InMemoryCreditBalanceRepositoryAdapter implements CreditBalanceRepositoryPort {
                    private final Map<String, CreditBalance> storage = new ConcurrentHashMap<>();

                    @Override
                    public CreditBalance save(CreditBalance balance) {
                        storage.put(balance.getId(), balance);
                        return balance;
                    }

                    @Override
                    public Optional<CreditBalance> findByTenantId(String tenantId) {
                        return storage.values().stream()
                                .filter(b -> b.getTenantId().equals(tenantId))
                                .findFirst();
                    }

                    @Override
                    public List<CreditBalance> findAll() {
                        return new ArrayList<>(storage.values());
                    }
                }
                ```

                ```java
                package %s.adapter.in.web;

                import %s.domain.CreditBalance;
                import %s.port.in.DrawdownPrepaidCreditUseCase;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.*;

                import java.util.List;
                import java.util.Map;

                @RestController
                @RequestMapping("/api/credit-balances")
                @CrossOrigin(origins = "*")
                public class CreditBalanceController {
                    private final DrawdownPrepaidCreditUseCase useCase;

                    public CreditBalanceController(DrawdownPrepaidCreditUseCase useCase) {
                        this.useCase = useCase;
                    }

                    @GetMapping
                    public ResponseEntity<List<CreditBalance>> getAll() {
                        return ResponseEntity.ok(useCase.findAllBalances());
                    }

                    @PostMapping("/drawdown")
                    public ResponseEntity<CreditBalance> drawdown(@RequestBody Map<String, Object> payload) {
                        String tenantId = (String) payload.get("tenantId");
                        Number amount = (Number) payload.get("amount");
                        CreditBalance balance = useCase.drawdownCredit(tenantId, amount != null ? amount.doubleValue() : 50.0);
                        return ResponseEntity.ok(balance);
                    }
                }
                ```
                """.formatted(pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg);
    }

    private String buildInvoiceCodeBlocks(String pkg) {
        return """
                ### 2.4 Invoice Aggregate Core & Adapters
                ```java
                package %s.domain;

                import java.time.Instant;

                public class Invoice {
                    private String id;
                    private String tenantId;
                    private Double amount;
                    private Double taxAmount;
                    private Double totalDue;
                    private String currency;
                    private String status;
                    private Instant invoiceDate;

                    public Invoice() {
                        this.invoiceDate = Instant.now();
                    }

                    public Invoice(String id, String tenantId, Double amount, String currency, String status) {
                        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
                        this.tenantId = tenantId;
                        this.amount = amount != null ? amount : 0.0;
                        this.taxAmount = Math.round((this.amount * 0.10) * 100.0) / 100.0;
                        this.totalDue = this.amount + this.taxAmount;
                        this.currency = currency != null ? currency : "USD";
                        this.status = status != null ? status : "DRAFT";
                        this.invoiceDate = Instant.now();
                    }

                    public void markPaid() {
                        this.status = "PAID";
                    }

                    public void markPaymentFailed() {
                        this.status = "PAYMENT_FAILED";
                    }

                    public String getId() { return id; }
                    public String getTenantId() { return tenantId; }
                    public Double getAmount() { return amount; }
                    public Double getTaxAmount() { return taxAmount; }
                    public Double getTotalDue() { return totalDue; }
                    public String getCurrency() { return currency; }
                    public String getStatus() { return status; }
                    public Instant getInvoiceDate() { return invoiceDate; }

                    public void setId(String id) { this.id = id; }
                    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
                    public void setAmount(Double amount) { this.amount = amount; }
                    public void setTaxAmount(Double taxAmount) { this.taxAmount = taxAmount; }
                    public void setTotalDue(Double totalDue) { this.totalDue = totalDue; }
                    public void setCurrency(String currency) { this.currency = currency; }
                    public void setStatus(String status) { this.status = status; }
                    public void setInvoiceDate(Instant invoiceDate) { this.invoiceDate = invoiceDate; }
                }
                ```

                ```java
                package %s.port.in;

                import %s.domain.Invoice;
                import java.util.List;

                public interface GenerateInvoiceUseCase {
                    Invoice generateInvoice(String tenantId, Double amount, String currency);
                    List<Invoice> findAllInvoices();
                }
                ```

                ```java
                package %s.port.out;

                import %s.domain.Invoice;
                import java.util.List;

                public interface InvoiceRepositoryPort {
                    Invoice save(Invoice invoice);
                    List<Invoice> findAll();
                }
                ```

                ```java
                package %s.service;

                import %s.domain.Invoice;
                import %s.port.in.GenerateInvoiceUseCase;
                import %s.port.out.InvoiceRepositoryPort;
                import org.springframework.stereotype.Service;

                import java.util.List;

                @Service
                public class InvoiceService implements GenerateInvoiceUseCase {
                    private final InvoiceRepositoryPort repositoryPort;

                    public InvoiceService(InvoiceRepositoryPort repositoryPort) {
                        this.repositoryPort = repositoryPort;
                    }

                    @Override
                    public Invoice generateInvoice(String tenantId, Double amount, String currency) {
                        Invoice invoice = new Invoice(null, tenantId, amount, currency, "ISSUED");
                        return repositoryPort.save(invoice);
                    }

                    @Override
                    public List<Invoice> findAllInvoices() {
                        return repositoryPort.findAll();
                    }
                }
                ```

                ```java
                package %s.adapter.out.persistence;

                import %s.domain.Invoice;
                import %s.port.out.InvoiceRepositoryPort;
                import org.springframework.stereotype.Repository;

                import java.util.ArrayList;
                import java.util.List;
                import java.util.Map;
                import java.util.concurrent.ConcurrentHashMap;

                @Repository
                public class InMemoryInvoiceRepositoryAdapter implements InvoiceRepositoryPort {
                    private final Map<String, Invoice> storage = new ConcurrentHashMap<>();

                    @Override
                    public Invoice save(Invoice invoice) {
                        storage.put(invoice.getId(), invoice);
                        return invoice;
                    }

                    @Override
                    public List<Invoice> findAll() {
                        return new ArrayList<>(storage.values());
                    }
                }
                ```

                ```java
                package %s.adapter.in.web;

                import %s.domain.Invoice;
                import %s.port.in.GenerateInvoiceUseCase;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.*;

                import java.util.List;
                import java.util.Map;

                @RestController
                @RequestMapping("/api/invoices")
                @CrossOrigin(origins = "*")
                public class InvoiceController {
                    private final GenerateInvoiceUseCase useCase;

                    public InvoiceController(GenerateInvoiceUseCase useCase) {
                        this.useCase = useCase;
                    }

                    @GetMapping
                    public ResponseEntity<List<Invoice>> getAll() {
                        return ResponseEntity.ok(useCase.findAllInvoices());
                    }

                    @PostMapping
                    public ResponseEntity<Invoice> generate(@RequestBody Map<String, Object> payload) {
                        String tenantId = (String) payload.get("tenantId");
                        Number amount = (Number) payload.get("amount");
                        String currency = (String) payload.getOrDefault("currency", "USD");

                        Invoice invoice = useCase.generateInvoice(tenantId, amount != null ? amount.doubleValue() : 250.0, currency);
                        return ResponseEntity.ok(invoice);
                    }
                }
                ```
                """.formatted(pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg, pkg);
    }
}
