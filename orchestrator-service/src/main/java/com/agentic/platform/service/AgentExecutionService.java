package com.agentic.platform.service;

import com.agentic.platform.domain.model.WorkflowInstance;

public interface AgentExecutionService {
    String executePhaseAgent(WorkflowInstance workflow, String promptInstruction);
}
