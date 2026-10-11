package com.agentic.platform.interfaces.rest;

import com.agentic.platform.service.WorkflowEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowStreamController {

    private final WorkflowEventPublisher eventPublisher;

    public WorkflowStreamController(WorkflowEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGlobalEvents() {
        return eventPublisher.registerGlobalEmitter();
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamWorkflowEvents(@PathVariable String id) {
        return eventPublisher.registerWorkflowEmitter(id);
    }
}
