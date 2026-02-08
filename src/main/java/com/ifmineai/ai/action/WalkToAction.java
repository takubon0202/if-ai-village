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
    private static final int MAX_TICKS = 120; // 6秒タイムアウト
    private static final int RETRY_INTERVAL = 10; // 0.5秒ごとにリトライ
    private static final double ARRIVAL_DISTANCE = 2.0;
    private static final int STUCK_THRESHOLD = 3;

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
        // AI は常時 true なので、Pathfinder で目的地を設定するだけ
        npc.getPathfinder().moveTo(target, speed);
    }

    @Override
    public boolean tick(Mob npc) {
        ticksElapsed++;

        if (ticksElapsed >= MAX_TICKS) {
            LOGGER.fine("WalkTo タイムアウト: " + describe());
            npc.getPathfinder().stopPathfinding();
            return true;
        }

        // 到着チェック
        double distSq = npc.getLocation().distanceSquared(target);
        if (distSq <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
            npc.getPathfinder().stopPathfinding();
            return true;
        }

        // スタック検出
        retryCounter++;
        if (retryCounter >= RETRY_INTERVAL) {
            retryCounter = 0;

            double movedDistSq = npc.getLocation().distanceSquared(lastLocation);
            if (movedDistSq < 0.1) {
                stuckCounter++;
                if (stuckCounter >= STUCK_THRESHOLD) {
                    LOGGER.fine("WalkTo スタック: " + describe());
                    npc.getPathfinder().stopPathfinding();
                    return true;
                }
            } else {
                stuckCounter = 0;
            }
            lastLocation = npc.getLocation().clone();

            // Pathfinder 再発行
            npc.getPathfinder().moveTo(target, speed);
        }

        return false;
    }

    @Override
    public void cancel(Mob npc) {
        npc.getPathfinder().stopPathfinding();
    }

    @Override
    public String describe() {
        return "Walking to (" + target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ() + ")";
    }
}
