package com.ifmineai.ai.agent;

import com.ifmineai.ai.DecisionContext;
import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.memory.MemoryEntry;
import com.ifmineai.ai.memory.MemoryStore;
import com.ifmineai.config.AIConfig;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 環境認識エージェント - NPC周辺の環境をスキャンしてDecisionContextを構築
 */
public class AwarenessAgent implements BehaviorAgent {

    private final AIConfig config;
    private MemoryStore memoryStore;

    public AwarenessAgent(AIConfig config) {
        this.config = config;
    }

    public void setMemoryStore(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    @Override
    public String name() {
        return "AwarenessAgent";
    }

    /**
     * NPC周辺をスキャンしてコンテキストを生成
     */
    public DecisionContext scan(Mob npc, NPCBrain brain) {
        Location npcLoc = npc.getLocation();
        World world = npc.getWorld();
        double radius = config.getAwarenessRadius();

        // 近くのプレイヤー
        List<DecisionContext.NearbyPlayer> nearbyPlayers = new ArrayList<>();
        for (Player player : npcLoc.getNearbyPlayers(radius)) {
            nearbyPlayers.add(new DecisionContext.NearbyPlayer(
                    player.getName(),
                    player.getLocation().distance(npcLoc),
                    player.isSneaking(),
                    player.isSprinting()
            ));
        }

        // 近くのエンティティ (プレイヤー以外)
        List<DecisionContext.NearbyEntity> nearbyEntities = new ArrayList<>();
        for (Entity entity : npcLoc.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player || entity.getUniqueId().equals(npc.getUniqueId())) continue;
            if (nearbyEntities.size() >= 10) break; // 上限
            nearbyEntities.add(new DecisionContext.NearbyEntity(
                    entity.getType().name(),
                    entity.getLocation().distance(npcLoc)
            ));
        }

        // 時間帯
        String timeOfDay = getTimeOfDay(world.getTime());

        // 天気
        String weather;
        if (world.isThundering()) {
            weather = "thunderstorm";
        } else if (world.hasStorm()) {
            weather = "rain";
        } else {
            weather = "clear";
        }

        // バイオーム
        String biome = world.getBiome(npcLoc.getBlockX(), npcLoc.getBlockY(), npcLoc.getBlockZ()).getKey().getKey();

        // 現在のアクション説明
        String currentActionDesc = "";
        if (brain.getCurrentAction() != null) {
            currentActionDesc = brain.getCurrentAction().describe();
        }

        // 最近の記憶を取得
        List<String> recentMemories = new ArrayList<>();
        if (memoryStore != null) {
            List<MemoryEntry> memories = memoryStore.getRecentMemories(brain.getNpcUUID(), 5);
            for (MemoryEntry mem : memories) {
                recentMemories.add(mem.content());
            }
        }

        return new DecisionContext(
                npc.getName(),
                brain.getData().getPersonalityType(),
                brain.getState(),
                npcLoc,
                brain.getHomeLocation(),
                config.getHomeRadius(),
                nearbyPlayers,
                nearbyEntities,
                timeOfDay,
                weather,
                biome,
                recentMemories,
                currentActionDesc
        );
    }

    private String getTimeOfDay(long time) {
        if (time < 6000) return "morning";
        if (time < 12000) return "afternoon";
        if (time < 13000) return "evening";
        return "night";
    }
}
