package com.ifmineai.ai;

import org.bukkit.Location;

import java.util.List;

public record DecisionContext(
        String npcName,
        String personalityType,
        NPCState currentState,
        Location npcLocation,
        Location homeLocation,
        double homeRadius,
        List<NearbyPlayer> nearbyPlayers,
        List<NearbyEntity> nearbyEntities,
        String timeOfDay,
        String weather,
        String biome,
        List<String> recentMemories,
        String currentActionDescription
) {

    public record NearbyPlayer(
            String name,
            double distance,
            boolean isSneaking,
            boolean isSprinting
    ) {}

    public record NearbyEntity(
            String type,
            double distance
    ) {}
}
