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
        String userPrompt = playerName + "があなたに近づいて話しかけてきました。挨拶してください。";

        return client.requestConversation(systemPrompt, userPrompt)
                .thenApply(response -> {
                    addHistory(npcUUID, "assistant", response);
                    memoryStore.addMemory(npcUUID, new MemoryEntry(
                            MemoryType.INTERACTION,
                            playerName + "と会話を開始",
                            3
                    ));
                    List<NPCAction> actions = new ArrayList<>();
                    actions.add(new SayAction(response, 16.0, brain.getData().getPersonalityType()));
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
                    addHistory(npcUUID, "assistant", response);
                    memoryStore.addMemory(npcUUID, new MemoryEntry(
                            MemoryType.INTERACTION,
                            playerName + ": " + message + " → 応答: " + truncate(response, 100),
                            2
                    ));
                    List<NPCAction> actions = new ArrayList<>();
                    actions.add(new SayAction(response, 16.0, brain.getData().getPersonalityType()));
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

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
