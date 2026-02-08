package com.ifmineai;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;

public class CounselorPatrolTask extends BukkitRunnable {

    private final Villager villager;
    private final CounselorData data;
    private final double speed;
    private final int pauseTicks;

    private double progress = 0.0;
    private boolean movingForward = true;
    private int pauseCounter = 0;

    public CounselorPatrolTask(Villager villager, CounselorData data,
                               double speed, int pauseTicks) {
        this.villager = villager;
        this.data = data;
        this.speed = speed;
        this.pauseTicks = pauseTicks;
    }

    @Override
    public void run() {
        if (villager.isDead() || !villager.isValid()) {
            cancel();
            return;
        }

        if (pauseCounter > 0) {
            pauseCounter--;
            return;
        }

        if (movingForward) {
            progress += speed / data.getRange();
            if (progress >= 1.0) {
                progress = 1.0;
                movingForward = false;
                pauseCounter = pauseTicks;
            }
        } else {
            progress -= speed / data.getRange();
            if (progress <= 0.0) {
                progress = 0.0;
                movingForward = true;
                pauseCounter = pauseTicks;
            }
        }

        double dx = 0;
        double dz = 0;
        float yaw = 0f;

        switch (data.getDirection()) {
            case "north":
                dz = -1;
                yaw = 180f;
                break;
            case "south":
                dz = 1;
                yaw = 0f;
                break;
            case "east":
                dx = 1;
                yaw = -90f;
                break;
            case "west":
                dx = -1;
                yaw = 90f;
                break;
        }

        if (!movingForward) {
            yaw = (yaw + 180f) % 360f;
        }

        double targetX = data.getOriginX() + dx * data.getRange() * progress;
        double targetZ = data.getOriginZ() + dz * data.getRange() * progress;

        World world = villager.getWorld();
        villager.teleport(new Location(world, targetX, data.getOriginY(), targetZ, yaw, 0f));
    }
}
