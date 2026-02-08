package com.ifmineai.ai.action;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class SayAction implements NPCAction {

    private final String message;
    private final double radius;
    private final String npcDisplayName;
    private final String targetPlayerName; // null = 全体、指定 = そのプレイヤーのみ

    /**
     * 全近隣プレイヤーに話す (行動ループ用)
     */
    public SayAction(String message, double radius, String npcDisplayName) {
        this(message, radius, npcDisplayName, null);
    }

    /**
     * 特定プレイヤーに話す (会話用)
     */
    public SayAction(String message, double radius, String npcDisplayName, String targetPlayerName) {
        this.message = message;
        this.radius = radius;
        this.npcDisplayName = npcDisplayName;
        this.targetPlayerName = targetPlayerName;
    }

    @Override
    public void start(Mob npc) {
        Component chat = Component.text(npcDisplayName + ": ", NamedTextColor.GOLD)
                .append(Component.text(message, NamedTextColor.WHITE));

        if (targetPlayerName != null) {
            // 会話モード: 相手プレイヤーにだけ送信 + NPCがプレイヤーの方を向く
            Player target = Bukkit.getPlayer(targetPlayerName);
            if (target != null && target.isOnline()) {
                target.sendMessage(chat);
                lookAtPlayer(npc, target);
            }
        } else {
            // 全体モード: 範囲内の全プレイヤーに送信
            npc.getLocation().getNearbyPlayers(radius).forEach(player -> player.sendMessage(chat));
        }
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

    /**
     * NPCをプレイヤーの方向に向ける
     */
    private void lookAtPlayer(Mob npc, Player player) {
        Location npcLoc = npc.getLocation();
        Location playerLoc = player.getLocation();

        double dx = playerLoc.getX() - npcLoc.getX();
        double dy = (playerLoc.getY() + player.getHeight() * 0.8) - (npcLoc.getY() + npc.getHeight() * 0.8);
        double dz = playerLoc.getZ() - npcLoc.getZ();

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        npcLoc.setYaw(yaw);
        npcLoc.setPitch(pitch);
        npc.teleport(npcLoc);
    }
}
