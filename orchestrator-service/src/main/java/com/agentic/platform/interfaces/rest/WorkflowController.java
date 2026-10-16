package com.agentic.platform.interfaces.rest;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowTransitionLog;
import com.agentic.platform.interfaces.dto.WorkflowDtos.CreateWorkflowRequest;
import com.agentic.platform.interfaces.dto.WorkflowDtos.HumanDecisionRequest;
import com.agentic.platform.interfaces.dto.WorkflowDtos.CouncilReviewResultRequest;
import com.agentic.platform.interfaces.dto.WorkflowDtos.WorkflowSummaryResponse;
import com.agentic.platform.interfaces.dto.WorkflowDtos.WorkflowDetailResponse;
import com.agentic.platform.service.WorkflowOrchestratorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final WorkflowOrchestratorService orchestratorService;

    public WorkflowController(WorkflowOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/start")
    public ResponseEntity<WorkflowSummaryResponse> startWorkflow(@RequestBody CreateWorkflowRequest request) {
        WorkflowInstance instance = orchestratorService.createWorkflow(
                request.projectId(),
                request.clientId(),
                request.taskName(),
                request.description(),
                request.initialPayload()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkflowSummaryResponse.from(instance));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowSummaryResponse>> getAllWorkflows() {
        List<WorkflowSummaryResponse> list = orchestratorService.getAllWorkflows().stream()
                .map(WorkflowSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDetailResponse> getWorkflowDetail(@PathVariable String id) {
        WorkflowInstance instance = orchestratorService.getWorkflowById(id);
        List<WorkflowTransitionLog> history = orchestratorService.getWorkflowHistory(id);
        return ResponseEntity.ok(WorkflowDetailResponse.from(instance, history));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String id) {
        orchestratorService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllWorkflows() {
        orchestratorService.deleteAllWorkflows();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/advance")
    public ResponseEntity<WorkflowSummaryResponse> advanceWorkflow(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "ORCHESTRATOR") String actor,
            @RequestParam(required = false, defaultValue = "Advancing to next phase") String comment) {
        WorkflowInstance updated = orchestratorService.advanceWorkflow(id, actor, comment);
        return ResponseEntity.ok(WorkflowSummaryResponse.from(updated));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<WorkflowSummaryResponse> approveWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) HumanDecisionRequest request) {
        String comment = request != null && request.comment() != null ? request.comment() : "Human approved via dashboard";
        String actor = request != null && request.actor() != null ? request.actor() : "HUMAN_OPERATOR";
        WorkflowInstance updated = orchestratorService.handleHumanDecision(id, "APPROVE", comment, actor);
        return ResponseEntity.ok(WorkflowSummaryResponse.from(updated));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<WorkflowSummaryResponse> rejectWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) HumanDecisionRequest request) {
        String comment = request != null && request.comment() != null ? request.comment() : "Human rejected via dashboard";
        String actor = request != null && request.actor() != null ? request.actor() : "HUMAN_OPERATOR";
        WorkflowInstance updated = orchestratorService.handleHumanDecision(id, "REJECT", comment, actor);
        return ResponseEntity.ok(WorkflowSummaryResponse.from(updated));
    }

    @PostMapping("/{id}/council-review")
    public ResponseEntity<WorkflowSummaryResponse> submitCouncilReview(
            @PathVariable String id,
            @RequestBody CouncilReviewResultRequest request) {
        WorkflowInstance updated = orchestratorService.handleCouncilReviewResult(
                id,
                request.architectApproved(),
                request.securityApproved(),
                request.performanceApproved(),
                request.coveragePercentage(),
                request.feedback()
        );
        return ResponseEntity.ok(WorkflowSummaryResponse.from(updated));
    }

    @PostMapping("/{id}/review-result")
    public ResponseEntity<WorkflowSummaryResponse> submitReviewResult(
            @PathVariable String id,
            @RequestBody CouncilReviewResultRequest request) {
        return submitCouncilReview(id, request);
    }

    @GetMapping("/{id}/artifacts")
    public ResponseEntity<List<Map<String, String>>> getProjectArtifacts(@PathVariable String id) {
        WorkflowInstance instance = orchestratorService.getWorkflowById(id);
        String projectId = instance.getProjectId();
        java.nio.file.Path baseDir = java.nio.file.Paths.get("..", "workspace", "projects", projectId);
        if (!java.nio.file.Files.exists(baseDir)) {
            baseDir = java.nio.file.Paths.get("workspace", "projects", projectId);
        }

        if (!java.nio.file.Files.exists(baseDir)) {
            return ResponseEntity.ok(List.of());
        }

        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(baseDir)) {
            java.nio.file.Path finalBase = baseDir;
            List<Map<String, String>> artifacts = stream
                    .filter(java.nio.file.Files::isRegularFile)
                    .map(p -> {
                        String rel = finalBase.relativize(p).toString().replace("\\", "/");
                        return Map.of(
                                "name", p.getFileName().toString(),
                                "path", rel,
                                "type", rel.endsWith(".md") ? "MARKDOWN" : (rel.endsWith(".json") ? "JSON" : (rel.endsWith(".sql") ? "SQL" : (rel.endsWith(".java") ? "JAVA" : "OTHER")))
                        );
                    })
                    .toList();
            return ResponseEntity.ok(artifacts);
        } catch (java.io.IOException e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/{id}/artifact-content")
    public ResponseEntity<Map<String, String>> getArtifactContent(
            @PathVariable String id,
            @RequestParam String path) {
        WorkflowInstance instance = orchestratorService.getWorkflowById(id);
        String projectId = instance.getProjectId();
        java.nio.file.Path baseDir = java.nio.file.Paths.get("..", "workspace", "projects", projectId);
        if (!java.nio.file.Files.exists(baseDir)) {
            baseDir = java.nio.file.Paths.get("workspace", "projects", projectId);
        }

        java.nio.file.Path target = baseDir.resolve(path).normalize();
        if (!target.startsWith(baseDir.normalize())) {
            throw new IllegalArgumentException("Invalid path traversal attempt");
        }

        if (!java.nio.file.Files.exists(target) || !java.nio.file.Files.isRegularFile(target)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Artifact not found: " + path));
        }

        try {
            String content = java.nio.file.Files.readString(target);
            return ResponseEntity.ok(Map.of(
                    "fileName", target.getFileName().toString(),
                    "path", path,
                    "content", content
            ));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to read artifact: " + e.getMessage()));
        }
    }
}
