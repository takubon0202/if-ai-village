package com.ifmineai.ai;

import com.ifmineai.CounselorData;
import com.ifmineai.ai.action.IdleAction;
import com.ifmineai.ai.action.NPCAction;
import com.ifmineai.ai.action.WalkToAction;
import com.ifmineai.ai.agent.MovementAgent;

import org.bukkit.Location;
import org.bukkit.entity.Mob;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NPCBrain {

    // 発言クールダウン: 行動ループからの自発的発言は30秒に1回まで
    private static final int SPEECH_COOLDOWN_TICKS = 600; // 30秒
    // ホームリーシュ: ホームからこの倍率を超えたら引き戻す
    private static final double HOME_LEASH_MULTIPLIER = 2.0;

    private final UUID npcUUID;
    private final CounselorData data;
    private Mob npcEntity;
    private NPCState state;
    private final Deque<NPCAction> actionQueue;
    private NPCAction currentAction;
    private int ticksSinceLastDecision;
    private boolean awaitingAIResponse;
    private DecisionContext lastContext;
    private String conversationPartner;

    // 発言クールダウン
    private int ticksSinceLastSpeech;
    // 最近挨拶したプレイヤー (セッション内で重複挨拶を減らす)
    private final Set<String> greetedPlayers = new HashSet<>();
    // 会話タイムアウト用
    private int ticksSinceLastConversationActivity;

    public NPCBrain(UUID npcUUID, CounselorData data, Mob npcEntity) {
        this.npcUUID = npcUUID;
        this.data = data;
        this.npcEntity = npcEntity;
        this.state = NPCState.IDLE;
        this.actionQueue = new ArrayDeque<>();
        this.currentAction = null;
        this.ticksSinceLastDecision = 0;
        this.awaitingAIResponse = false;
        this.ticksSinceLastSpeech = SPEECH_COOLDOWN_TICKS; // 初回は即話せる
        this.ticksSinceLastConversationActivity = 0;
    }

    public void tick() {
        if (npcEntity == null || npcEntity.isDead() || !npcEntity.isValid()) {
            return;
        }

        ticksSinceLastDecision++;
        ticksSinceLastSpeech++;

        if (isInConversation()) {
            ticksSinceLastConversationActivity++;
        }

        // 現在のアクションがない場合、キューから取得
        if (currentAction == null) {
            if (!actionQueue.isEmpty()) {
                currentAction = actionQueue.poll();
                currentAction.start(npcEntity);
                updateStateFromAction(currentAction);
            } else {
                state = isInConversation() ? NPCState.TALKING : NPCState.IDLE;
            }
            return;
        }

        // 現在のアクションをtick
        boolean done = currentAction.tick(npcEntity);
        if (done) {
            currentAction = null;
            if (!actionQueue.isEmpty()) {
                currentAction = actionQueue.poll();
                currentAction.start(npcEntity);
                updateStateFromAction(currentAction);
            } else {
                state = isInConversation() ? NPCState.TALKING : NPCState.IDLE;
            }
        }
    }

    public void enqueueAction(NPCAction action) {
        actionQueue.offer(action);
    }

    public void clearActions() {
        if (currentAction != null) {
            currentAction.cancel(npcEntity);
            currentAction = null;
        }
        actionQueue.clear();
        state = NPCState.IDLE;
    }

    public void interruptWith(NPCAction action) {
        clearActions();
        enqueueAction(action);
    }

    public boolean needsDecision(int decisionIntervalTicks) {
        return !awaitingAIResponse
                && state == NPCState.IDLE
                && actionQueue.isEmpty()
                && currentAction == null
                && !isInConversation()
                && ticksSinceLastDecision >= decisionIntervalTicks;
    }

    public void markAIRequestSent() {
        awaitingAIResponse = true;
        state = NPCState.THINKING;
    }

    public void markAIResponseReceived() {
        awaitingAIResponse = false;
        ticksSinceLastDecision = 0;
    }

    // --- 発言クールダウン ---

    /** 行動ループからの自発的発言が許可されているか */
    public boolean canSpeakBehavior() {
        return ticksSinceLastSpeech >= SPEECH_COOLDOWN_TICKS;
    }

    /** 発言したことを記録 */
    public void markSpoke() {
        ticksSinceLastSpeech = 0;
    }

    /** このプレイヤーに最近挨拶済みか */
    public boolean hasGreeted(String playerName) {
        return greetedPlayers.contains(playerName);
    }

    /** 挨拶済みとしてマーク */
    public void markGreeted(String playerName) {
        greetedPlayers.add(playerName);
    }

    // --- 会話管理 ---

    public boolean isInConversation() {
        return conversationPartner != null;
    }

    public void startConversation(String playerName) {
        this.conversationPartner = playerName;
        this.state = NPCState.TALKING;
        this.ticksSinceLastConversationActivity = 0;
    }

    public void endConversation() {
        this.conversationPartner = null;
        if (state == NPCState.TALKING) {
            state = NPCState.IDLE;
        }
        this.ticksSinceLastConversationActivity = 0;
    }

    /** 会話でやりとりがあったことを記録 */
    public void markConversationActivity() {
        this.ticksSinceLastConversationActivity = 0;
    }

    /** 会話がタイムアウトしたか */
    public boolean isConversationTimedOut(int timeoutTicks) {
        return isInConversation() && ticksSinceLastConversationActivity >= timeoutTicks;
    }

    // --- ホームリーシュ ---

    /** NPCがホーム範囲から大きく離れているか */
    public boolean isTooFarFromHome() {
        if (npcEntity == null) return false;
        Location home = getHomeLocation();
        if (home == null) return false;
        double maxDist = data.getRange() * HOME_LEASH_MULTIPLIER;
        return npcEntity.getLocation().distanceSquared(home) > maxDist * maxDist;
    }

    /** ホームに帰還するアクションを生成 */
    public WalkToAction createReturnHomeAction() {
        Location home = getHomeLocation();
        if (home == null) return null;
        home.setY(MovementAgent.findGroundY(
                npcEntity.getWorld(), home.getBlockX(), home.getBlockZ(),
                npcEntity.getLocation().getBlockY() + 10));
        return new WalkToAction(home, 1.0);
    }

    public Location getHomeLocation() {
        if (npcEntity == null) return null;
        return new Location(
                npcEntity.getWorld(),
                data.getOriginX(), data.getOriginY(), data.getOriginZ()
        );
    }

    private void updateStateFromAction(NPCAction action) {
        if (action instanceof com.ifmineai.ai.action.WalkToAction) {
            state = NPCState.WALKING;
        } else if (action instanceof com.ifmineai.ai.action.LookAtAction) {
            state = NPCState.LOOKING;
        } else if (action instanceof com.ifmineai.ai.action.SayAction) {
            state = NPCState.TALKING;
        } else if (action instanceof com.ifmineai.ai.action.EmoteAction) {
            state = NPCState.EMOTING;
        } else if (action instanceof com.ifmineai.ai.action.IdleAction) {
            state = NPCState.IDLE;
        }
    }

    // --- Getters ---

    public UUID getNpcUUID() { return npcUUID; }
    public CounselorData getData() { return data; }
    public Mob getNpcEntity() { return npcEntity; }
    public void setNpcEntity(Mob npcEntity) { this.npcEntity = npcEntity; }
    public NPCState getState() { return state; }
    public NPCAction getCurrentAction() { return currentAction; }
    public int getActionQueueSize() { return actionQueue.size(); }
    public boolean isAwaitingAIResponse() { return awaitingAIResponse; }
    public DecisionContext getLastContext() { return lastContext; }
    public void setLastContext(DecisionContext ctx) { this.lastContext = ctx; }
    public String getConversationPartner() { return conversationPartner; }
    public int getTicksSinceLastDecision() { return ticksSinceLastDecision; }
}
