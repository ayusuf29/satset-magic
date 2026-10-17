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

            boolean isMergeGate = workflow.getCurrentState() == com.agentic.platform.domain.model.WorkflowState.PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE;

            String escapedWorkflowId = escapeHtml(workflow.getId());
            String escapedTaskName = escapeHtml(workflow.getTaskName());
            String escapedState = escapeHtml(workflow.getCurrentState() != null ? workflow.getCurrentState().name() : "");
            String escapedReason = escapeHtml(reason);

            String messageText;
            if (isMergeGate) {
                messageText = String.format(
                        "🚀 <b>CODE REVIEW APPROVAL &amp; PROTECTED MERGE</b>\n\n" +
                        "🔹 <b>Workflow ID:</b> <code>%s</code>\n" +
                        "🔹 <b>Task Name:</b> <b>%s</b>\n" +
                        "🔹 <b>Current Phase:</b> <code>%s</code>\n" +
                        "🌿 <b>Target:</b> <code>main</code> branch (PROTECTED)\n" +
                        "🏛️ <b>Council Status:</b> 3/3 UNANIMOUS PASS (Coverage: %.1f%%)\n" +
                        "🔹 <b>Reason:</b> %s\n\n" +
                        "🔔 <i>Agent is waiting for your action in the dashboard.</i>\n\n" +
                        "⚠️ <b>Aturan Keamanan:</b> Agen dilarang merge langsung ke branch <code>main</code>. Otorisasi pimpinan teknis (manusia) diperlukan:",
                        escapedWorkflowId,
                        escapedTaskName,
                        escapedState,
                        workflow.getCoveragePercentage(),
                        escapedReason
                );
            } else {
                messageText = String.format(
                        "⚠️ <b>HUMAN APPROVAL REQUIRED</b>\n\n" +
                        "🔹 <b>Workflow ID:</b> <code>%s</code>\n" +
                        "🔹 <b>Task Name:</b> <b>%s</b>\n" +
                        "🔹 <b>Current Phase:</b> <code>%s</code>\n" +
                        "🔹 <b>Reason:</b> %s\n" +
                        "🔹 <b>Review Loop:</b> %d / 3\n\n" +
                        "🔔 <i>Agent is waiting for your action in the dashboard.</i>",
                        escapedWorkflowId,
                        escapedTaskName,
                        escapedState,
                        escapedReason,
                        workflow.getReviewLoopCount()
                );
            }

            Map<String, Object> approveButton = Map.of(
                    "text", isMergeGate ? "✅ APPROVE & MERGE TO MAIN" : "✅ APPROVE",
                    "callback_data", "approve:" + workflow.getId()
            );
            Map<String, Object> rejectButton = Map.of(
                    "text", isMergeGate ? "❌ REJECT WITH NOTE" : "❌ REJECT",
                    "callback_data", "reject:" + workflow.getId()
            );

            List<List<Map<String, Object>>> keyboard = List.of(
                    List.of(approveButton, rejectButton)
            );

            Map<String, Object> requestPayload = Map.of(
                    "chat_id", chatId,
                    "text", messageText,
                    "parse_mode", "HTML",
                    "reply_markup", Map.of("inline_keyboard", keyboard)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);

            try {
                restTemplate.postForEntity(telegramUrl, entity, String.class);
                log.info("Telegram approval alert successfully dispatched to chat_id: {}", chatId);
            } catch (Exception ex) {
                log.warn("HTML Telegram dispatch failed ({}), attempting plain text fallback...", ex.getMessage());
                // Fallback to plain text without parse_mode to guarantee delivery
                String plainText = stripHtml(messageText);
                Map<String, Object> fallbackPayload = Map.of(
                        "chat_id", chatId,
                        "text", plainText,
                        "reply_markup", Map.of("inline_keyboard", keyboard)
                );
                HttpEntity<Map<String, Object>> fallbackEntity = new HttpEntity<>(fallbackPayload, headers);
                restTemplate.postForEntity(telegramUrl, fallbackEntity, String.class);
                log.info("Telegram approval alert successfully dispatched via plain text fallback to chat_id: {}", chatId);
            }
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("chat not found")) {
                log.warn("Telegram Notice: Bot (@SatsetMagicBot) belum di-start oleh chat_id {}. Buka Telegram, cari @SatsetMagicBot lalu klik /start agar notifikasi terkirim.", chatId);
            } else {
                log.warn("Failed to dispatch Telegram message: {}", ex.getMessage());
            }
        }
    }

    @Override
    public void sendEmergencyAlert(String subject, String details) {
        log.error("EMERGENCY ALERT: [{}] - {}", subject, details);
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            return;
        }
        try {
            String telegramUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            String msg = String.format("🚨 <b>EMERGENCY ALERT</b>\n\n<b>Subject:</b> %s\n<b>Details:</b> %s",
                    escapeHtml(subject), escapeHtml(details));
            Map<String, Object> payload = Map.of(
                    "chat_id", chatId,
                    "text", msg,
                    "parse_mode", "HTML"
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(telegramUrl, new HttpEntity<>(payload, headers), String.class);
        } catch (Exception e) {
            log.warn("Failed to send emergency Telegram alert: {}", e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
