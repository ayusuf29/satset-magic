package com.agentic.platform.service.lifecycle;

import com.agentic.platform.domain.model.WorkflowState;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PhaseContext {
    private final String workflowId;
    private final String projectId;
    private final String taskName;
    private final String description;
    private final WorkflowState phase;
    private final String agentRole;
    private final String installedSkills;
    private final String patternsAndTechniques;
    private final String revisionFeedback;
    private final Instant startTime;
    private final Map<String, Object> attributes;

    public PhaseContext(
            String workflowId,
            String projectId,
            String taskName,
            String description,
            WorkflowState phase,
            String agentRole,
            String installedSkills,
            String patternsAndTechniques,
            String revisionFeedback) {
        this.workflowId = workflowId;
        this.projectId = projectId;
        this.taskName = taskName;
        this.description = description;
        this.phase = phase;
        this.agentRole = agentRole;
        this.installedSkills = installedSkills;
        this.patternsAndTechniques = patternsAndTechniques;
        this.revisionFeedback = revisionFeedback;
        this.startTime = Instant.now();
        this.attributes = new HashMap<>();
    }

    public static PhaseContext of(
            String workflowId,
            String projectId,
            String taskName,
            String description,
            WorkflowState phase,
            String agentRole,
            String installedSkills,
            String patternsAndTechniques,
            String revisionFeedback) {
        return new PhaseContext(workflowId, projectId, taskName, description, phase, agentRole, installedSkills, patternsAndTechniques, revisionFeedback);
    }

    public static PhaseContext of(
            String workflowId,
            String projectId,
            String taskName,
            String description,
            WorkflowState phase,
            String agentRole,
            String installedSkills,
            String patternsAndTechniques) {
        return new PhaseContext(workflowId, projectId, taskName, description, phase, agentRole, installedSkills, patternsAndTechniques, null);
    }

    public String getWorkflowId() { return workflowId; }
    public String getProjectId() { return projectId; }
    public String getTaskName() { return taskName; }
    public String getDescription() { return description; }
    public WorkflowState getPhase() { return phase; }
    public String getAgentRole() { return agentRole; }
    public String getInstalledSkills() { return installedSkills; }
    public String getPatternsAndTechniques() { return patternsAndTechniques; }
    public String getRevisionFeedback() { return revisionFeedback; }
    public boolean hasRevisionFeedback() {
        return revisionFeedback != null && !revisionFeedback.isBlank();
    }
    public Instant getStartTime() { return startTime; }
    public Map<String, Object> getAttributes() { return attributes; }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> clazz) {
        Object val = this.attributes.get(key);
        return clazz.isInstance(val) ? (T) val : null;
    }
}
