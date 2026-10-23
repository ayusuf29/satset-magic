package com.agentic.platform.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
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
import java.util.ArrayList;
import java.util.List;

@Configuration
public class LangChain4jConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jConfig.class);

    @Value("${agentic.model.api-key:}")
    private String geminiApiKey;

    @Value("${agentic.model.model-name:gemini-3.5-flash}")
    private String primaryModelName;

    @Value("${agentic.model.fallback-model-name:gemini-2.5-flash}")
    private String secondaryModelName;

    @Value("${agentic.model.groq-api-key:${GROQ_API_KEY:}}")
    private String groqApiKey;

    @Value("${agentic.model.openrouter-api-key:${OPENROUTER_API_KEY:}}")
    private String openrouterApiKey;

    @Value("${agentic.model.deepseek-api-key:${DEEPSEEK_API_KEY:}}")
    private String deepseekApiKey;

    @Value("${agentic.model.local-base-url:${LOCAL_LLM_BASE_URL:}}")
    private String localLlmBaseUrl;

    @Value("${agentic.model.local-model-name:${LOCAL_LLM_MODEL_NAME:qwen2.5-coder-14b-instruct}}")
    private String localModelName;

    public record NamedModel(String displayName, ChatLanguageModel model) {}

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        List<NamedModel> chain = new ArrayList<>();

        // Tier 1 - 6: Google Gemini / Gemma Models via GEMINI_API_KEY
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            List<String> geminiModels = List.of(
                    primaryModelName != null && !primaryModelName.isBlank() ? primaryModelName : "gemini-3.5-flash",
                    "gemini-3.6-flash",
                    secondaryModelName != null && !secondaryModelName.isBlank() ? secondaryModelName : "gemini-2.5-flash",
                    "gemini-3.5-flash-lite",
                    "gemini-3-flash-preview",
                    "gemma-4-26b-a4b-it"
            );

            for (String m : geminiModels) {
                try {
                    ChatLanguageModel geminiModel = fixModelTimeout(GoogleAiGeminiChatModel.builder()
                            .apiKey(geminiApiKey)
                            .modelName(m)
                            .temperature(0.2)
                            .maxOutputTokens(4096)
                            .build(), Duration.ofSeconds(60));
                    chain.add(new NamedModel("Gemini [" + m + "]", geminiModel));
                } catch (Exception e) {
                    log.warn("Could not register Gemini model {}: {}", m, e.getMessage());
                }
            }
        }

        // Tier 7: Groq Free Cloud (Llama 3.3 70B)
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            try {
                ChatLanguageModel groqModel = OpenAiChatModel.builder()
                        .baseUrl("https://api.groq.com/openai/v1")
                        .apiKey(groqApiKey)
                        .modelName("llama-3.3-70b-versatile")
                        .temperature(0.2)
                        .maxTokens(4096)
                        .timeout(Duration.ofSeconds(60))
                        .build();
                chain.add(new NamedModel("Groq [llama-3.3-70b-versatile]", groqModel));
            } catch (Exception e) {
                log.warn("Could not register Groq model: {}", e.getMessage());
            }
        }

        // Tier 8: OpenRouter Free Models (DeepSeek R1 / Llama / Qwen)
        if (openrouterApiKey != null && !openrouterApiKey.isBlank()) {
            try {
                ChatLanguageModel openRouterModel = OpenAiChatModel.builder()
                        .baseUrl("https://openrouter.ai/api/v1")
                        .apiKey(openrouterApiKey)
                        .modelName("deepseek/deepseek-r1:free")
                        .temperature(0.2)
                        .maxTokens(4096)
                        .timeout(Duration.ofSeconds(60))
                        .build();
                chain.add(new NamedModel("OpenRouter [deepseek-r1:free]", openRouterModel));
            } catch (Exception e) {
                log.warn("Could not register OpenRouter model: {}", e.getMessage());
            }
        }

        // Tier 9: DeepSeek Direct API
        if (deepseekApiKey != null && !deepseekApiKey.isBlank()) {
            try {
                ChatLanguageModel deepseekModel = OpenAiChatModel.builder()
                        .baseUrl("https://api.deepseek.com/v1")
                        .apiKey(deepseekApiKey)
                        .modelName("deepseek-chat")
                        .temperature(0.2)
                        .maxTokens(4096)
                        .timeout(Duration.ofSeconds(60))
                        .build();
                chain.add(new NamedModel("DeepSeek [deepseek-chat]", deepseekModel));
            } catch (Exception e) {
                log.warn("Could not register DeepSeek model: {}", e.getMessage());
            }
        }

        // Tier 10: Local Jan.ai / Ollama / LM Studio (OpenAI-Compatible)
        if (localLlmBaseUrl != null && !localLlmBaseUrl.isBlank() && !localLlmBaseUrl.contains("not-needed")) {
            try {
                ChatLanguageModel localModel = OpenAiChatModel.builder()
                        .baseUrl(localLlmBaseUrl)
                        .apiKey("not-needed")
                        .modelName(localModelName != null && !localModelName.isBlank() ? localModelName : "qwen2.5-coder-14b-instruct")
                        .temperature(0.2)
                        .maxTokens(4096)
                        .timeout(Duration.ofSeconds(60))
                        .build();
                chain.add(new NamedModel("Local LLM [" + localModelName + " @ " + localLlmBaseUrl + "]", localModel));
            } catch (Exception e) {
                log.warn("Could not register Local LLM model: {}", e.getMessage());
            }
        }

        if (chain.isEmpty()) {
            log.warn("No LLM API keys configured. Using rich deterministic domain fallback for all phases.");
            return messages -> {
                throw new IllegalStateException("No active LLM providers configured. Triggering rich deterministic domain fallback.");
            };
        }

        log.info("Registered {} sequential LLM fallback candidate models: {}",
                chain.size(), chain.stream().map(NamedModel::displayName).toList());

        return new ChatLanguageModel() {
            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                for (int i = 0; i < chain.size(); i++) {
                    NamedModel entry = chain.get(i);
                    try {
                        log.info("[SEQUENTIAL LLM CHAIN Step {}/{}] Attempting inference with: {}",
                                i + 1, chain.size(), entry.displayName());
                        long start = System.currentTimeMillis();
                        Response<AiMessage> response = entry.model().generate(messages);
                        long dur = System.currentTimeMillis() - start;

                        if (response != null && response.content() != null && !response.content().text().isBlank()) {
                            log.info("[SEQUENTIAL LLM CHAIN Step {}/{}] {} responded successfully in {}ms!",
                                    i + 1, chain.size(), entry.displayName(), dur);
                            return response;
                        }
                    } catch (Exception ex) {
                        log.warn("[SEQUENTIAL LLM CHAIN Step {}/{}] {} failed or returned error ({}). Stepping sequentially to next candidate...",
                                i + 1, chain.size(), entry.displayName(), ex.getMessage());
                    }
                }
                throw new IllegalStateException("All " + chain.size() + " sequential LLM candidate models failed or unavailable. Falling back to deterministic domain template.");
            }
        };
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
