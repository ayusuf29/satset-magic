package com.agentic.platform.service.lifecycle;

import com.agentic.platform.domain.model.WorkflowState;

import java.util.ArrayList;
import java.util.List;

public class PhaseResult {

    public enum Status {
        SUCCESS,
        VETOED,
        DEADLOCK,
        FAILED
    }

    private final WorkflowState phase;
    private final Status status;
    private final String rationale;
    private final String outputSummary;
    private final List<String> producedArtifacts;
    private final long durationMs;
    private final Double coveragePercentage;
    private final String feedback;

    public PhaseResult(
            WorkflowState phase,
            Status status,
            String rationale,
            String outputSummary,
            List<String> producedArtifacts,
            long durationMs,
            Double coveragePercentage,
            String feedback) {
        this.phase = phase;
        this.status = status;
        this.rationale = rationale;
        this.outputSummary = outputSummary;
        this.producedArtifacts = producedArtifacts != null ? producedArtifacts : new ArrayList<>();
        this.durationMs = durationMs;
        this.coveragePercentage = coveragePercentage;
        this.feedback = feedback;
    }

    public static PhaseResult success(
            WorkflowState phase,
            String rationale,
            String outputSummary,
            List<String> artifacts,
            long durationMs) {
        return new PhaseResult(phase, Status.SUCCESS, rationale, outputSummary, artifacts, durationMs, null, null);
    }

    public static PhaseResult success(
            WorkflowState phase,
            String rationale,
            String outputSummary,
            List<String> artifacts,
            long durationMs,
            double coverage) {
        return new PhaseResult(phase, Status.SUCCESS, rationale, outputSummary, artifacts, durationMs, coverage, null);
    }

    public static PhaseResult failure(
            WorkflowState phase,
            String rationale,
            String errorDetails,
            long durationMs) {
        return new PhaseResult(phase, Status.FAILED, rationale, errorDetails, List.of(), durationMs, null, errorDetails);
    }

    public WorkflowState getPhase() { return phase; }
    public Status getStatus() { return status; }
    public String getRationale() { return rationale; }
    public String getOutputSummary() { return outputSummary; }
    public List<String> getProducedArtifacts() { return producedArtifacts; }
    public long getDurationMs() { return durationMs; }
    public Double getCoveragePercentage() { return coveragePercentage; }
    public String getFeedback() { return feedback; }
}
