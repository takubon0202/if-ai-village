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
    private Location lastLocation;
    private int stuckCounter;
    private static final int MAX_TICKS = 120; // 6秒タイムアウト (短縮)
    private static final int RETRY_INTERVAL = 10; // 0.5秒ごとにリトライ (短縮)
    private static final double ARRIVAL_DISTANCE = 1.8;
    private static final int STUCK_THRESHOLD = 3; // 3回連続移動なしでスタック判定

    public WalkToAction(Location target, double speed) {
        this.target = target;
        this.speed = speed;
    }

    @Override
    public void start(Mob npc) {
        ticksElapsed = 0;
        retryCounter = 0;
        stuckCounter = 0;
        lastLocation = npc.getLocation().clone();
        // AI有効にしてPathfinderで移動開始
        npc.setAI(true);
        npc.getPathfinder().moveTo(target, speed);
    }

    @Override
    public boolean tick(Mob npc) {
        ticksElapsed++;

        if (ticksElapsed >= MAX_TICKS) {
            LOGGER.fine("WalkTo タイムアウト: " + describe() + " NPC=" + npc.getName());
            npc.getPathfinder().stopPathfinding();
            npc.setAI(false);
            return true;
        }

        // 到着チェック
        double distSq = npc.getLocation().distanceSquared(target);
        if (distSq <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
            npc.getPathfinder().stopPathfinding();
            npc.setAI(false);
            return true;
        }

        // スタック検出: 一定間隔で位置が変わっていなければスタック
        retryCounter++;
        if (retryCounter >= RETRY_INTERVAL) {
            retryCounter = 0;

            double movedDistSq = npc.getLocation().distanceSquared(lastLocation);
            if (movedDistSq < 0.1) {
                // ほぼ動いていない
                stuckCounter++;
                if (stuckCounter >= STUCK_THRESHOLD) {
                    LOGGER.fine("WalkTo スタック検出: " + describe() + " NPC=" + npc.getName());
                    npc.getPathfinder().stopPathfinding();
                    npc.setAI(false);
                    return true;
                }
            } else {
                stuckCounter = 0;
            }
            lastLocation = npc.getLocation().clone();

            // Pathfinderを再発行 (経路が消えた場合の再開)
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
