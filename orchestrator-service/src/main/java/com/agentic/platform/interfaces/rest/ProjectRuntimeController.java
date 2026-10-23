package com.agentic.platform.interfaces.rest;

import com.agentic.platform.service.phase.PortRegistryService;
import com.agentic.platform.service.phase.ProjectRuntimeDeploymentAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ProjectRuntimeController {

    private final ProjectRuntimeDeploymentAgent runtimeAgent;
    private final PortRegistryService portRegistryService;

    public ProjectRuntimeController(ProjectRuntimeDeploymentAgent runtimeAgent, PortRegistryService portRegistryService) {
        this.runtimeAgent = runtimeAgent;
        this.portRegistryService = portRegistryService;
    }

    @GetMapping("/projects/{projectId}/runtime/status")
    public ResponseEntity<ProjectRuntimeDeploymentAgent.ProjectRuntimeStatus> getStatus(@PathVariable String projectId) {
        return ResponseEntity.ok(runtimeAgent.getProjectRuntimeStatus(projectId));
    }

    @PostMapping("/projects/{projectId}/runtime/start")
    public ResponseEntity<ProjectRuntimeDeploymentAgent.ProjectRuntimeStatus> startStack(@PathVariable String projectId) {
        return ResponseEntity.ok(runtimeAgent.startProjectStack(projectId));
    }

    @PostMapping("/projects/{projectId}/runtime/stop")
    public ResponseEntity<ProjectRuntimeDeploymentAgent.ProjectRuntimeStatus> stopStack(@PathVariable String projectId) {
        return ResponseEntity.ok(runtimeAgent.stopProjectStack(projectId));
    }

    @GetMapping("/runtime/ports")
    public ResponseEntity<Map<String, PortRegistryService.ProjectPortAllocation>> getAllPortAllocations() {
        return ResponseEntity.ok(portRegistryService.getAllAllocations());
    }
}
