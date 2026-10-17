package com.agentic.platform.service.phase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Service responsible for SDLC workspace provisioning, prompt/inference audit journaling,
 * and markdown sanitization across all autonomous agent phases.
 */
@Service
public class SdlcJournalService {

    private static final Logger log = LoggerFactory.getLogger(SdlcJournalService.class);

    public Path resolveProjectBase(String projectId) {
        if (Files.exists(Paths.get("orchestrator-service"))) {
            return Paths.get("workspace", "projects", projectId);
        }
        if (Files.exists(Paths.get("workspace"))) {
            return Paths.get("workspace", "projects", projectId);
        }
        return Paths.get("..", "workspace", "projects", projectId);
    }

    public String readArtifactContent(String projectId, String relativePath) {
        try {
            Path target = resolveProjectBase(projectId).resolve(relativePath);
            if (Files.exists(target) && Files.isRegularFile(target)) {
                return Files.readString(target);
            }
        } catch (Exception e) {
            log.warn("Could not read artifact {} for project {}: {}", relativePath, projectId, e.getMessage());
        }
        return "";
    }

    public void provisionProjectWorkspace(String projectId, String taskName, String description) {
        try {
            Path base = resolveProjectBase(projectId);
            Files.createDirectories(base.resolve("architecture"));
            Files.createDirectories(base.resolve("council-audits"));
            Files.createDirectories(base.resolve("backlog"));
            Files.createDirectories(base.resolve("tasks"));
            Files.createDirectories(base.resolve("tests"));
            Files.createDirectories(base.resolve("src"));
            Files.createDirectories(base.resolve("reports"));
            Files.createDirectories(base.resolve("database"));
            Files.createDirectories(base.resolve("inferences"));

            Path rawInputPath = base.resolve("raw_input.md");
            if (!Files.exists(rawInputPath)) {
                String rawContent = String.format("""
                        # Project %s: Raw Requirement

                        **Task:** %s
                        **Provisioned At:** %s

                        ## Description & Context
                        %s
                        """, projectId, taskName, Instant.now(), description != null ? description : taskName);
                Files.writeString(rawInputPath, rawContent);
            }
            log.info("Successfully provisioned isolated workspace directory structure for project: {}", projectId);
        } catch (Exception ex) {
            log.warn("Non-blocking workspace provisioning notice for project {}: {}", projectId, ex.getMessage());
        }
    }

    public String cleanMarkdownOutput(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```markdown")) {
            trimmed = trimmed.substring(11).trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3).trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    public String resolveInferenceFileName(String phase, String agentRole, String taskSummary) {
        String roleLower = agentRole != null ? agentRole.toLowerCase() : "";
        String summaryLower = taskSummary != null ? taskSummary.toLowerCase() : "";

        if (phase == null) {
            return "general-inference.md";
        }

        return switch (phase) {
            case "PHASE_0_REQUIREMENT_DISCOVERY" ->
                    (roleLower.contains("appsec") || summaryLower.contains("threat"))
                            ? "phase0-appsec-threat-model.md"
                            : "phase0-spec-writer.md";
            case "PHASE_1_ARCHITECTURE_AND_DESIGN" -> "phase1-system-architect.md";
            case "PHASE_2_BACKLOG_GENERATION" -> "phase2-backlog-spec.md";
            case "PHASE_3_TASK_DISPATCHING" -> "phase3-task-dispatcher.md";
            case "PHASE_4_TDD_PLANNING" -> "phase4-tdd-orchestrator.md";
            case "PHASE_5_DEVELOPMENT_IMPLEMENTATION" -> "phase5-dev-engineer.md";
            case "PHASE_6_CODE_REVIEW" -> "phase6-triad-council.md";
            case "PHASE_7_QA_VERIFICATION" -> "phase7-qa-sandbox.md";
            case "PHASE_8_READY_FOR_PRODUCTION_GATE" -> "phase8-production-gate.md";
            default -> phase.toLowerCase().replace("phase_", "phase").replace("_", "-") + ".md";
        };
    }

    public String recordPromptAndInference(
            String projectId,
            String phase,
            String agentRole,
            String taskSummary,
            String systemPrompt,
            String userPrompt,
            String responseText,
            long durationMs,
            boolean isFallback) {
        try {
            Path base = resolveProjectBase(projectId);
            Path inferencesDir = base.resolve("inferences");
            Files.createDirectories(inferencesDir);

            String fileName = resolveInferenceFileName(phase, agentRole, taskSummary);
            Path filePath = inferencesDir.resolve(fileName);

            String modelName = isFallback ? "Deterministic Domain Draft Fallback" : "Gemini 3.5 Flash";
            String statusBadge = isFallback ? "⚠️ FALLBACK_DRAFT" : "✅ LLM_GENERATED";

            String content = String.format("""
                    # AI Prompt & Inference Audit: [%s] %s
                    **Project ID:** %s  
                    **Phase:** %s  
                    **Executing Agent:** %s  
                    **Engine / Model:** `%s`  
                    **Inference Latency:** `%d ms`  
                    **Execution Status:** %s  
                    **Recorded At:** `%s`  

                    ---

                    ## 1. System Prompt
                    ```text
                    %s
                    ```

                    ## 2. User Prompt (Task Specifications & Context)
                    ```text
                    %s
                    ```

                    ## 3. Model Response Content
                    ```markdown
                    %s
                    ```
                    """,
                    phase, taskSummary,
                    projectId,
                    phase,
                    agentRole,
                    modelName,
                    durationMs,
                    statusBadge,
                    Instant.now(),
                    systemPrompt != null ? systemPrompt : "None",
                    userPrompt != null ? userPrompt : "None",
                    responseText != null ? responseText : "No content returned");

            Files.writeString(filePath, content);
            log.info("Successfully recorded prompt and response audit entry in inferences/{} for project: {}", fileName, projectId);
            return "inferences/" + fileName;
        } catch (Exception ex) {
            log.error("Failed to record prompt and inference to journal for project {}: {}", projectId, ex.getMessage());
            return "inferences/general-inference.md";
        }
    }
}
