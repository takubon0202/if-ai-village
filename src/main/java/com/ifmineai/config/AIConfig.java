package com.ifmineai.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class AIConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    // AI全般
    private boolean aiEnabled;
    private String geminiApiKey;
    private String geminiFlashModel;
    private String geminiProModel;
    private int maxConcurrentRequests;

    // 行動
    private int decisionIntervalTicks;
    private int pathfinderRetryTicks;
    private double awarenessRadius;
    private double homeRadius;

    // 会話
    private int conversationTimeoutSeconds;
    private int maxConversationHistory;

    // 記憶
    private int shortTermMemorySize;
    private int longTermMemorySize;

    public AIConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        aiEnabled = config.getBoolean("ai.enabled", false);
        geminiApiKey = config.getString("ai.gemini-api-key", "").trim();
        geminiFlashModel = config.getString("ai.gemini-flash-model", "gemini-3-flash-preview");
        geminiProModel = config.getString("ai.gemini-pro-model", "gemini-3-pro-preview");
        maxConcurrentRequests = config.getInt("ai.max-concurrent-requests", 3);

        decisionIntervalTicks = config.getInt("ai.behavior.decision-interval-ticks", 30);
        pathfinderRetryTicks = config.getInt("ai.behavior.pathfinder-retry-ticks", 20);
        awarenessRadius = config.getDouble("ai.behavior.awareness-radius", 16.0);
        homeRadius = config.getDouble("ai.behavior.home-radius", 32.0);

        conversationTimeoutSeconds = config.getInt("ai.conversation.timeout-seconds", 60);
        maxConversationHistory = config.getInt("ai.conversation.max-history", 20);

        shortTermMemorySize = config.getInt("ai.memory.short-term-size", 50);
        longTermMemorySize = config.getInt("ai.memory.long-term-size", 200);
    }

    /**
     * APIキーの形式を簡易チェック
     */
    public static boolean validateApiKeyFormat(String key) {
        if (key == null || key.isEmpty()) return false;
        // Google AI Studio APIキーは "AIza" で始まり39文字
        return key.startsWith("AIza") && key.length() >= 30;
    }

    /**
     * 起動時のステータスをログに出力
     */
    public void logStartupStatus(Logger logger) {
        logger.info("========================================");
        logger.info("  IF MineAI - AI NPC 設定状況");
        logger.info("========================================");

        if (!aiEnabled) {
            logger.info("  AI: 無効 (config.yml の ai.enabled を true に)");
        } else if (geminiApiKey.isEmpty()) {
            logger.warning("  AI: 有効だがAPIキー未設定!");
            logger.warning("  config.yml の ai.gemini-api-key に");
            logger.warning("  Google AI Studio APIキーを入力してください");
            logger.warning("  取得先: https://aistudio.google.com/apikey");
        } else if (!validateApiKeyFormat(geminiApiKey)) {
            logger.warning("  AI: APIキーの形式が不正です");
            logger.warning("  「AIza」で始まる正しいキーを設定してください");
        } else {
            logger.info("  AI: 有効");
            logger.info("  モデル: " + geminiFlashModel);
            logger.info("  同時リクエスト上限: " + maxConcurrentRequests);
        }

        logger.info("========================================");
    }

    public boolean isAiEnabled() {
        return aiEnabled && isApiKeyValid();
    }

    public boolean isAiConfigured() {
        return isApiKeyValid();
    }

    public boolean isApiKeyValid() {
        return validateApiKeyFormat(geminiApiKey);
    }

    public boolean isEnabledFlag() {
        return aiEnabled;
    }

    public String getGeminiApiKey() { return geminiApiKey; }
    public String getGeminiFlashModel() { return geminiFlashModel; }
    public String getGeminiProModel() { return geminiProModel; }
    public int getMaxConcurrentRequests() { return maxConcurrentRequests; }
    public int getDecisionIntervalTicks() { return decisionIntervalTicks; }
    public int getPathfinderRetryTicks() { return pathfinderRetryTicks; }
    public double getAwarenessRadius() { return awarenessRadius; }
    public double getHomeRadius() { return homeRadius; }
    public int getConversationTimeoutSeconds() { return conversationTimeoutSeconds; }
    public int getMaxConversationHistory() { return maxConversationHistory; }
    public int getShortTermMemorySize() { return shortTermMemorySize; }
    public int getLongTermMemorySize() { return longTermMemorySize; }
}
