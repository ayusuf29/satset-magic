package com.agentic.platform.interfaces.rest;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowTransitionLog;
import com.agentic.platform.interfaces.dto.WorkflowDtos.*;
import com.agentic.platform.service.WorkflowOrchestratorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/{id}/review-result")
    public ResponseEntity<WorkflowSummaryResponse> submitReviewResult(
            @PathVariable String id,
            @RequestBody CodeReviewResultRequest request) {
        WorkflowInstance updated = orchestratorService.handleCodeReviewResult(
                id,
                request.passed(),
                request.coveragePercentage(),
                request.feedback(),
                request.reviewer()
        );
        return ResponseEntity.ok(WorkflowSummaryResponse.from(updated));
    }
}
