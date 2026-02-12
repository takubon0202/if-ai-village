package com.ifmineai.ai.agent;

import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.action.NPCAction;
import com.ifmineai.ai.action.SayAction;
import com.ifmineai.ai.gemini.GeminiClient;
import com.ifmineai.ai.gemini.GeminiPromptBuilder;
import com.ifmineai.ai.memory.MemoryEntry;
import com.ifmineai.ai.memory.MemoryStore;
import com.ifmineai.ai.memory.MemoryType;
import com.ifmineai.config.AIConfig;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会話管理エージェント - プレイヤーとの会話履歴を管理しGemini経由で応答を生成
 * 1問1答を厳守: AI応答生成中は新しいリクエストを無視
 */
public class ConversationAgent implements BehaviorAgent {

    private final AIConfig config;
    private final MemoryStore memoryStore;
    // NPC UUID -> 会話履歴
    private final Map<UUID, List<ChatMessage>> conversationHistory = new ConcurrentHashMap<>();
    // NPC UUID -> AI応答生成中フラグ (1問1答ガード)
    private final Set<UUID> responding = ConcurrentHashMap.newKeySet();

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

        // 既に応答生成中なら空を返す
        if (!responding.add(npcUUID)) {
            return CompletableFuture.completedFuture(List.of());
        }

        conversationHistory.computeIfAbsent(npcUUID, k -> new ArrayList<>());

        String systemPrompt = promptBuilder.buildConversationSystemPrompt(brain, playerName);
        String userPrompt = playerName + "が話しかけてきました。短い挨拶を1つだけ返してください。";

        return client.requestConversation(systemPrompt, userPrompt)
                .thenApply(response -> {
                    responding.remove(npcUUID);
                    String cleaned = cleanResponse(response);
                    addHistory(npcUUID, "assistant", cleaned);
                    memoryStore.addMemory(npcUUID, new MemoryEntry(
                            MemoryType.INTERACTION,
                            playerName + "と会話を開始",
                            3
                    ));
                    List<NPCAction> actions = new ArrayList<>();
                    actions.add(new SayAction(cleaned, 16.0, brain.getData().getPersonalityType(), playerName));
                    return actions;
                })
                .exceptionally(ex -> {
                    responding.remove(npcUUID);
                    return List.of();
                });
    }

    /**
     * プレイヤーメッセージへの応答を生成 (1問1答)
     */
    public CompletableFuture<List<NPCAction>> handleMessage(
            NPCBrain brain, String playerName, String message,
            GeminiClient client, GeminiPromptBuilder promptBuilder) {

        UUID npcUUID = brain.getNpcUUID();

        // AI応答生成中なら新しいリクエストを無視 (1問1答)
        if (!responding.add(npcUUID)) {
            return CompletableFuture.completedFuture(List.of());
        }

        addHistory(npcUUID, "user", playerName + ": " + message);

        String systemPrompt = promptBuilder.buildConversationSystemPrompt(brain, playerName);
        List<ChatMessage> history = getHistory(npcUUID);
        String userPrompt = promptBuilder.buildConversationUserPrompt(history, message, playerName);

        return client.requestConversation(systemPrompt, userPrompt)
                .thenApply(response -> {
                    responding.remove(npcUUID);
                    String cleaned = cleanResponse(response);
                    addHistory(npcUUID, "assistant", cleaned);
                    memoryStore.addMemory(npcUUID, new MemoryEntry(
                            MemoryType.INTERACTION,
                            playerName + ": " + message + " → " + truncate(cleaned, 80),
                            2
                    ));
                    List<NPCAction> actions = new ArrayList<>();
                    actions.add(new SayAction(cleaned, 16.0, brain.getData().getPersonalityType(), playerName));
                    return actions;
                })
                .exceptionally(ex -> {
                    responding.remove(npcUUID);
                    return List.of();
                });
    }

    /**
     * 応答生成中かどうか
     */
    public boolean isResponding(UUID npcUUID) {
        return responding.contains(npcUUID);
    }

    public void endConversation(UUID npcUUID) {
        conversationHistory.remove(npcUUID);
        responding.remove(npcUUID);
    }

    private void addHistory(UUID npcUUID, String role, String content) {
        List<ChatMessage> history = conversationHistory.computeIfAbsent(npcUUID, k -> new ArrayList<>());
        history.add(new ChatMessage(role, content, System.currentTimeMillis()));
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
        String cleaned = response.replace("\n", " ").replace("\r", "").trim();
        // AI が「NPC名: 」のようなプレフィックスを付けることがある → 除去
        if (cleaned.contains(": ") && cleaned.indexOf(": ") < 20) {
            String prefix = cleaned.substring(0, cleaned.indexOf(": "));
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
