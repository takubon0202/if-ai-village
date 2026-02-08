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
            showMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "menu" -> showMenu(player);
            case "wizard" -> handleWizard(player, args);
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

    // ===== メニュー / ウィザード =====

    private void showMenu(Player player) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(
                Component.text("  NPC管理メニュー", NamedTextColor.GOLD, TextDecoration.BOLD)
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(Component.empty());

        // [スポーン] ボタン
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text(" スポーン ", NamedTextColor.WHITE)
                                .decorate(TextDecoration.BOLD)
                                .color(TextColor.color(0x000000))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("NPCを新しくスポーンします", NamedTextColor.GREEN)
                                ))
                                .clickEvent(ClickEvent.runCommand("/counselor wizard spawn")))
                        .append(Component.text("  "))
                        // [削除] ボタン
                        .append(Component.text(" 削除 ", NamedTextColor.WHITE)
                                .decorate(TextDecoration.BOLD)
                                .color(TextColor.color(0x000000))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("NPCを削除します", NamedTextColor.RED)
                                ))
                                .clickEvent(ClickEvent.runCommand("/counselor wizard remove")))
                        .append(Component.text("  "))
                        // [一覧] ボタン
                        .append(Component.text(" 一覧 ", NamedTextColor.WHITE)
                                .decorate(TextDecoration.BOLD)
                                .color(TextColor.color(0x000000))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("スポーン済みNPCの一覧を表示", NamedTextColor.AQUA)
                                ))
                                .clickEvent(ClickEvent.runCommand("/counselor list")))
        );

        // 見やすいボタン行（色付き角括弧）
        player.sendMessage(Component.empty());
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("[", LINE_COLOR))
                        .append(Component.text("スポーン", NamedTextColor.GREEN)
                                .decorate(TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/counselor wizard spawn"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("NPCを新しくスポーンします", NamedTextColor.GREEN)
                                )))
                        .append(Component.text("]", LINE_COLOR))
                        .append(Component.text("  "))
                        .append(Component.text("[", LINE_COLOR))
                        .append(Component.text("削除", NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/counselor wizard remove"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("NPCを削除します", NamedTextColor.RED)
                                )))
                        .append(Component.text("]", LINE_COLOR))
                        .append(Component.text("  "))
                        .append(Component.text("[", LINE_COLOR))
                        .append(Component.text("一覧", NamedTextColor.AQUA)
                                .decorate(TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/counselor list"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("スポーン済みNPCの一覧を表示", NamedTextColor.AQUA)
                                )))
                        .append(Component.text("]", LINE_COLOR))
        );

        player.sendMessage(Component.empty());
        player.sendMessage(
                Component.text("  → ", DESC_COLOR)
                        .append(Component.text("/counselor help", ACCENT)
                                .clickEvent(ClickEvent.runCommand("/counselor help"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("詳細なヘルプを表示", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" で詳細ヘルプ", DESC_COLOR))
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
    }

    private void handleWizard(Player player, String[] args) {
        if (args.length < 2) {
            showMenu(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "spawn" -> {
                if (args.length == 2) {
                    showWizardSpawnDirection(player);
                } else if (args.length == 3) {
                    showWizardSpawnRange(player, args[2].toLowerCase());
                } else if (args.length == 4) {
                    showWizardSpawnPersonality(player, args[2].toLowerCase(), args[3]);
                }
            }
            case "remove" -> showWizardRemove(player);
            default -> showMenu(player);
        }
    }

    private void showWizardSpawnDirection(Player player) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(
                Component.text("  スポーン (1/3)", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(" - 方角を選択", DESC_COLOR)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(Component.empty());

        Component buttons = Component.text("  ");
        String[] directions = {"north", "south", "east", "west"};
        for (String dir : directions) {
            buttons = buttons
                    .append(createDirectionButton(dir))
                    .append(Component.text("  "));
        }
        player.sendMessage(buttons);

        player.sendMessage(Component.empty());
        sendBackButton(player, "/counselor menu");
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
    }

    private void showWizardSpawnRange(Player player, String direction) {
        if (!List.of("north", "south", "east", "west").contains(direction)) {
            player.sendMessage(Component.text("不正な方角です。", NamedTextColor.RED));
            showWizardSpawnDirection(player);
            return;
        }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(
                Component.text("  スポーン (2/3)", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(" - 範囲を選択", DESC_COLOR)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(
                Component.text("  方角: ", DESC_COLOR)
                        .append(Component.text(getDirectionJP(direction), NamedTextColor.YELLOW))
        );
        player.sendMessage(Component.empty());

        Component buttons = Component.text("  ");
        int[] ranges = {3, 5, 10, 15, 20, 30, 50};
        for (int r : ranges) {
            buttons = buttons
                    .append(createRangeButton(direction, r))
                    .append(Component.text(" "));
        }
        player.sendMessage(buttons);

        player.sendMessage(Component.empty());
        sendBackButton(player, "/counselor wizard spawn");
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
    }

    private void showWizardSpawnPersonality(Player player, String direction, String range) {
        if (!List.of("north", "south", "east", "west").contains(direction)) {
            player.sendMessage(Component.text("不正な方角です。", NamedTextColor.RED));
            showWizardSpawnDirection(player);
            return;
        }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(
                Component.text("  スポーン (3/3)", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .append(Component.text(" - 性格を選択", DESC_COLOR)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(
                Component.text("  方角: ", DESC_COLOR)
                        .append(Component.text(getDirectionJP(direction), NamedTextColor.YELLOW))
                        .append(Component.text("  範囲: ", DESC_COLOR))
                        .append(Component.text(range + "ブロック", NamedTextColor.YELLOW))
        );
        player.sendMessage(Component.empty());

        String[] personalities = {"counselor", "guard", "merchant", "explorer"};
        for (String p : personalities) {
            player.sendMessage(
                    Component.text("  ")
                            .append(createPersonalityButton(direction, range, p))
            );
        }

        player.sendMessage(Component.empty());
        sendBackButton(player, "/counselor wizard spawn " + direction);
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
    }

    private void showWizardRemove(Player player) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(
                Component.text("  NPC削除", NamedTextColor.RED, TextDecoration.BOLD)
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
        player.sendMessage(Component.empty());

        // [最寄り削除]
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("[", LINE_COLOR))
                        .append(Component.text("最寄りのNPCを削除", NamedTextColor.YELLOW)
                                .decorate(TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/counselor remove nearest"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("最も近いNPCを1体削除します", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text("]", LINE_COLOR))
        );
        player.sendMessage(Component.empty());

        // [全NPC削除]
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("[", LINE_COLOR))
                        .append(Component.text("全NPCを削除", NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/counselor remove all"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("全てのNPCを削除します (注意!)", NamedTextColor.RED)
                                )))
                        .append(Component.text("]", LINE_COLOR))
        );

        player.sendMessage(Component.empty());
        sendBackButton(player, "/counselor menu");
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR));
    }

    // ===== ウィザードヘルパーメソッド =====

    private Component createDirectionButton(String direction) {
        return Component.text("[", LINE_COLOR)
                .append(Component.text(getDirectionJP(direction), NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/counselor wizard spawn " + direction))
                        .hoverEvent(HoverEvent.showText(
                                Component.text(getDirectionJP(direction) + "方向にスポーン", NamedTextColor.YELLOW)
                        )))
                .append(Component.text("]", LINE_COLOR));
    }

    private Component createRangeButton(String direction, int range) {
        return Component.text("[", LINE_COLOR)
                .append(Component.text(String.valueOf(range), NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/counselor wizard spawn " + direction + " " + range))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("パトロール範囲: " + range + "ブロック", NamedTextColor.AQUA)
                        )))
                .append(Component.text("]", LINE_COLOR));
    }

    private Component createPersonalityButton(String direction, String range, String personality) {
        TextColor color = getPersonalityColor(personality);
        String jpName = getPersonalityJP(personality);
        String desc = getPersonalityDescription(personality);

        return Component.text("[", LINE_COLOR)
                .append(Component.text(jpName, color)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/counselor spawn " + direction + " " + range + " " + personality))
                        .hoverEvent(HoverEvent.showText(
                                Component.text(jpName + " - " + desc, color)
                        )))
                .append(Component.text("]", LINE_COLOR))
                .append(Component.text(" " + desc, DESC_COLOR));
    }

    private String getDirectionJP(String direction) {
        return switch (direction) {
            case "north" -> "北";
            case "south" -> "南";
            case "east" -> "東";
            case "west" -> "西";
            default -> direction;
        };
    }

    private String getPersonalityJP(String personality) {
        return switch (personality) {
            case "counselor" -> "相談員";
            case "guard" -> "衛兵";
            case "merchant" -> "商人";
            case "explorer" -> "探検家";
            default -> personality;
        };
    }

    private TextColor getPersonalityColor(String personality) {
        return switch (personality) {
            case "counselor" -> NamedTextColor.GREEN;
            case "guard" -> NamedTextColor.RED;
            case "merchant" -> NamedTextColor.YELLOW;
            case "explorer" -> NamedTextColor.AQUA;
            default -> NamedTextColor.WHITE;
        };
    }

    private String getPersonalityDescription(String personality) {
        return switch (personality) {
            case "counselor" -> "優しく相談に乗ってくれます";
            case "guard" -> "ワールドの安全を守ります";
            case "merchant" -> "取引や商売の話をします";
            case "explorer" -> "冒険や探検が大好きです";
            default -> "";
        };
    }

    private void sendBackButton(Player player, String backCommand) {
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("[", LINE_COLOR))
                        .append(Component.text("← 戻る", DESC_COLOR)
                                .clickEvent(ClickEvent.runCommand(backCommand))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("前の画面に戻る", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text("]", LINE_COLOR))
        );
    }

    // ===== ヘルプ =====

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
