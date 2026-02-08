package com.ifmineai.ai.action;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Mob;

public class EmoteAction implements NPCAction {

    public enum Emotion {
        HAPPY(Particle.HEART, 5),
        SAD(Particle.FALLING_WATER, 8),
        CURIOUS(Particle.ENCHANT, 10),
        ANGRY(Particle.LAVA, 5),
        SURPRISED(Particle.FIREWORK, 8);

        private final Particle particle;
        private final int count;

        Emotion(Particle particle, int count) {
            this.particle = particle;
            this.count = count;
        }

        public Particle getParticle() { return particle; }
        public int getCount() { return count; }
    }

    private final Emotion emotion;
    private final int durationTicks;
    private int ticksElapsed;
    private int particleTimer;

    public EmoteAction(Emotion emotion, int durationTicks) {
        this.emotion = emotion;
        this.durationTicks = durationTicks;
    }

    public EmoteAction(Emotion emotion) {
        this(emotion, 30); // デフォルト1.5秒
    }

    @Override
    public void start(Mob npc) {
        ticksElapsed = 0;
        particleTimer = 0;
        spawnParticles(npc);
    }

    @Override
    public boolean tick(Mob npc) {
        ticksElapsed++;
        particleTimer++;

        if (particleTimer >= 10) {
            particleTimer = 0;
            spawnParticles(npc);
        }

        return ticksElapsed >= durationTicks;
    }

    @Override
    public void cancel(Mob npc) {
    }

    @Override
    public String describe() {
        return "Emoting: " + emotion.name();
    }

    private void spawnParticles(Mob npc) {
        Location loc = npc.getLocation().add(0, npc.getHeight() + 0.3, 0);
        npc.getWorld().spawnParticle(
                emotion.getParticle(),
                loc,
                emotion.getCount(),
                0.3, 0.3, 0.3,
                0.02
        );
    }
}
