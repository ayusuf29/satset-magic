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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Phase3TaskDispatchingAgent implements PhaseAgentHandler {

    private static final Logger log = LoggerFactory.getLogger(Phase3TaskDispatchingAgent.class);

    private final ChatLanguageModel chatLanguageModel;
    private final SdlcJournalService journalService;
    private final SkillLoaderService skillLoaderService;

    public Phase3TaskDispatchingAgent(
            ChatLanguageModel chatLanguageModel,
            SdlcJournalService journalService,
            SkillLoaderService skillLoaderService) {
        this.chatLanguageModel = chatLanguageModel;
        this.journalService = journalService;
        this.skillLoaderService = skillLoaderService;
    }

    public record DispatchedStory(String id, String title) {}

    @Override
    public WorkflowState getHandledPhase() {
        return WorkflowState.PHASE_3_TASK_DISPATCHING;
    }

    @Override
    public String getAgentRole() {
        return "Task Dispatcher Agent";
    }

    @Override
    public String getInstalledSkills() {
        return "clean-architecture, c4-model-designer";
    }

    @Override
    public String getPatternsAndTechniques() {
        return "Team Topologies & Port/Adapter Modular Boundaries";
    }

    @Override
    public WorkflowState getNextPhaseOnSuccess() {
        return WorkflowState.PHASE_3_WAITING_HUMAN_APPROVAL;
    }

    @Override
    public boolean isAutoAdvance() {
        return false; // Pauses for Human Task Dispatch Approval Gate
    }

    @Override
    public PhaseResult execute(
            WorkflowInstance workflow,
            PhaseContext context,
            java.util.function.Consumer<PhaseExecutionManager.ProgressUpdate> progress) throws Exception {

        String projectId = workflow.getProjectId();
        String taskName = workflow.getTaskName();

        // Single Source of Truth: Consume technical user stories and C4 component architecture
        String stories = journalService.readArtifactContent(projectId, "backlog/TECHNICAL_USER_STORIES.md");
        String c4Model = journalService.readArtifactContent(projectId, "architecture/C4_MODEL.md");
        String revisionNotes = context.getRevisionFeedback();
        String specs = (!stories.isBlank() || !c4Model.isBlank())
                ? "### Technical User Stories (from Phase 2 Backlog):\n" + stories +
                  (!c4Model.isBlank() ? "\n\n### C4 Architecture & Ports (from Phase 1):\n" + c4Model : "")
                : (workflow.getDescription() != null ? workflow.getDescription() : taskName);

        if (revisionNotes != null && !revisionNotes.isBlank()) {
            specs = String.format("""
                    ================================================================================
                    ⚠️ MANDATORY REVISION DIRECTIVE FROM LEAD SUPERVISOR:
                    The human supervisor has reviewed previous outputs and returned this phase for refinement with the following feedback:
                    "%s"
                    You MUST strictly incorporate and reflect every requested requirement, team stream, and component into the Task Dispatch Allocation Matrix!
                    ================================================================================

                    %s
                    """, revisionNotes, specs);
        }

        progress.accept(new PhaseExecutionManager.ProgressUpdate(30,
                "Analyzing domain dependencies & allocating stream-aligned team topology..."));
        generateDispatchAllocationFile(projectId, taskName, specs);

        progress.accept(new PhaseExecutionManager.ProgressUpdate(80,
                "Defining interface port boundaries & dispatching to development streams..."));

        return PhaseResult.success(
                WorkflowState.PHASE_3_TASK_DISPATCHING,
                "Partitioned core domain, inbound/outbound ports, and stream-aligned team assignments for task: " + taskName,
                "Team Topology Dispatch Matrix generated to 'tasks/DISPATCH_ALLOCATION.md'",
                List.of("tasks/DISPATCH_ALLOCATION.md"),
                System.currentTimeMillis() - context.getStartTime().toEpochMilli());
    }

    private void generateDispatchAllocationFile(String projectId, String taskName, String description) {
        try {
            Path base = journalService.resolveProjectBase(projectId);
            Path tasksDir = base.resolve("tasks");
            Files.createDirectories(tasksDir);
            Path filePath = tasksDir.resolve("DISPATCH_ALLOCATION.md");

            String skillDirectives = skillLoaderService.loadSkillsAsPromptContext("clean-architecture, c4-model-designer");

            String systemPromptText = "You are an expert Task Dispatcher Agent following Clean Architecture and Team Topologies. Output pure markdown directly without preamble. You MUST generate all 3 sections completely with a fully populated matrix without stopping or truncation.";
            String userPromptText = String.format("""
                    You are an enterprise Task Dispatcher Agent following Clean Architecture (Hexagonal Ports & Adapters) and Team Topologies.
                    Create a concrete, fully populated modular task allocation and stream-aligned team dispatch document for:

                    Project ID: %s
                    Task Name: %s
                    Requirements Description:
                    %s
                    %s

                    Format the output strictly as Markdown:
                    # Task Dispatch Allocation & Team Topologies (Phase 3 Output)
                    **Project ID:** %s
                    **Task:** %s
                    **Dispatcher Agent:** Task Dispatcher Agent
                    **Pattern:** Hexagonal Inbound/Outbound Port Dispatching & Team Topologies
                    **Status:** DISPATCHED_TO_ENGINEERING

                    ---

                    ## 1. Stream-Aligned Team Allocation Matrix
                    | Stream / Team | Component Responsibility | Inbound Port | Outbound Port | Target Module |
                    | :--- | :--- | :--- | :--- | :--- |
                    (Provide concrete rows tailored to the domain: Core Domain Stream, Inbound Web Adapter Stream, Outbound Persistence Stream, Security & Governance Stream, Frontend Experience Stream)

                    ## 2. Module Boundary Isolation & Inversion of Control
                    (Define concrete interface specifications, dependency injection boundaries, and zero-framework domain isolation)

                    ## 3. Dispatch Checklist & Execution Sequence
                    (Step-by-step engineering sequence for Phase 4 TDD and Phase 5 Dev Implementation agents)
                    """, projectId, taskName, description, skillDirectives, projectId, taskName);

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
                log.warn("LLM Dispatch allocation fallback triggered for project {}: {}", projectId, ex.getMessage());
                content = buildFallbackDispatchAllocation(projectId, taskName, description);
            }

            if (isInvalidDispatchAllocationOutput(content)) {
                log.warn("LLM Dispatch allocation produced insufficient, truncated, or unclosed content. Falling back to dynamic domain dispatch allocation.");
                content = buildFallbackDispatchAllocation(projectId, taskName, description);
                isFallback = true;
            }

            String auditTrace = String.format("""

                    ---

                    ## 4. Task Dispatcher Inference Audit Trace
                    - **Agent Role:** Task Dispatcher Agent
                    - **Model / Engine:** `%s`
                    - **Inference Latency:** `%d ms`
                    - **Status:** %s
                    - **Audit Ledger:** Recorded in [`../inferences/phase3-task-dispatcher.md`](../inferences/phase3-task-dispatcher.md)
                    """,
                    isFallback ? "Deterministic Domain Draft Fallback" : "Gemini 3.5 Flash",
                    duration,
                    isFallback ? "FALLBACK_DRAFT" : "LLM_GENERATED");

            String finalDoc = content.contains("Inference Audit Trace")
                    ? content
                    : content.stripTrailing() + "\n" + auditTrace;

            Files.writeString(filePath, finalDoc);
            log.info("Successfully generated DISPATCH_ALLOCATION.md for project: {}", projectId);

            journalService.recordPromptAndInference(
                    projectId, "PHASE_3_TASK_DISPATCHING", "Task Dispatcher Agent",
                    "Task Dispatch Allocation & Team Topologies", systemPromptText, userPromptText, content, duration, isFallback);

        } catch (Exception e) {
            log.error("Failed to generate task dispatch allocation for project {}: {}", projectId, e.getMessage());
        }
    }

    public boolean isInvalidDispatchAllocationOutput(String content) {
        if (content == null || content.isBlank() || content.contains("[MOCK_OUTPUT]")) {
            return true;
        }
        if (content.lines().count() < 25) {
            log.warn("Dispatch allocation output has too few lines ({} < 25). Truncation detected.", content.lines().count());
            return true;
        }
        int fenceCount = 0;
        int idx = 0;
        while ((idx = content.indexOf("```", idx)) != -1) {
            fenceCount++;
            idx += 3;
        }
        if (fenceCount % 2 != 0) {
            log.warn("Dispatch allocation output has unclosed code fence (count: {}). Truncation detected.", fenceCount);
            return true;
        }
        boolean hasMatrix = content.contains("Team Allocation Matrix") || content.contains("Stream-Aligned");
        boolean hasIsolation = content.contains("Boundary Isolation") || content.contains("Inversion of Control");
        boolean hasChecklist = content.contains("Dispatch Checklist") || content.contains("Execution Sequence");
        if (!hasMatrix || !hasIsolation || !hasChecklist) {
            log.warn("Dispatch allocation output is missing required sections. Truncation detected.");
            return true;
        }
        String trimmed = content.stripTrailing();
        if ((trimmed.endsWith("|") && !trimmed.contains("\n|")) || trimmed.endsWith("and") || trimmed.endsWith("with") || trimmed.endsWith(",")) {
            log.warn("Dispatch allocation ends abruptly. Truncation detected.");
            return true;
        }
        return false;
    }

    /**
     * Dynamically builds the dispatch allocation document from earlier SDLC phase artifacts.
     * Extracts domain aggregates, user stories, and hexagonal ports to construct stream-aligned team topology.
     */
    public String buildFallbackDispatchAllocation(String projectId, String taskName, String description) {
        String storiesText = journalService.readArtifactContent(projectId, "backlog/TECHNICAL_USER_STORIES.md");
        String c4Model = journalService.readArtifactContent(projectId, "architecture/C4_MODEL.md");

        List<String> aggregates = parseAggregates(c4Model, storiesText);
        List<String> useCases = parseInboundUseCases(c4Model, storiesText);
        List<String> outboundPorts = parseOutboundPorts(c4Model, storiesText);
        List<DispatchedStory> stories = parseUserStorySummaries(storiesText);

        if (aggregates.isEmpty()) {
            aggregates = List.of(extractNounFromTask(taskName));
        }

        String primaryAggregate = aggregates.get(0);
        String allAggregatesStr = String.join(", ", aggregates);

        String mainUseCase = useCases.isEmpty() ? "Manage" + primaryAggregate + "UseCase" : useCases.get(0);
        String mainRepo = outboundPorts.isEmpty() ? primaryAggregate + "RepositoryPort" : outboundPorts.get(0);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
                # Task Dispatch Allocation & Team Topologies (Phase 3 Output)
                **Project ID:** %s
                **Task:** %s
                **Dispatcher Agent:** Task Dispatcher Agent (Autonomous SSOT Mapping)
                **Pattern:** Hexagonal Inbound/Outbound Port Dispatching & Team Topologies
                **Status:** DISPATCHED_TO_ENGINEERING
                **Reference Documents:** [`TECHNICAL_USER_STORIES.md`](../backlog/TECHNICAL_USER_STORIES.md), [`C4_MODEL.md`](../architecture/C4_MODEL.md)

                ---

                ## 1. Stream-Aligned Team Allocation Matrix

                | Stream / Team | Component Responsibility | Inbound Port | Outbound Port | Target Module |
                | :--- | :--- | :--- | :--- | :--- |
                | **Core Domain Stream** | Pure business invariants, aggregate lifecycles, and state transitions for: %s | `%s` | `%s` | `src/backend/domain` |
                | **Inbound Web Adapter Stream** | Spring Boot 3.3 REST Controllers, RFC 7807 Error Handlers, and Jakarta Bean Validation (`@Valid`, `@NotNull`) | REST Controllers / GraphQL Fetchers | Inbound Domain Ports | `src/backend/adapter/in` |
                | **Outbound Persistence Stream** | PostgreSQL 16 JPA Repositories, Spring Data JPA integration, and `@Version` optimistic locking | `%s` | PostgreSQL Aurora JDBC Adapter | `src/backend/adapter/out` |
                | **Security & Governance Stream** | Multi-tenant context extraction (`TenantContextHolder`), STRIDE mitigations, and append-only `audit_logs` | Security Context Filter | TransitionLogPort | `src/backend/config` |
                | **Frontend Experience Stream** | Next.js 14 App Router management dashboard, responsive telemetry cards, and interactive forms | Next.js Client Components | Backend REST / GraphQL API | `src/frontend/app` |
                """,
                projectId, taskName,
                allAggregatesStr,
                mainUseCase,
                mainRepo,
                mainRepo));

        if (useCases.size() > 1 || outboundPorts.size() > 1) {
            sb.append("\n### Granular Port-to-Stream Mapping\n");
            sb.append("| Stream Sub-Domain | Primary Inbound Port | Primary Outbound Port | Target Entity |\n");
            sb.append("| :--- | :--- | :--- | :--- |\n");

            int maxItems = Math.max(aggregates.size(), Math.max(useCases.size(), outboundPorts.size()));
            for (int i = 0; i < maxItems; i++) {
                String agg = i < aggregates.size() ? aggregates.get(i) : primaryAggregate;
                String uc = i < useCases.size() ? useCases.get(i) : "Manage" + agg + "UseCase";
                String port = i < outboundPorts.size() ? outboundPorts.get(i) : agg + "RepositoryPort";
                sb.append(String.format("| %s Sub-System | `%s` | `%s` | `%s` |\n", agg, uc, port, agg));
            }
        }

        if (!stories.isEmpty()) {
            sb.append("\n### User Stories Dispatched to Engineering Streams\n");
            for (DispatchedStory story : stories) {
                sb.append(String.format("- **%s:** %s &rarr; Dispatched to `Core Domain Stream` & `Inbound Web Adapter Stream`\n", story.id(), story.title()));
            }
        }

        sb.append(String.format("""

                ---

                ## 2. Module Boundary Isolation & Inversion of Control
                1. **Zero Framework Dependencies in Domain Core**:
                   The `domain` package must contain pure Java models representing %s without Spring Boot, Hibernate, or Jakarta annotations.
                2. **Strict Inversion of Control**:
                   Application services implement inbound use cases (`%s`) and consume outbound ports (`%s`). Web controllers and JPA repositories never reference each other directly.
                3. **Granular Interface Segregation (ISP)**:
                   Ports are partitioned by functional responsibility rather than coalesced into monolithic services.
                4. **Multi-Tenant Data Isolation**:
                   Every repository query and GraphQL fetcher strictly enforces tenant boundary via thread-local context.

                ---

                ## 3. Dispatch Checklist & Execution Sequence
                - [x] **Step 1 (Phase 3):** Team topology boundaries and port interfaces established and dispatched.
                - [ ] **Step 2 (Phase 4):** TDD Orchestrator Agent scaffolds unit and integration test suites enforcing >= 70%% branch coverage.
                - [ ] **Step 3 (Phase 5):** Dev Implementation Agent materializes Hexagonal domain core, Spring Boot REST controllers, and Next.js dashboard.
                - [ ] **Step 4 (Phase 6):** Triad Council Consortium conducts multi-agent review (Architect, AppSec Auditor, Reliability Lead).
                """, allAggregatesStr, mainUseCase, mainRepo));

        return sb.toString();
    }

    public List<String> parseAggregates(String c4Model, String storiesText) {
        LinkedHashSet<String> aggregates = new LinkedHashSet<>();
        if (c4Model != null && !c4Model.isBlank()) {
            Matcher mermaidAgg = Pattern.compile("Agg_\\w+\\[\"([A-Za-z0-9]+)(?:\\s+Aggregate|\\s+Root)?").matcher(c4Model);
            while (mermaidAgg.find()) {
                String candidate = mermaidAgg.group(1).trim();
                if (!candidate.equalsIgnoreCase("Domain") && !candidate.equalsIgnoreCase("Entity") && !candidate.equalsIgnoreCase("Item")) {
                    aggregates.add(candidate);
                }
            }
            Matcher rootAgg = Pattern.compile("(?:Aggregate Root|Aggregate)[:\\s*`]+([A-Za-z0-9]+)").matcher(c4Model);
            while (rootAgg.find()) {
                String candidate = rootAgg.group(1).trim();
                if (!candidate.equalsIgnoreCase("Domain") && !candidate.equalsIgnoreCase("Entity") && !candidate.equalsIgnoreCase("Item")) {
                    aggregates.add(candidate);
                }
            }
        }
        if (storiesText != null && !storiesText.isBlank()) {
            Matcher storyAgg = Pattern.compile("(?:Domain Aggregate|Aggregate)[:\\s*`]+([A-Za-z0-9]+)").matcher(storiesText);
            while (storyAgg.find()) {
                aggregates.add(storyAgg.group(1).trim());
            }
        }
        return new ArrayList<>(aggregates);
    }

    public List<String> parseInboundUseCases(String c4Model, String storiesText) {
        LinkedHashSet<String> useCases = new LinkedHashSet<>();
        if (c4Model != null && !c4Model.isBlank()) {
            Matcher m = Pattern.compile("(?:public\\s+interface\\s+|Port_\\w+\\[\")([A-Za-z0-9]+UseCase)").matcher(c4Model);
            while (m.find()) {
                useCases.add(m.group(1).trim());
            }
        }
        if (storiesText != null && !storiesText.isBlank()) {
            Matcher m = Pattern.compile("`([A-Za-z0-9]+UseCase)`").matcher(storiesText);
            while (m.find()) {
                useCases.add(m.group(1).trim());
            }
        }
        return new ArrayList<>(useCases);
    }

    public List<String> parseOutboundPorts(String c4Model, String storiesText) {
        LinkedHashSet<String> ports = new LinkedHashSet<>();
        if (c4Model != null && !c4Model.isBlank()) {
            Matcher m = Pattern.compile("(?:public\\s+interface\\s+|Port_\\w+\\[\")([A-Za-z0-9]+(?:RepositoryPort|PublisherPort|GatewayPort|Port))").matcher(c4Model);
            while (m.find()) {
                ports.add(m.group(1).trim());
            }
        }
        if (storiesText != null && !storiesText.isBlank()) {
            Matcher m = Pattern.compile("`([A-Za-z0-9]+(?:RepositoryPort|PublisherPort|GatewayPort|Port))`").matcher(storiesText);
            while (m.find()) {
                ports.add(m.group(1).trim());
            }
        }
        return new ArrayList<>(ports);
    }

    public List<DispatchedStory> parseUserStorySummaries(String storiesText) {
        List<DispatchedStory> list = new ArrayList<>();
        if (storiesText == null || storiesText.isBlank()) {
            return list;
        }
        Matcher m = Pattern.compile("(?m)^###\\s+(US-\\d+)[:\\s]+(.*)$").matcher(storiesText);
        while (m.find()) {
            list.add(new DispatchedStory(m.group(1).trim(), m.group(2).trim()));
        }
        return list;
    }

    private String extractNounFromTask(String taskName) {
        if (taskName == null || taskName.isBlank()) return "Domain";
        Set<String> stopWords = Set.of(
                "a", "an", "the", "and", "or", "for", "with", "to", "in", "on", "at",
                "create", "build", "develop", "implement", "app", "apps", "application",
                "platform", "system", "service", "portal", "tool", "website"
        );
        String[] words = taskName.replaceAll("[^a-zA-Z0-9\\s]", " ").trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            String lower = w.toLowerCase();
            if (!stopWords.contains(lower) && !w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
                if (sb.length() >= 15) break;
            }
        }
        String candidate = sb.toString();
        return candidate.isBlank() ? "Domain" : candidate;
    }
}
