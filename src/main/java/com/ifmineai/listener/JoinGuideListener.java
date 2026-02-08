package com.ifmineai.listener;

import com.ifmineai.IFMineAIPlugin;
import com.ifmineai.config.AIConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinGuideListener implements Listener {

    private static final TextColor ACCENT = TextColor.color(0x55FFFF);
    private static final TextColor GOLD = TextColor.color(0xFFAA00);
    private static final TextColor LINE_COLOR = TextColor.color(0x555555);
    private static final TextColor DESC_COLOR = TextColor.color(0xAAAAAA);

    private final IFMineAIPlugin plugin;

    public JoinGuideListener(IFMineAIPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 少し遅延させて他のメッセージの後に表示
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sendWelcomeGuide(player);
        }, 20L);
    }

    private void sendWelcomeGuide(Player player) {
        AIConfig config = plugin.getAIConfig();
        boolean aiRunning = plugin.getAIBrainManager() != null && plugin.getAIBrainManager().isEnabled();
        int npcCount = plugin.getCounselorManager() != null
                ? plugin.getCounselorManager().getCounselors().size() : 0;

        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR);

        // ヘッダー
        player.sendMessage(Component.empty());
        player.sendMessage(line);
        player.sendMessage(
                Component.text("  IF MineAI ", GOLD, TextDecoration.BOLD)
                        .append(Component.text("v" + plugin.getPluginMeta().getVersion(), DESC_COLOR)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(
                Component.text("  AI NPCと会話できるプラグイン", DESC_COLOR)
        );
        player.sendMessage(line);

        // ステータス
        Component aiStatus;
        if (aiRunning) {
            aiStatus = Component.text("  AI: ", NamedTextColor.WHITE)
                    .append(Component.text("稼働中", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" | NPC: " + npcCount + "体", DESC_COLOR)
                            .decoration(TextDecoration.BOLD, false));
        } else if (!config.isEnabledFlag()) {
            aiStatus = Component.text("  AI: ", NamedTextColor.WHITE)
                    .append(Component.text("無効", NamedTextColor.RED))
                    .append(Component.text(" - /ainpc enable で有効化", NamedTextColor.YELLOW));
        } else if (!config.isApiKeyValid()) {
            aiStatus = Component.text("  AI: ", NamedTextColor.WHITE)
                    .append(Component.text("APIキー未設定", NamedTextColor.RED))
                    .append(Component.text(" - /ainpc setkey <キー>", NamedTextColor.YELLOW));
        } else {
            aiStatus = Component.text("  AI: ", NamedTextColor.WHITE)
                    .append(Component.text("準備中", NamedTextColor.YELLOW));
        }
        player.sendMessage(aiStatus);
        player.sendMessage(Component.empty());

        // コマンド一覧 (クリック可能)
        player.sendMessage(
                Component.text("  --- よく使うコマンド ---", GOLD)
        );

        sendClickableCommand(player,
                "/mineai",
                "総合ヘルプを表示",
                "/mineai"
        );

        sendClickableCommand(player,
                "/counselor spawn north 10",
                "NPCをスポーン",
                "/counselor spawn north 10"
        );

        sendClickableCommand(player,
                "/counselor list",
                "NPC一覧を表示",
                "/counselor list"
        );

        if (player.hasPermission("ifmineai.ai.admin")) {
            sendClickableCommand(player,
                    "/ainpc status",
                    "AI状態を確認",
                    "/ainpc status"
            );
        }

        player.sendMessage(Component.empty());

        // 初心者ヒント
        if (npcCount == 0) {
            player.sendMessage(
                    Component.text("  Tip: ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("まだNPCがいません! ", NamedTextColor.WHITE)
                                    .decoration(TextDecoration.BOLD, false))
                            .append(Component.text("/counselor spawn north 10", ACCENT)
                                    .decoration(TextDecoration.BOLD, false)
                                    .clickEvent(ClickEvent.suggestCommand("/counselor spawn north 10"))
                                    .hoverEvent(HoverEvent.showText(
                                            Component.text("クリックでコマンドを入力欄にコピー", NamedTextColor.YELLOW)
                                    )))
                            .append(Component.text(" で最初のNPCを作ろう!", NamedTextColor.WHITE)
                                    .decoration(TextDecoration.BOLD, false))
            );
        } else if (!aiRunning && player.hasPermission("ifmineai.ai.admin")) {
            player.sendMessage(
                    Component.text("  Tip: ", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .append(Component.text("AIを有効にすると、NPCが自律的に行動・会話します!", NamedTextColor.WHITE)
                                    .decoration(TextDecoration.BOLD, false))
            );
            player.sendMessage(
                    Component.text("       /ainpc setkey <APIキー> → /ainpc enable → /ainpc reload", ACCENT)
                            .clickEvent(ClickEvent.suggestCommand("/ainpc setkey "))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("クリックでAPIキー設定を開始", NamedTextColor.YELLOW)
                            ))
            );
        }

        player.sendMessage(line);
        player.sendMessage(
                Component.text("  詳しくは ", DESC_COLOR)
                        .append(Component.text("/mineai help", ACCENT)
                                .clickEvent(ClickEvent.runCommand("/mineai help"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックでヘルプを表示", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" をご覧ください", DESC_COLOR))
        );
        player.sendMessage(line);
        player.sendMessage(Component.empty());
    }

    private void sendClickableCommand(Player player, String command, String description, String runCommand) {
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("> ", GOLD))
                        .append(Component.text(command, ACCENT)
                                .clickEvent(ClickEvent.suggestCommand(runCommand))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックでコマンドを入力", NamedTextColor.YELLOW)
                                                .append(Component.newline())
                                                .append(Component.text(runCommand, NamedTextColor.WHITE))
                                )))
                        .append(Component.text(" - " + description, DESC_COLOR))
        );
    }
}
