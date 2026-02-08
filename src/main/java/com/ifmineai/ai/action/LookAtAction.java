package com.ifmineai.ai.action;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

public class LookAtAction implements NPCAction {

    private final Entity targetEntity;
    private final int durationTicks;
    private int ticksElapsed;

    public LookAtAction(Entity targetEntity, int durationTicks) {
        this.targetEntity = targetEntity;
        this.durationTicks = durationTicks;
    }

    @Override
    public void start(Mob npc) {
        ticksElapsed = 0;
        lookAt(npc);
    }

    @Override
    public boolean tick(Mob npc) {
        ticksElapsed++;

        if (targetEntity == null || targetEntity.isDead() || !targetEntity.isValid()) {
            return true;
        }

        lookAt(npc);

        return ticksElapsed >= durationTicks;
    }

    @Override
    public void cancel(Mob npc) {
        // 特にクリーンアップ不要
    }

    @Override
    public String describe() {
        String name = targetEntity != null ? targetEntity.getName() : "unknown";
        return "Looking at " + name;
    }

    private void lookAt(Mob npc) {
        if (targetEntity == null || !targetEntity.isValid()) return;

        Location npcLoc = npc.getLocation();
        Location targetLoc = targetEntity.getLocation().add(0, targetEntity.getHeight() * 0.8, 0);

        double dx = targetLoc.getX() - npcLoc.getX();
        double dy = targetLoc.getY() - (npcLoc.getY() + npc.getHeight() * 0.8);
        double dz = targetLoc.getZ() - npcLoc.getZ();

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        npcLoc.setYaw(yaw);
        npcLoc.setPitch(pitch);
        npc.teleport(npcLoc);
    }
}
