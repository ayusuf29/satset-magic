package com.agentic.platform.service;

import com.agentic.platform.domain.model.AgentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WorkflowEventPublisherImpl implements WorkflowEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEventPublisherImpl.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final List<SseEmitter> globalEmitters = new CopyOnWriteArrayList<>();
    private final Map<String, List<SseEmitter>> workflowEmitters = new ConcurrentHashMap<>();

    public WorkflowEventPublisherImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public SseEmitter registerGlobalEmitter() {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout
        globalEmitters.add(emitter);
        emitter.onCompletion(() -> globalEmitters.remove(emitter));
        emitter.onTimeout(() -> globalEmitters.remove(emitter));
        emitter.onError((e) -> globalEmitters.remove(emitter));
        return emitter;
    }

    @Override
    public SseEmitter registerWorkflowEmitter(String workflowId) {
        SseEmitter emitter = new SseEmitter(180_000L);
        workflowEmitters.computeIfAbsent(workflowId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeWorkflowEmitter(workflowId, emitter));
        emitter.onTimeout(() -> removeWorkflowEmitter(workflowId, emitter));
        emitter.onError((e) -> removeWorkflowEmitter(workflowId, emitter));
        return emitter;
    }

    private void removeWorkflowEmitter(String workflowId, SseEmitter emitter) {
        List<SseEmitter> list = workflowEmitters.get(workflowId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    @Override
    public void publishEvent(AgentEvent event) {
        // 1. Broadcast to WebSocket STOMP
        try {
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/agent-activities", event);
                if (event.state().isHumanGate() || "DEADLOCK".equalsIgnoreCase(event.type()) || "ALERT".equalsIgnoreCase(event.type())) {
                    messagingTemplate.convertAndSend("/topic/hitl-alerts", event);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to publish event to WebSocket broker: {}", ex.getMessage());
        }

        // 2. Broadcast to Global SSE Emitters
        sendToEmitters(globalEmitters, event);

        // 3. Broadcast to Workflow-specific SSE Emitters
        List<SseEmitter> specific = workflowEmitters.get(event.workflowId());
        if (specific != null && !specific.isEmpty()) {
            sendToEmitters(specific, event);
        }
    }

    private void sendToEmitters(List<SseEmitter> emitters, AgentEvent event) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.type())
                        .id(event.id())
                        .data(event));
            } catch (Throwable e) {
                deadEmitters.add(emitter);
                log.debug("SSE emitter connection closed or unreachable. Removing emitter.");
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
