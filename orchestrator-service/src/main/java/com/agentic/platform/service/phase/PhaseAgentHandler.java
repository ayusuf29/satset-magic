package com.agentic.platform.service.phase;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.service.lifecycle.PhaseContext;
import com.agentic.platform.service.lifecycle.PhaseExecutionManager;
import com.agentic.platform.service.lifecycle.PhaseResult;

import java.util.function.Consumer;

/**
 * Strategy interface for autonomous phase execution handlers following SOLID principles (SRP, OCP, LSP, ISP, DIP).
 * Each phase agent handler is responsible for a single SDLC stage.
 */
public interface PhaseAgentHandler {

    /**
     * The workflow state / phase handled by this agent.
     */
    WorkflowState getHandledPhase();

    /**
     * Agent role name for telemetry and SDLC logging.
     */
    String getAgentRole();

    /**
     * Skills mounted on this agent.
     */
    String getInstalledSkills();

    /**
     * Patterns and architectural techniques utilized in this phase.
     */
    String getPatternsAndTechniques();

    /**
     * Executes the autonomous phase activities (LLM generation, scaffolding, audits, verification).
     */
    PhaseResult execute(WorkflowInstance workflow, PhaseContext context, Consumer<PhaseExecutionManager.ProgressUpdate> progress) throws Exception;

    /**
     * The next workflow state after successful completion, or null if the phase requires a human decision gate.
     */
    default WorkflowState getNextPhaseOnSuccess() {
        return null;
    }

    /**
     * Whether the orchestrator should automatically transition to getNextPhaseOnSuccess() upon phase success.
     */
    default boolean isAutoAdvance() {
        return false;
    }
}
