package com.ifmineai.command;

import com.ifmineai.CounselorManager;
import com.ifmineai.IFMineAIPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CounselorCommandHandler implements CommandExecutor {

    private static final TextColor ACCENT = TextColor.color(0x55FFFF);
    private static final TextColor DESC_COLOR = TextColor.color(0xAAAAAA);
    private static final TextColor LINE_COLOR = TextColor.color(0x555555);

    private final IFMineAIPlugin plugin;
    private final CounselorManager counselorManager;

    public CounselorCommandHandler(IFMineAIPlugin plugin, CounselorManager counselorManager) {
        this.plugin = plugin;
        this.counselorManager = counselorManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行できます", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "spawn" -> handleSpawn(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> counselorManager.listCounselors(player);
            default -> {
                player.sendMessage(Component.text("不明なサブコマンドです。", NamedTextColor.RED)
                        .append(Component.text(" /counselor help", ACCENT)
                                .clickEvent(ClickEvent.runCommand("/counselor help"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックでヘルプを表示", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" でヘルプを表示", NamedTextColor.RED)));
            }
        }

        return true;
    }

    private void handleSpawn(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
            player.sendMessage(
                    Component.text("  spawn の使い方:", NamedTextColor.GOLD, TextDecoration.BOLD)
            );
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
            player.sendMessage(
                    Component.text("  /counselor spawn <方角> <範囲> [性格タイプ]", ACCENT)
                            .clickEvent(ClickEvent.suggestCommand("/counselor spawn "))
            );
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("  方角: ", NamedTextColor.WHITE)
                    .append(Component.text("north / south / east / west", DESC_COLOR)));
            player.sendMessage(Component.text("  範囲: ", NamedTextColor.WHITE)
                    .append(Component.text("パトロール距離 (1~50ブロック)", DESC_COLOR)));
            player.sendMessage(Component.text("  性格: ", NamedTextColor.WHITE)
                    .append(Component.text("counselor / guard / merchant / explorer", DESC_COLOR)));
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("  例:", NamedTextColor.GOLD));

            sendExample(player, "/counselor spawn north 10", "北方向に10ブロック範囲でスポーン");
            sendExample(player, "/counselor spawn east 20 guard", "東に衛兵タイプでスポーン");
            sendExample(player, "/counselor spawn south 5 merchant", "南に商人タイプでスポーン");

            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
            return;
        }

        String direction = args[1].toLowerCase();
        if (!List.of("north", "south", "east", "west").contains(direction)) {
            player.sendMessage(Component.text("方角は north, south, east, west のいずれかを指定してください", NamedTextColor.RED));
            player.sendMessage(
                    Component.text("  → ", DESC_COLOR)
                            .append(Component.text("/counselor spawn north 10", ACCENT)
                                    .clickEvent(ClickEvent.suggestCommand("/counselor spawn " + args[1] + " ")))
            );
            return;
        }

        int range;
        try {
            range = Integer.parseInt(args[2]);
            if (range < 1 || range > 200) {
                player.sendMessage(Component.text("範囲は1~200の整数で指定してください", NamedTextColor.RED));
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("範囲は数値で指定してください (例: 10)", NamedTextColor.RED));
            return;
        }

        String personalityType = "counselor";
        if (args.length >= 4) {
            personalityType = args[3].toLowerCase();
        }

        counselorManager.spawnCounselor(player, direction, range, personalityType);
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
            player.sendMessage(
                    Component.text("  remove の使い方:", NamedTextColor.GOLD, TextDecoration.BOLD)
            );
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
            player.sendMessage(
                    Component.text("  /counselor remove nearest", ACCENT)
                            .clickEvent(ClickEvent.suggestCommand("/counselor remove nearest"))
                            .append(Component.text(" - 最寄りのNPCを削除", DESC_COLOR))
            );
            player.sendMessage(
                    Component.text("  /counselor remove all", ACCENT)
                            .clickEvent(ClickEvent.suggestCommand("/counselor remove all"))
                            .append(Component.text(" - 全NPCを削除 (注意!)", DESC_COLOR))
            );
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "nearest" -> counselorManager.removeNearest(player);
            case "all" -> {
                counselorManager.removeAll();
                player.sendMessage(Component.text("全ての相談員NPCを削除しました", NamedTextColor.YELLOW));
            }
            default -> player.sendMessage(Component.text("使い方: /counselor remove <nearest|all>", NamedTextColor.RED));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(
                Component.text("  /counselor ヘルプ ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("- NPC管理コマンド", DESC_COLOR)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));

        sendHelpLine(player, "/counselor spawn <方角> <範囲> [性格]", "NPCをスポーン");
        sendHelpLine(player, "/counselor remove nearest", "最寄りのNPCを削除");
        sendHelpLine(player, "/counselor remove all", "全NPCを削除");
        sendHelpLine(player, "/counselor list", "NPC一覧を表示");

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  性格タイプ: ", NamedTextColor.WHITE)
                .append(Component.text("counselor", NamedTextColor.GREEN))
                .append(Component.text(" (相談員) / ", DESC_COLOR))
                .append(Component.text("guard", NamedTextColor.RED))
                .append(Component.text(" (衛兵) / ", DESC_COLOR))
                .append(Component.text("merchant", NamedTextColor.YELLOW))
                .append(Component.text(" (商人) / ", DESC_COLOR))
                .append(Component.text("explorer", NamedTextColor.AQUA))
                .append(Component.text(" (探検家)", DESC_COLOR))
        );

        player.sendMessage(Component.empty());
        player.sendMessage(
                Component.text("  → ", DESC_COLOR)
                        .append(Component.text("/mineai help", ACCENT)
                                .clickEvent(ClickEvent.runCommand("/mineai help"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックで全体ヘルプ", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" で全コマンドのヘルプ", DESC_COLOR))
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
    }

    private void sendHelpLine(Player player, String command, String description) {
        String suggestBase = command.contains("<") ? command.split("<")[0].trim()
                : command.contains("[") ? command.split("\\[")[0].trim()
                : command;
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text(command, ACCENT)
                                .clickEvent(ClickEvent.suggestCommand(suggestBase))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックでコマンド入力", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" - " + description, DESC_COLOR))
        );
    }

    private void sendExample(Player player, String command, String description) {
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("  " + command, ACCENT)
                                .clickEvent(ClickEvent.suggestCommand(command))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックでコマンドをコピー", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" - " + description, DESC_COLOR))
        );
    }
}
