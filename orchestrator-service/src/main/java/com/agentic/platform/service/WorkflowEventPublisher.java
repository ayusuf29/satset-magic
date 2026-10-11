package com.agentic.platform.service;

import com.agentic.platform.domain.model.AgentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface WorkflowEventPublisher {
    SseEmitter registerGlobalEmitter();
    SseEmitter registerWorkflowEmitter(String workflowId);
    void publishEvent(AgentEvent event);
}
