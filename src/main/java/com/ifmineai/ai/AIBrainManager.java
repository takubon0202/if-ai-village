package com.ifmineai.ai;

import com.ifmineai.CounselorData;
import com.ifmineai.IFMineAIPlugin;
import com.ifmineai.ai.agent.AwarenessAgent;
import com.ifmineai.ai.agent.ConversationAgent;
import com.ifmineai.ai.agent.MemoryAgent;
import com.ifmineai.ai.agent.MovementAgent;
import com.ifmineai.ai.gemini.GeminiClient;
import com.ifmineai.ai.gemini.GeminiPromptBuilder;
import com.ifmineai.ai.gemini.GeminiResponseParser;
import com.ifmineai.ai.action.NPCAction;
import com.ifmineai.ai.memory.MemoryStore;
import com.ifmineai.ai.personality.PersonalityLoader;
import com.ifmineai.ai.personality.PersonalityProfile;
import com.ifmineai.config.AIConfig;

import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class AIBrainManager {

    private final IFMineAIPlugin plugin;
    private final AIConfig aiConfig;
    private final Map<UUID, NPCBrain> brains = new ConcurrentHashMap<>();
    private final List<UUID> brainOrder = new ArrayList<>(); // ラウンドロビン用
    private int roundRobinIndex = 0;
    private BukkitTask tickTask;

    // 共有コンポーネント
    private GeminiClient geminiClient;
    private GeminiPromptBuilder promptBuilder;
    private GeminiResponseParser responseParser;
    private AwarenessAgent awarenessAgent;
    private MovementAgent movementAgent;
    private ConversationAgent conversationAgent;
    private MemoryAgent memoryAgent;
    private MemoryStore memoryStore;
    private PersonalityLoader personalityLoader;

    private boolean enabled;

    public AIBrainManager(IFMineAIPlugin plugin, AIConfig aiConfig) {
        this.plugin = plugin;
        this.aiConfig = aiConfig;
        this.enabled = aiConfig.isAiEnabled();
    }

    public void initialize() {
        if (!aiConfig.isAiEnabled()) {
            plugin.getLogger().info("AI NPC: 無効 (APIキー未設定またはai.enabled=false)");
            enabled = false;
            return;
        }

        // コンポーネント初期化
        personalityLoader = new PersonalityLoader(plugin);
        memoryStore = new MemoryStore(plugin);
        geminiClient = new GeminiClient(aiConfig);
        promptBuilder = new GeminiPromptBuilder(personalityLoader);
        responseParser = new GeminiResponseParser(memoryStore);
        awarenessAgent = new AwarenessAgent(aiConfig);
        movementAgent = new MovementAgent(aiConfig);
        conversationAgent = new ConversationAgent(aiConfig, memoryStore);
        memoryAgent = new MemoryAgent(aiConfig, memoryStore);

        // tickループ開始
        tickTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::tickAll, 1L, 1L
        );

        enabled = true;
        plugin.getLogger().info("AI NPC: 有効 (モデル: " + aiConfig.getGeminiFlashModel() + ")");
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (NPCBrain brain : brains.values()) {
            brain.clearActions();
            Mob npc = brain.getNpcEntity();
            if (npc != null && npc.isValid()) {
                npc.setAI(false);
            }
        }
        brains.clear();
        brainOrder.clear();

        if (memoryStore != null) {
            memoryStore.saveAll();
        }
        if (geminiClient != null) {
            geminiClient.shutdown();
        }
    }

    public void registerNPC(Mob npcEntity, CounselorData data) {
        UUID uuid = npcEntity.getUniqueId();
        NPCBrain brain = new NPCBrain(uuid, data, npcEntity);
        brains.put(uuid, brain);
        if (!brainOrder.contains(uuid)) {
            brainOrder.add(uuid);
        }
        plugin.getLogger().info("AI Brain登録: " + uuid);
    }

    public void unregisterNPC(UUID uuid) {
        NPCBrain brain = brains.remove(uuid);
        if (brain != null) {
            brain.clearActions();
        }
        brainOrder.remove(uuid);
    }

    public boolean hasBrain(UUID uuid) {
        return brains.containsKey(uuid);
    }

    /**
     * メインtickループ - 全ブレインを毎tick実行し、ラウンドロビンでAI判断をリクエスト
     */
    private void tickAll() {
        if (brains.isEmpty()) return;

        // 全ブレインのアクション実行 (毎tick)
        for (NPCBrain brain : brains.values()) {
            brain.tick();
        }

        // ラウンドロビンで1体ずつAI判断をチェック
        if (brainOrder.isEmpty()) return;

        if (roundRobinIndex >= brainOrder.size()) {
            roundRobinIndex = 0;
        }

        UUID currentUUID = brainOrder.get(roundRobinIndex);
        NPCBrain brain = brains.get(currentUUID);
        roundRobinIndex++;

        if (brain == null || brain.getNpcEntity() == null || !brain.getNpcEntity().isValid()) {
            return;
        }

        if (brain.needsDecision(aiConfig.getDecisionIntervalTicks())) {
            requestAIDecision(brain);
        }
    }

    /**
     * Gemini AIに行動決定をリクエスト (非同期)
     */
    private void requestAIDecision(NPCBrain brain) {
        Mob npc = brain.getNpcEntity();
        if (npc == null) return;

        // 環境スキャン (メインスレッド)
        DecisionContext context = awarenessAgent.scan(npc, brain);
        brain.setLastContext(context);
        brain.markAIRequestSent();

        // 性格プロファイル取得
        PersonalityProfile profile = personalityLoader.getProfile(brain.getData().getPersonalityType());

        // プロンプト構築
        String systemPrompt = promptBuilder.buildSystemPrompt(profile, brain);
        String userPrompt = promptBuilder.buildUserPrompt(context, brain);

        // 非同期でGemini APIコール
        geminiClient.requestBehavior(systemPrompt, userPrompt).thenAccept(response -> {
            // パース
            List<NPCAction> actions = responseParser.parse(response, npc, brain);

            // メインスレッドでアクション適用
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                brain.markAIResponseReceived();
                if (!actions.isEmpty()) {
                    for (NPCAction action : actions) {
                        brain.enqueueAction(action);
                    }
                } else {
                    // フォールバック: ランダム待機
                    brain.enqueueAction(
                            new com.ifmineai.ai.action.IdleAction(40 + new Random().nextInt(40))
                    );
                }
            });
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "AI判断リクエスト失敗: " + brain.getNpcUUID(), ex);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                brain.markAIResponseReceived();
                // エラー時はフォールバック: 移動エージェントによるランダム歩行
                NPCAction fallback = movementAgent.generateRandomWalk(npc, brain);
                if (fallback != null) {
                    brain.enqueueAction(fallback);
                }
            });
            return null;
        });
    }

    /**
     * プレイヤーからの会話メッセージを処理
     */
    public void handlePlayerMessage(UUID npcUUID, String playerName, String message) {
        NPCBrain brain = brains.get(npcUUID);
        if (brain == null) return;

        conversationAgent.handleMessage(brain, playerName, message, geminiClient, promptBuilder)
                .thenAccept(actions -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        for (NPCAction action : actions) {
                            brain.enqueueAction(action);
                        }
                    });
                })
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.WARNING, "会話処理失敗: " + npcUUID, ex);
                    return null;
                });
    }

    /**
     * プレイヤーが右クリックで会話開始
     */
    public void startConversation(UUID npcUUID, String playerName) {
        NPCBrain brain = brains.get(npcUUID);
        if (brain == null) return;

        brain.startConversation(playerName);
        conversationAgent.startConversation(brain, playerName, geminiClient, promptBuilder)
                .thenAccept(actions -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        for (NPCAction action : actions) {
                            brain.enqueueAction(action);
                        }
                    });
                })
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.WARNING, "会話開始失敗: " + npcUUID, ex);
                    return null;
                });
    }

    // --- Getters ---

    public boolean isEnabled() { return enabled; }
    public Map<UUID, NPCBrain> getBrains() { return Collections.unmodifiableMap(brains); }
    public NPCBrain getBrain(UUID uuid) { return brains.get(uuid); }
    public GeminiClient getGeminiClient() { return geminiClient; }
    public MemoryStore getMemoryStore() { return memoryStore; }
    public PersonalityLoader getPersonalityLoader() { return personalityLoader; }
    public AIConfig getAIConfig() { return aiConfig; }
}
