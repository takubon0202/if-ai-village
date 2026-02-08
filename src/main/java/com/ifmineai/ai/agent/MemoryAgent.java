package com.ifmineai.ai.agent;

import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.memory.MemoryEntry;
import com.ifmineai.ai.memory.MemoryStore;
import com.ifmineai.ai.memory.MemoryType;
import com.ifmineai.config.AIConfig;

import java.util.List;
import java.util.UUID;

/**
 * 記憶管理エージェント - NPCの短期/長期記憶を操作
 */
public class MemoryAgent implements BehaviorAgent {

    private final AIConfig config;
    private final MemoryStore memoryStore;

    public MemoryAgent(AIConfig config, MemoryStore memoryStore) {
        this.config = config;
        this.memoryStore = memoryStore;
    }

    @Override
    public String name() {
        return "MemoryAgent";
    }

    /**
     * NPCの最近の記憶を取得 (プロンプト用)
     */
    public List<String> getRecentMemories(UUID npcUUID, int limit) {
        return memoryStore.getRecentMemories(npcUUID, limit)
                .stream()
                .map(MemoryEntry::content)
                .toList();
    }

    /**
     * 記憶を保存 (AIのrememberツールコールから呼ばれる)
     */
    public void remember(UUID npcUUID, String content, int importance) {
        memoryStore.addMemory(npcUUID, new MemoryEntry(
                MemoryType.OBSERVATION,
                content,
                importance
        ));
    }

    /**
     * インタラクション記憶を保存
     */
    public void recordInteraction(UUID npcUUID, String description) {
        memoryStore.addMemory(npcUUID, new MemoryEntry(
                MemoryType.INTERACTION,
                description,
                2
        ));
    }

    /**
     * 場所記憶を保存
     */
    public void recordLocation(UUID npcUUID, String description) {
        memoryStore.addMemory(npcUUID, new MemoryEntry(
                MemoryType.LOCATION,
                description,
                1
        ));
    }
}
