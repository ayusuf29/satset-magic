package com.agentic.platform.service.lifecycle;

import com.agentic.platform.domain.model.AgentEvent;
import com.agentic.platform.service.WorkflowEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class WebSocketTelemetryObserver implements PhaseLifecycleObserver {

    private static final Logger log = LoggerFactory.getLogger(WebSocketTelemetryObserver.class);

    private final WorkflowEventPublisher eventPublisher;

    public WebSocketTelemetryObserver(WorkflowEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onPreTask(PhaseContext context) {
        String msg = String.format("[%s] Memulai tugas pada fase %s untuk task: %s (Skills: %s)",
                context.getAgentRole(), context.getPhase().name(), context.getTaskName(), context.getInstalledSkills());

        eventPublisher.publishEvent(AgentEvent.of(
                context.getWorkflowId(),
                context.getPhase(),
                "AGENT_START",
                msg
        ));
        log.info("[TELEMETRY OBSERVER: PRE-TASK] Emitted AGENT_START for workflow: {}", context.getWorkflowId());
    }

    @Override
    public void onTaskProgress(PhaseContext context, int progressPercentage, String currentActivity) {
        String msg = String.format("[%s][%d%%] %s",
                context.getAgentRole(), progressPercentage, currentActivity);

        eventPublisher.publishEvent(AgentEvent.of(
                context.getWorkflowId(),
                context.getPhase(),
                "AGENT_PROGRESS",
                msg
        ));
    }

    @Override
    public void onPostTask(PhaseContext context, PhaseResult result) {
        String msg = String.format("[%s] Fase %s selesai diselesaikan: %s",
                context.getAgentRole(), context.getPhase().name(), result.getOutputSummary());

        eventPublisher.publishEvent(AgentEvent.of(
                context.getWorkflowId(),
                context.getPhase(),
                "AGENT_OUTPUT",
                msg
        ));
        log.info("[TELEMETRY OBSERVER: POST-TASK] Emitted AGENT_OUTPUT for workflow: {}", context.getWorkflowId());
    }

    @Override
    public void onTaskFailed(PhaseContext context, Throwable throwable) {
        String msg = String.format("[%s] Fase %s gagal: %s",
                context.getAgentRole(), context.getPhase().name(), throwable.getMessage());

        eventPublisher.publishEvent(AgentEvent.of(
                context.getWorkflowId(),
                context.getPhase(),
                "AGENT_OUTPUT",
                msg
        ));
    }
}
