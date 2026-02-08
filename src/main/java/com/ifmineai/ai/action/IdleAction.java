package com.ifmineai.ai.action;

import org.bukkit.entity.Mob;

public class IdleAction implements NPCAction {

    private final int durationTicks;
    private int ticksElapsed;

    public IdleAction(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    @Override
    public void start(Mob npc) {
        ticksElapsed = 0;
        npc.setAI(false);
    }

    @Override
    public boolean tick(Mob npc) {
        ticksElapsed++;
        return ticksElapsed >= durationTicks;
    }

    @Override
    public void cancel(Mob npc) {
    }

    @Override
    public String describe() {
        return "Idling for " + (durationTicks / 20.0) + "s";
    }
}
