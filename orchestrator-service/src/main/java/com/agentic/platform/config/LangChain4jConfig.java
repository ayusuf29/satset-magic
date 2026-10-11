package com.agentic.platform.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jConfig.class);

    @Value("${agentic.model.api-key:}")
    private String geminiApiKey;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            log.info("Configuring LangChain4j Google AI Gemini ChatLanguageModel...");
            return GoogleAiGeminiChatModel.builder()
                    .apiKey(geminiApiKey)
                    .modelName("gemini-1.5-flash")
                    .temperature(0.2)
                    .maxOutputTokens(4096)
                    .build();
        } else {
            log.warn("GEMINI_API_KEY is not set. Falling back to local Mock ChatLanguageModel.");
            return (messages) -> {
                String promptText = messages.toString();
                return dev.langchain4j.model.output.Response.from(
                    dev.langchain4j.data.message.AiMessage.from(
                        "Autonomous Agent Evaluation Completed: Analysis verified against architecture guidelines. [MOCK_OUTPUT]"
                    )
                );
            };
        }
    }
}
