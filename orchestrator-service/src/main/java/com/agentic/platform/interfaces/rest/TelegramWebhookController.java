package com.agentic.platform.interfaces.rest;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.service.WorkflowOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/webhook/telegram")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final WorkflowOrchestratorService orchestratorService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${agentic.notification.telegram.bot-token:}")
    private String botToken;

    @Value("${agentic.notification.telegram.chat-id:}")
    private String defaultChatId;

    @Value("${agentic.notification.telegram.authorized-chat-ids:}")
    private String authorizedChatIdsConfig;

    public TelegramWebhookController(WorkflowOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "TelegramWebhookController",
                "botConfigured", botToken != null && !botToken.isBlank()
        ));
    }

    @PostMapping
    public ResponseEntity<String> handleTelegramUpdate(@RequestBody Map<String, Object> update) {
        log.info("Received update from Telegram webhook: {}", update);

        if (update.containsKey("callback_query")) {
            return processCallbackQuery((Map<String, Object>) update.get("callback_query"));
        }

        return ResponseEntity.ok("NO_CALLBACK_QUERY");
    }

    private ResponseEntity<String> processCallbackQuery(Map<String, Object> callbackQuery) {
        try {
            String callbackQueryId = String.valueOf(callbackQuery.get("id"));
            Map<String, Object> from = (Map<String, Object>) callbackQuery.get("from");
            String userId = String.valueOf(from.get("id"));
            String username = from.get("username") != null ? String.valueOf(from.get("username")) : "User_" + userId;
            String data = String.valueOf(callbackQuery.get("data"));

            Map<String, Object> message = (Map<String, Object>) callbackQuery.get("message");
            Integer messageId = message != null ? (Integer) message.get("message_id") : null;
            Map<String, Object> chat = message != null ? (Map<String, Object>) message.get("chat") : null;
            String chatId = chat != null ? String.valueOf(chat.get("id")) : defaultChatId;

            // 1. RBAC & Whitelist Validation
            if (!isUserAuthorized(userId, chatId)) {
                log.warn("Unauthorized Telegram HITL attempt by user: {} (ID: {})", username, userId);
                answerCallback(callbackQueryId, "⛔ Anda tidak memiliki otoritas otorisasi pimpinan!");
                return ResponseEntity.ok("UNAUTHORIZED");
            }

            // 2. Parse callback action: format "action:workflowId"
            String[] parts = data.split(":");
            if (parts.length < 2) {
                log.warn("Invalid callback data format: {}", data);
                answerCallback(callbackQueryId, "Format data tidak valid.");
                return ResponseEntity.badRequest().body("INVALID_DATA");
            }

            String action = parts[0];
            String workflowId = parts[1];

            WorkflowInstance currentWf = orchestratorService.getWorkflowById(workflowId);
            boolean isMergeGate = currentWf.getCurrentState() == WorkflowState.PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE;

            if ("approve".equalsIgnoreCase(action) || "approve_merge".equalsIgnoreCase(action)) {
                String comment = isMergeGate
                        ? "Disetujui & di-merge ke main branch via Telegram oleh @" + username
                        : "Disetujui via Telegram oleh @" + username;

                orchestratorService.handleHumanDecision(workflowId, "APPROVE", comment, "TELEGRAM:@" + username);

                String cardText = isMergeGate
                        ? String.format("✅ <b>PR DISANGGUPKAN &amp; MERGED KE MAIN BRANCH</b>\n\n🔹 <b>Workflow ID:</b> <code>%s</code>\n🔹 <b>Task:</b> %s\n🔹 <b>Otorisator:</b> @%s\n🌿 Branch <code>main</code> telah diperbarui &amp; melanjutkan ke QA Sandbox.", escapeHtml(workflowId), escapeHtml(currentWf.getTaskName()), escapeHtml(username))
                        : String.format("✅ <b>KEPUTUSAN: DISETUJUI</b>\n\n🔹 <b>Workflow ID:</b> <code>%s</code>\n🔹 <b>Otorisator:</b> @%s\nWorkflow resmi dilanjutkan ke fase berikutnya.", escapeHtml(workflowId), escapeHtml(username));

                if (messageId != null && chatId != null) {
                    updateTelegramCard(chatId, messageId, cardText);
                }
                answerCallback(callbackQueryId, "✅ Otorisasi berhasil diproses!");
                log.info("Successfully processed Telegram APPROVAL for workflow {} by @{}", workflowId, username);

            } else if ("reject".equalsIgnoreCase(action)) {
                String comment = "Ditolak via Telegram oleh @" + username;
                orchestratorService.handleHumanDecision(workflowId, "REJECT", comment, "TELEGRAM:@" + username);

                String cardText = String.format("❌ <b>KEPUTUSAN: DITOLAK</b>\n\n🔹 <b>Workflow ID:</b> <code>%s</code>\n🔹 <b>Otorisator:</b> @%s\nTugas dikembalikan ke Developer Agent untuk diperbaiki.", escapeHtml(workflowId), escapeHtml(username));

                if (messageId != null && chatId != null) {
                    updateTelegramCard(chatId, messageId, cardText);
                }
                answerCallback(callbackQueryId, "❌ Penolakan berhasil dicatat!");
                log.info("Successfully processed Telegram REJECTION for workflow {} by @{}", workflowId, username);
            }

            return ResponseEntity.ok("PROCESSED");

        } catch (Exception ex) {
            log.error("Error handling Telegram callback: {}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body("ERROR: " + ex.getMessage());
        }
    }

    private boolean isUserAuthorized(String userId, String chatId) {
        if (authorizedChatIdsConfig == null || authorizedChatIdsConfig.isBlank()) {
            return defaultChatId == null || defaultChatId.isBlank() || defaultChatId.equals(chatId) || defaultChatId.equals(userId);
        }
        Set<String> authorized = Set.of(authorizedChatIdsConfig.split(","));
        return authorized.contains(userId) || authorized.contains(chatId);
    }

    private void answerCallback(String callbackQueryId, String text) {
        if (botToken == null || botToken.isBlank()) return;
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/answerCallbackQuery";
            Map<String, Object> body = Map.of("callback_query_id", callbackQueryId, "text", text);
            postJson(url, body);
        } catch (Exception ex) {
            log.warn("Failed to answer Telegram callback query: {}", ex.getMessage());
        }
    }

    private void updateTelegramCard(String chatId, Integer messageId, String newText) {
        if (botToken == null || botToken.isBlank()) return;
        String url = "https://api.telegram.org/bot" + botToken + "/editMessageText";
        try {
            Map<String, Object> body = Map.of(
                    "chat_id", chatId,
                    "message_id", messageId,
                    "text", newText,
                    "parse_mode", "HTML"
            );
            postJson(url, body);
        } catch (Exception ex) {
            log.warn("Failed to edit Telegram message text in HTML, retrying plain text: {}", ex.getMessage());
            try {
                String plain = newText.replaceAll("<[^>]*>", "").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
                Map<String, Object> body = Map.of(
                        "chat_id", chatId,
                        "message_id", messageId,
                        "text", plain
                );
                postJson(url, body);
            } catch (Exception e2) {
                log.warn("Failed to edit Telegram message text in plain text: {}", e2.getMessage());
            }
        }
    }

    private void postJson(String url, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        restTemplate.postForEntity(url, entity, String.class);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
