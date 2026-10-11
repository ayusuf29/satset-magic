package com.agentic.platform.service;

import com.agentic.platform.domain.model.WorkflowInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${agentic.notification.telegram.bot-token:}")
    private String botToken;

    @Value("${agentic.notification.telegram.chat-id:}")
    private String chatId;

    @Value("${agentic.dashboard.url:http://localhost:3000}")
    private String dashboardUrl;

    @Override
    public void sendHumanApprovalRequiredAlert(WorkflowInstance workflow, String reason) {
        log.info("HUMAN-IN-THE-LOOP REQUIRED for workflow: {} | State: {} | Reason: {}",
                workflow.getId(), workflow.getCurrentState(), reason);

        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.info("Telegram notification skipped: bot-token or chat-id is not configured.");
            return;
        }

        try {
            String telegramUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            String messageText = String.format(
                    "⚠️ *HUMAN APPROVAL REQUIRED*\n\n" +
                    "🔹 *Workflow ID:* `%s`\n" +
                    "🔹 *Task Name:* *%s*\n" +
                    "🔹 *State:* `%s`\n" +
                    "🔹 *Reason:* %s\n" +
                    "🔹 *Review Loop:* %d / 3\n\n" +
                    "Pilih keputusan di bawah atau tinjau langsung di Dashboard:",
                    workflow.getId(),
                    workflow.getTaskName(),
                    workflow.getCurrentState(),
                    reason,
                    workflow.getReviewLoopCount()
            );

            Map<String, Object> approveButton = Map.of(
                    "text", "✅ APPROVE",
                    "callback_data", "approve:" + workflow.getId()
            );
            Map<String, Object> rejectButton = Map.of(
                    "text", "❌ REJECT",
                    "callback_data", "reject:" + workflow.getId()
            );
            Map<String, Object> viewDetailButton = Map.of(
                    "text", "📝 VIEW DETAIL",
                    "url", dashboardUrl + "/workflow/" + workflow.getId()
            );

            List<List<Map<String, Object>>> keyboard = List.of(
                    List.of(approveButton, rejectButton),
                    List.of(viewDetailButton)
            );

            Map<String, Object> requestPayload = Map.of(
                    "chat_id", chatId,
                    "text", messageText,
                    "parse_mode", "Markdown",
                    "reply_markup", Map.of("inline_keyboard", keyboard)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);

            restTemplate.postForEntity(telegramUrl, entity, String.class);
            log.info("Telegram approval alert successfully dispatched to chat_id: {}", chatId);
        } catch (Exception ex) {
            log.warn("Failed to dispatch Telegram message: {}", ex.getMessage());
        }
    }

    @Override
    public void sendEmergencyAlert(String subject, String details) {
        log.error("EMERGENCY ALERT: [{}] - {}", subject, details);
    }
}
