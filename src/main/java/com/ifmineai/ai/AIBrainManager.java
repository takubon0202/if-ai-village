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
import com.ifmineai.ai.action.SayAction;
import com.ifmineai.ai.memory.MemoryStore;
import com.ifmineai.ai.personality.PersonalityLoader;
import com.ifmineai.ai.personality.PersonalityProfile;
import com.ifmineai.config.AIConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class AIBrainManager {

    private final IFMineAIPlugin plugin;
    private final AIConfig aiConfig;
    private final Map<UUID, NPCBrain> brains = new ConcurrentHashMap<>();
    private final List<UUID> brainOrder = new ArrayList<>();
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

        personalityLoader = new PersonalityLoader(plugin);
        memoryStore = new MemoryStore(plugin);
        geminiClient = new GeminiClient(aiConfig);
        promptBuilder = new GeminiPromptBuilder(personalityLoader);
        responseParser = new GeminiResponseParser(memoryStore);
        awarenessAgent = new AwarenessAgent(aiConfig);
        awarenessAgent.setMemoryStore(memoryStore);
        movementAgent = new MovementAgent(aiConfig);
        conversationAgent = new ConversationAgent(aiConfig, memoryStore);
        memoryAgent = new MemoryAgent(aiConfig, memoryStore);

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
     * メインtickループ
     */
    private void tickAll() {
        if (brains.isEmpty()) return;

        // 全ブレインのアクション実行 (毎tick)
        for (NPCBrain brain : brains.values()) {
            brain.tick();

            // ホームリーシュ: 範囲外に出たら帰還
            if (!brain.isInConversation()
                    && brain.getState() == NPCState.IDLE
                    && brain.isTooFarFromHome()) {
                var returnAction = brain.createReturnHomeAction();
                if (returnAction != null) {
                    brain.interruptWith(returnAction);
                }
            }

            // 会話タイムアウト (60秒)
            int timeoutTicks = aiConfig.getConversationTimeoutSeconds() * 20;
            if (brain.isConversationTimedOut(timeoutTicks)) {
                String partner = brain.getConversationPartner();
                endConversation(brain.getNpcUUID());
                // プレイヤーに通知
                if (partner != null) {
                    Player player = Bukkit.getPlayer(partner);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(Component.text(
                                "NPCとの会話がタイムアウトしました", NamedTextColor.GRAY));
                        // activeConversations からも除去
                        plugin.getActiveConversations().values().remove(brain.getNpcUUID());
                    }
                }
            }
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

        DecisionContext context = awarenessAgent.scan(npc, brain);
        brain.setLastContext(context);
        brain.markAIRequestSent();

        PersonalityProfile profile = personalityLoader.getProfile(brain.getData().getPersonalityType());

        String systemPrompt = promptBuilder.buildSystemPrompt(profile, brain);
        String userPrompt = promptBuilder.buildUserPrompt(context, brain);

        geminiClient.requestBehavior(systemPrompt, userPrompt).thenAccept(response -> {
            List<NPCAction> actions = responseParser.parse(response, npc, brain);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                brain.markAIResponseReceived();

                // 会話中に行動レスポンスが到着した場合は破棄
                if (brain.isInConversation()) {
                    return;
                }

                if (!actions.isEmpty()) {
                    for (NPCAction action : actions) {
                        // 発言クールダウンフィルタ
                        if (action instanceof SayAction) {
                            if (!brain.canSpeakBehavior()) {
                                continue;
                            }
                            brain.markSpoke();
                        }
                        brain.enqueueAction(action);
                    }
                    // フィルタ後にアクションが空になった場合のフォールバック
                    if (brain.getActionQueueSize() == 0 && brain.getCurrentAction() == null) {
                        enqueueIdleFallback(npc, brain);
                    }
                } else {
                    enqueueIdleFallback(npc, brain);
                }
            });
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "AI判断リクエスト失敗: " + brain.getNpcUUID(), ex);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                brain.markAIResponseReceived();
                if (!brain.isInConversation()) {
                    enqueueIdleFallback(npc, brain);
                }
            });
            return null;
        });
    }

    /**
     * フォールバック: ランダム歩行 or アイドル
     */
    private void enqueueIdleFallback(Mob npc, NPCBrain brain) {
        NPCAction randomWalk = movementAgent.generateRandomWalk(npc, brain);
        if (randomWalk != null) {
            brain.enqueueAction(randomWalk);
        } else {
            brain.enqueueAction(new com.ifmineai.ai.action.IdleAction(60 + new Random().nextInt(60)));
        }
    }

    /**
     * プレイヤーからの会話メッセージを処理
     */
    public void handlePlayerMessage(UUID npcUUID, String playerName, String message) {
        NPCBrain brain = brains.get(npcUUID);
        if (brain == null) return;

        // 会話アクティビティを記録 (タイムアウト延長)
        brain.markConversationActivity();

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

        brain.clearActions();
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

    /**
     * 会話を終了し、会話履歴もクリアする
     */
    public void endConversation(UUID npcUUID) {
        NPCBrain brain = brains.get(npcUUID);
        if (brain != null) {
            brain.endConversation();
        }
        if (conversationAgent != null) {
            conversationAgent.endConversation(npcUUID);
        }
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
