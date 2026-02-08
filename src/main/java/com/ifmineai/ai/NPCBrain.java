package com.ifmineai.ai;

import com.ifmineai.CounselorData;
import com.ifmineai.ai.action.IdleAction;
import com.ifmineai.ai.action.NPCAction;

import org.bukkit.Location;
import org.bukkit.entity.Mob;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class NPCBrain {

    private final UUID npcUUID;
    private final CounselorData data;
    private Mob npcEntity;
    private NPCState state;
    private final Deque<NPCAction> actionQueue;
    private NPCAction currentAction;
    private int ticksSinceLastDecision;
    private boolean awaitingAIResponse;
    private DecisionContext lastContext;
    private String conversationPartner; // プレイヤー名 (会話中の場合)

    public NPCBrain(UUID npcUUID, CounselorData data, Mob npcEntity) {
        this.npcUUID = npcUUID;
        this.data = data;
        this.npcEntity = npcEntity;
        this.state = NPCState.IDLE;
        this.actionQueue = new ArrayDeque<>();
        this.currentAction = null;
        this.ticksSinceLastDecision = 0;
        this.awaitingAIResponse = false;
    }

    /**
     * 毎tick呼ばれる。現在のアクションを実行し、完了したら次のアクションへ。
     */
    public void tick() {
        if (npcEntity == null || npcEntity.isDead() || !npcEntity.isValid()) {
            return;
        }

        ticksSinceLastDecision++;

        // 現在のアクションがない場合、キューから取得
        if (currentAction == null) {
            if (!actionQueue.isEmpty()) {
                currentAction = actionQueue.poll();
                currentAction.start(npcEntity);
                updateStateFromAction(currentAction);
            } else {
                // 会話中はTALKING状態を維持 (AI決定ループに入らないようにする)
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

    public boolean isInConversation() {
        return conversationPartner != null;
    }

    public void startConversation(String playerName) {
        this.conversationPartner = playerName;
        this.state = NPCState.TALKING;
    }

    public void endConversation() {
        this.conversationPartner = null;
        if (state == NPCState.TALKING) {
            state = NPCState.IDLE;
        }
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
