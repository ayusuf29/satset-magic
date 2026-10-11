package com.agentic.platform.service;

import com.agentic.platform.domain.model.WorkflowInstance;

public interface NotificationService {
    void sendHumanApprovalRequiredAlert(WorkflowInstance workflow, String reason);
    void sendEmergencyAlert(String subject, String details);
}
