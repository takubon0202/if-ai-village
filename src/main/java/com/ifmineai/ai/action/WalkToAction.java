package com.ifmineai.ai.action;

import org.bukkit.Location;
import org.bukkit.entity.Mob;

import java.util.logging.Logger;

public class WalkToAction implements NPCAction {

    private static final Logger LOGGER = Logger.getLogger(WalkToAction.class.getName());

    private final Location target;
    private final double speed;
    private int ticksElapsed;
    private int retryCounter;
    private static final int MAX_TICKS = 200; // 10秒タイムアウト
    private static final int RETRY_INTERVAL = 20;
    private static final double ARRIVAL_DISTANCE = 1.5;

    public WalkToAction(Location target, double speed) {
        this.target = target;
        this.speed = speed;
    }

    @Override
    public void start(Mob npc) {
        ticksElapsed = 0;
        retryCounter = 0;
        npc.setAI(true);
        npc.getPathfinder().moveTo(target, speed);
    }

    @Override
    public boolean tick(Mob npc) {
        ticksElapsed++;

        if (ticksElapsed >= MAX_TICKS) {
            LOGGER.fine("WalkTo タイムアウト: " + describe() + " NPC=" + npc.getName()
                    + " 現在地=(" + npc.getLocation().getBlockX() + "," + npc.getLocation().getBlockY() + "," + npc.getLocation().getBlockZ() + ")");
            npc.setAI(false);
            return true;
        }

        double distSq = npc.getLocation().distanceSquared(target);
        if (distSq <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
            npc.setAI(false);
            return true;
        }

        // Pathfinderが停止した場合の再発行
        retryCounter++;
        if (retryCounter >= RETRY_INTERVAL) {
            retryCounter = 0;
            npc.getPathfinder().moveTo(target, speed);
        }

        return false;
    }

    @Override
    public void cancel(Mob npc) {
        npc.getPathfinder().stopPathfinding();
        npc.setAI(false);
    }

    @Override
    public String describe() {
        return "Walking to (" + target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ() + ")";
    }
}
