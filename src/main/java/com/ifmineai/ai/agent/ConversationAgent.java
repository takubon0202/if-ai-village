package com.ifmineai.ai.agent;

import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.action.NPCAction;
import com.ifmineai.ai.action.SayAction;
import com.ifmineai.ai.gemini.GeminiClient;
import com.ifmineai.ai.gemini.GeminiPromptBuilder;
import com.ifmineai.ai.memory.MemoryEntry;
import com.ifmineai.ai.memory.MemoryStore;
import com.ifmineai.ai.memory.MemoryType;
import com.ifmineai.ai.personality.PersonalityProfile;
import com.ifmineai.config.AIConfig;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会話管理エージェント - プレイヤーとの会話履歴を管理しGemini経由で応答を生成
 */
public class ConversationAgent implements BehaviorAgent {

    private final AIConfig config;
    private final MemoryStore memoryStore;
    // NPC UUID -> 会話履歴
    private final Map<UUID, List<ChatMessage>> conversationHistory = new ConcurrentHashMap<>();

    public record ChatMessage(String role, String content, long timestamp) {}

    public ConversationAgent(AIConfig config, MemoryStore memoryStore) {
        this.config = config;
        this.memoryStore = memoryStore;
    }

    @Override
    public String name() {
        return "ConversationAgent";
    }

    /**
     * 会話開始 - 挨拶を生成
     */
    public CompletableFuture<List<NPCAction>> startConversation(
            NPCBrain brain, String playerName,
            GeminiClient client, GeminiPromptBuilder promptBuilder) {

        UUID npcUUID = brain.getNpcUUID();
        conversationHistory.computeIfAbsent(npcUUID, k -> new ArrayList<>());

        String systemPrompt = promptBuilder.buildConversationSystemPrompt(brain, playerName);
        String userPrompt = playerName + "が話しかけてきました。短い挨拶を1つだけ返してください（30文字以内）。";

        return client.requestConversation(systemPrompt, userPrompt)
                .thenApply(response -> {
                    String cleaned = cleanResponse(response);
                    addHistory(npcUUID, "assistant", cleaned);
                    memoryStore.addMemory(npcUUID, new MemoryEntry(
                            MemoryType.INTERACTION,
                            playerName + "と会話を開始",
                            3
                    ));
                    List<NPCAction> actions = new ArrayList<>();
                    // 会話相手にだけ送信 (プレイヤーの方を向いて話す)
                    actions.add(new SayAction(cleaned, 16.0, brain.getData().getPersonalityType(), playerName));
                    return actions;
                });
    }

    /**
     * プレイヤーメッセージへの応答を生成
     */
    public CompletableFuture<List<NPCAction>> handleMessage(
            NPCBrain brain, String playerName, String message,
            GeminiClient client, GeminiPromptBuilder promptBuilder) {

        UUID npcUUID = brain.getNpcUUID();
        addHistory(npcUUID, "user", playerName + ": " + message);

        String systemPrompt = promptBuilder.buildConversationSystemPrompt(brain, playerName);
        List<ChatMessage> history = getHistory(npcUUID);
        String userPrompt = promptBuilder.buildConversationUserPrompt(history, message, playerName);

        return client.requestConversation(systemPrompt, userPrompt)
                .thenApply(response -> {
                    String cleaned = cleanResponse(response);
                    addHistory(npcUUID, "assistant", cleaned);
                    memoryStore.addMemory(npcUUID, new MemoryEntry(
                            MemoryType.INTERACTION,
                            playerName + ": " + message + " → " + truncate(cleaned, 80),
                            2
                    ));
                    List<NPCAction> actions = new ArrayList<>();
                    // 会話相手にだけ送信 (プレイヤーの方を向いて話す)
                    actions.add(new SayAction(cleaned, 16.0, brain.getData().getPersonalityType(), playerName));
                    return actions;
                });
    }

    public void endConversation(UUID npcUUID) {
        conversationHistory.remove(npcUUID);
    }

    private void addHistory(UUID npcUUID, String role, String content) {
        List<ChatMessage> history = conversationHistory.computeIfAbsent(npcUUID, k -> new ArrayList<>());
        history.add(new ChatMessage(role, content, System.currentTimeMillis()));
        // 履歴上限
        while (history.size() > config.getMaxConversationHistory()) {
            history.remove(0);
        }
    }

    public List<ChatMessage> getHistory(UUID npcUUID) {
        return conversationHistory.getOrDefault(npcUUID, List.of());
    }

    /**
     * AI応答のクリーンアップ: 改行除去、長さ制限、余計なプレフィックス除去
     */
    private String cleanResponse(String response) {
        if (response == null || response.isBlank()) return "...";
        // 改行を空白に変換
        String cleaned = response.replace("\n", " ").replace("\r", "").trim();
        // AI が「NPC名: 」のようなプレフィックスを付けることがある → 除去
        if (cleaned.contains(": ") && cleaned.indexOf(": ") < 20) {
            String prefix = cleaned.substring(0, cleaned.indexOf(": "));
            // プレフィックスが短い名前的な文字列なら除去
            if (!prefix.contains(" ") && prefix.length() <= 15) {
                cleaned = cleaned.substring(cleaned.indexOf(": ") + 2).trim();
            }
        }
        // 200文字制限
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 197) + "...";
        }
        return cleaned.isEmpty() ? "..." : cleaned;
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
