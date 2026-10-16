package com.agentic.platform.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.Response;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jConfig.class);

    @Value("${agentic.model.api-key:}")
    private String geminiApiKey;

    @Value("${agentic.model.model-name:gemini-3.5-flash}")
    private String modelName;

    @Value("${agentic.model.fallback-model-name:gemini-2.5-flash}")
    private String fallbackModelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            log.info("Configuring LangChain4j Google AI Gemini with primary model: {}, fallback model: {}",
                    modelName, fallbackModelName);

            ChatLanguageModel primaryModel = fixModelTimeout(GoogleAiGeminiChatModel.builder()
                    .apiKey(geminiApiKey)
                    .modelName(modelName)
                    .temperature(0.2)
                    .maxOutputTokens(4096)
                    .build(), Duration.ofSeconds(60));

            ChatLanguageModel fallbackModel = fixModelTimeout(GoogleAiGeminiChatModel.builder()
                    .apiKey(geminiApiKey)
                    .modelName(fallbackModelName)
                    .temperature(0.2)
                    .maxOutputTokens(4096)
                    .build(), Duration.ofSeconds(60));

            return new ChatLanguageModel() {
                @Override
                public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                        java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
                    try {
                        log.info("Attempting LLM inference with Primary Model: {}", modelName);
                        Response<dev.langchain4j.data.message.AiMessage> res = primaryModel.generate(messages);
                        log.info("Primary Model ({}) responded successfully!", modelName);
                        return res;
                    } catch (Exception ex) {
                        log.warn("Primary model ({}) failed or returned error (e.g. 503/timeout): {}. Automatically switching to fallback model ({}) and retrying...",
                                modelName, ex.getMessage(), fallbackModelName);
                        Response<dev.langchain4j.data.message.AiMessage> fallbackRes = fallbackModel.generate(messages);
                        log.info("Fallback Model ({}) responded successfully!", fallbackModelName);
                        return fallbackRes;
                    }
                }
            };
        } else {
            log.warn("GEMINI_API_KEY is not set. Falling back to local Mock ChatLanguageModel.");
            return (messages) -> dev.langchain4j.model.output.Response.from(
                    dev.langchain4j.data.message.AiMessage.from(
                            "Autonomous Agent Evaluation Completed: Analysis verified against architecture guidelines. [MOCK_OUTPUT]"));
        }
    }

    /**
     * Fixes LangChain4j 0.35.0 internal OkHttp readTimeout bug where it defaults to 10 seconds.
     * Overrides readTimeout, connectTimeout, writeTimeout, and callTimeout to 60 seconds.
     */
    private ChatLanguageModel fixModelTimeout(ChatLanguageModel model, Duration timeout) {
        try {
            Field gsField = GoogleAiGeminiChatModel.class.getDeclaredField("geminiService");
            gsField.setAccessible(true);
            Object proxy = gsField.get(model);
            Object ih = Proxy.getInvocationHandler(proxy);

            Field retrofitField = ih.getClass().getDeclaredField("this$0");
            retrofitField.setAccessible(true);
            Retrofit retrofit = (Retrofit) retrofitField.get(ih);

            Field cfField = Retrofit.class.getDeclaredField("callFactory");
            cfField.setAccessible(true);
            Object cf = cfField.get(retrofit);

            if (cf instanceof OkHttpClient oldClient) {
                OkHttpClient newClient = oldClient.newBuilder()
                        .connectTimeout(timeout)
                        .readTimeout(timeout)
                        .writeTimeout(timeout)
                        .callTimeout(timeout)
                        .build();
                cfField.set(retrofit, newClient);
                log.info("Successfully patched Gemini OkHttpClient timeout to {}s", timeout.toSeconds());
            }
        } catch (Exception ex) {
            log.warn("Notice: could not apply OkHttpClient timeout patch: {}", ex.getMessage());
        }
        return model;
    }
}

