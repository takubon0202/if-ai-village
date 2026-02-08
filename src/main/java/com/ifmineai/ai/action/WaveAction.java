package com.ifmineai.ai.action;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

public class WaveAction implements NPCAction {

    private final Entity targetEntity;
    private int ticksElapsed;
    private static final int DURATION_TICKS = 30; // 1.5秒

    public WaveAction(Entity targetEntity) {
        this.targetEntity = targetEntity;
    }

    @Override
    public void start(Mob npc) {
        ticksElapsed = 0;
        // まず対象を見る
        if (targetEntity != null && targetEntity.isValid()) {
            lookAt(npc, targetEntity);
        }
    }

    @Override
    public boolean tick(Mob npc) {
        ticksElapsed++;

        // 手振りをパーティクルで表現 (5tick毎)
        if (ticksElapsed % 5 == 0) {
            double side = (ticksElapsed / 5 % 2 == 0) ? 0.4 : -0.4;
            Location handLoc = npc.getLocation().add(side, npc.getHeight() * 0.9, 0);
            npc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, handLoc, 3, 0.1, 0.1, 0.1, 0);
        }

        if (targetEntity != null && targetEntity.isValid()) {
            lookAt(npc, targetEntity);
        }

        return ticksElapsed >= DURATION_TICKS;
    }

    @Override
    public void cancel(Mob npc) {
    }

    @Override
    public String describe() {
        String name = targetEntity != null ? targetEntity.getName() : "someone";
        return "Waving at " + name;
    }

    private void lookAt(Mob npc, Entity target) {
        Location npcLoc = npc.getLocation();
        Location targetLoc = target.getLocation();

        double dx = targetLoc.getX() - npcLoc.getX();
        double dz = targetLoc.getZ() - npcLoc.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        npcLoc.setYaw(yaw);
        npcLoc.setPitch(0);
        npc.teleport(npcLoc);
    }
}
