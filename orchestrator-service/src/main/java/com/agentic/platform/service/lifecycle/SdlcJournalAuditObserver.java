package com.agentic.platform.service.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.stream.Collectors;

@Component
@Order(10)
public class SdlcJournalAuditObserver implements PhaseLifecycleObserver {

    private static final Logger log = LoggerFactory.getLogger(SdlcJournalAuditObserver.class);

    @Override
    public void onPreTask(PhaseContext context) {
        log.debug("[AUDIT OBSERVER: PRE-TASK] Registered phase initiation for project: {}", context.getProjectId());
    }

    @Override
    public void onTaskProgress(PhaseContext context, int progressPercentage, String currentActivity) {
        log.debug("[AUDIT OBSERVER: ON-TASK] Project: {} | Progress: {}% | Activity: {}",
                context.getProjectId(), progressPercentage, currentActivity);
    }

    @Override
    public void onPostTask(PhaseContext context, PhaseResult result) {
        try {
            Path base = resolveProjectBase(context.getProjectId());
            Files.createDirectories(base);
            Path filePath = base.resolve("SDLC_LOG.md");

            String artifactsList = result.getProducedArtifacts().isEmpty()
                    ? "Tercatat di audit ledger internal"
                    : result.getProducedArtifacts().stream().map(a -> "`" + a + "`").collect(Collectors.joining(", "));

            String entry = String.format("""

### [%s] %s
- **Agent Pelaksana:** %s
- **Skill Terpasang:** `%s`
- **Pola / Teknik:** %s
- **Alasan / Rationale (Why):** %s
- **Artefak yang Dihasilkan:** %s
- **Durasi Eksekusi:** %d ms
- **Prompt & Inference Log:** Tercatat lengkap di [PROMPTS_AND_INFERENCES.md](./PROMPTS_AND_INFERENCES.md)
- **Waktu:** %s

---
""",
                    context.getPhase(),
                    context.getPhase().name().replace("_", " "),
                    context.getAgentRole(),
                    context.getInstalledSkills(),
                    context.getPatternsAndTechniques(),
                    result.getRationale(),
                    artifactsList,
                    result.getDurationMs(),
                    Instant.now().toString()
            );

            if (!Files.exists(filePath)) {
                String header = "# SDLC Activity & Decision Journal\n**Project ID:** " + context.getProjectId() +
                        "\nLaporan menyeluruh aktivitas agen, teknik yang diterapkan, dan rationale (Why).\n---\n";
                Files.writeString(filePath, header + entry);
            } else {
                Files.writeString(filePath, entry, StandardOpenOption.APPEND);
            }

            log.info("[AUDIT OBSERVER: POST-TASK] Successfully appended phase journal entry to SDLC_LOG.md for project: {}", context.getProjectId());

        } catch (Exception ex) {
            log.error("Failed to append to SDLC_LOG.md in audit observer: {}", ex.getMessage());
        }
    }

    @Override
    public void onTaskFailed(PhaseContext context, Throwable throwable) {
        try {
            Path base = resolveProjectBase(context.getProjectId());
            Files.createDirectories(base);
            Path filePath = base.resolve("SDLC_LOG.md");

            String entry = String.format("""

### [%s] %s (FAILED)
- **Agent Pelaksana:** %s
- **Status:** FAILED / ERROR
- **Penyebab Error:** %s
- **Waktu:** %s

---
""",
                    context.getPhase(),
                    context.getPhase().name().replace("_", " "),
                    context.getAgentRole(),
                    throwable.getMessage(),
                    Instant.now().toString()
            );

            if (Files.exists(filePath)) {
                Files.writeString(filePath, entry, StandardOpenOption.APPEND);
            }
        } catch (Exception ex) {
            log.error("Failed to append error to SDLC_LOG.md: {}", ex.getMessage());
        }
    }

    private Path resolveProjectBase(String projectId) {
        if (Files.exists(Paths.get("orchestrator-service"))) {
            return Paths.get("workspace", "projects", projectId);
        }
        if (Files.exists(Paths.get("workspace"))) {
            return Paths.get("workspace", "projects", projectId);
        }
        return Paths.get("..", "workspace", "projects", projectId);
    }
}
