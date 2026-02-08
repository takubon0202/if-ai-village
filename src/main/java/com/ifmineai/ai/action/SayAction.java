package com.ifmineai.ai.action;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class SayAction implements NPCAction {

    private final String message;
    private final double radius;
    private final String npcDisplayName;

    public SayAction(String message, double radius, String npcDisplayName) {
        this.message = message;
        this.radius = radius;
        this.npcDisplayName = npcDisplayName;
    }

    @Override
    public void start(Mob npc) {
        Component chat = Component.text(npcDisplayName + ": ", NamedTextColor.GOLD)
                .append(Component.text(message, NamedTextColor.WHITE));

        npc.getLocation().getNearbyPlayers(radius).forEach(player -> player.sendMessage(chat));
    }

    @Override
    public boolean tick(Mob npc) {
        return true; // 即時完了
    }

    @Override
    public void cancel(Mob npc) {
    }

    @Override
    public String describe() {
        return "Saying: " + message;
    }
}
