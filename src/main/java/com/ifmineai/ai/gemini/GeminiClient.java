package com.ifmineai.ai.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.Tool;
import com.ifmineai.config.AIConfig;

import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gemini API非同期ラッパー
 * CompletableFuture + Semaphore で同時リクエスト数を制御
 */
public class GeminiClient {

    private static final Logger LOGGER = Logger.getLogger(GeminiClient.class.getName());

    private final AIConfig config;
    private final Client client;
    private final ExecutorService executor;
    private final Semaphore semaphore;
    private final GeminiToolRegistry toolRegistry;

    public GeminiClient(AIConfig config) {
        this.config = config;
        this.client = Client.builder()
                .apiKey(config.getGeminiApiKey())
                .build();
        this.executor = Executors.newFixedThreadPool(config.getMaxConcurrentRequests());
        this.semaphore = new Semaphore(config.getMaxConcurrentRequests());
        this.toolRegistry = new GeminiToolRegistry();
    }

    /**
     * 行動決定リクエスト (Flash モデル - 高速)
     * Function Callingで行動ツールを呼び出す
     */
    public CompletableFuture<GenerateContentResponse> requestBehavior(String systemPrompt, String userPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                semaphore.acquire();
                try {
                    GenerateContentConfig configBuilder = GenerateContentConfig.builder()
                            .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                            .tools(List.of(toolRegistry.getBehaviorTools()))
                            .temperature(0.8f)
                            .maxOutputTokens(512)
                            .build();

                    return client.models.generateContent(
                            config.getGeminiFlashModel(),
                            userPrompt,
                            configBuilder
                    );
                } finally {
                    semaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Gemini行動リクエスト失敗", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * 会話リクエスト (Pro モデル - 高品質)
     */
    public CompletableFuture<String> requestConversation(String systemPrompt, String userPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                semaphore.acquire();
                try {
                    GenerateContentConfig configBuilder = GenerateContentConfig.builder()
                            .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                            .temperature(0.9f)
                            .maxOutputTokens(512)
                            .build();

                    GenerateContentResponse response = client.models.generateContent(
                            config.getGeminiProModel(),
                            userPrompt,
                            configBuilder
                    );

                    String text = response.text();
                    return text != null ? text : "...";
                } finally {
                    semaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Gemini会話リクエスト失敗", e);
                throw new CompletionException(e);
            }
        }, executor);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
