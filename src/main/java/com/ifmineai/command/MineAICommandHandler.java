package com.ifmineai.command;

import com.ifmineai.IFMineAIPlugin;
import com.ifmineai.ai.AIBrainManager;
import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.NPCState;
import com.ifmineai.config.AIConfig;

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

import java.util.Map;
import java.util.UUID;

public class MineAICommandHandler implements CommandExecutor {

    private static final TextColor ACCENT = TextColor.color(0x55FFFF);
    private static final TextColor GOLD = TextColor.color(0xFFAA00);
    private static final TextColor LINE_COLOR = TextColor.color(0x555555);
    private static final TextColor DESC_COLOR = TextColor.color(0xAAAAAA);
    private static final TextColor HEADER_COLOR = TextColor.color(0xFFFF55);

    private final IFMineAIPlugin plugin;

    public MineAICommandHandler(IFMineAIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行できます", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> showHelp(player, args);
            case "guide" -> showGuide(player);
            case "setup" -> showSetup(player);
            case "about" -> showAbout(player);
            case "commands" -> showAllCommands(player);
            default -> showMainMenu(player);
        }

        return true;
    }

    private void showMainMenu(Player player) {
        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR);

        player.sendMessage(Component.empty());
        player.sendMessage(line);
        player.sendMessage(
                Component.text("  IF MineAI ", GOLD, TextDecoration.BOLD)
                        .append(Component.text("- 総合メニュー", DESC_COLOR)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(line);

        // AI状態サマリー
        showStatusSummary(player);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  --- メニュー ---", HEADER_COLOR));

        sendMenuEntry(player, "/mineai help", "全コマンドのヘルプを表示", "/mineai help");
        sendMenuEntry(player, "/mineai guide", "初心者向けスタートガイド", "/mineai guide");
        sendMenuEntry(player, "/mineai setup", "AI初期セットアップ手順", "/mineai setup");
        sendMenuEntry(player, "/mineai commands", "全コマンド一覧", "/mineai commands");
        sendMenuEntry(player, "/mineai about", "プラグイン情報", "/mineai about");

        player.sendMessage(line);
        player.sendMessage(Component.empty());
    }

    private void showStatusSummary(Player player) {
        AIConfig config = plugin.getAIConfig();
        AIBrainManager brainManager = plugin.getAIBrainManager();
        boolean aiRunning = brainManager != null && brainManager.isEnabled();
        int npcCount = plugin.getCounselorManager() != null
                ? plugin.getCounselorManager().getCounselors().size() : 0;

        player.sendMessage(Component.empty());

        // AI状態
        Component aiLine = Component.text("  AI: ", NamedTextColor.WHITE);
        if (aiRunning) {
            aiLine = aiLine.append(Component.text("稼働中", NamedTextColor.GREEN, TextDecoration.BOLD));

            Map<UUID, NPCBrain> brains = brainManager.getBrains();
            long idle = brains.values().stream().filter(b -> b.getState() == NPCState.IDLE).count();
            long walking = brains.values().stream().filter(b -> b.getState() == NPCState.WALKING).count();
            long talking = brains.values().stream().filter(b -> b.getState() == NPCState.TALKING).count();

            aiLine = aiLine.append(Component.text(
                    " (AI NPC: " + brains.size() + "体"
                            + " - 待機:" + idle + " 歩行:" + walking + " 会話:" + talking + ")",
                    DESC_COLOR
            ).decoration(TextDecoration.BOLD, false));
        } else {
            aiLine = aiLine.append(Component.text("停止中", NamedTextColor.RED));
            if (!config.isEnabledFlag()) {
                aiLine = aiLine.append(Component.text(" (未有効化)", DESC_COLOR));
            } else if (!config.isApiKeyValid()) {
                aiLine = aiLine.append(Component.text(" (APIキー未設定)", DESC_COLOR));
            }
        }
        player.sendMessage(aiLine);

        // NPC数
        player.sendMessage(
                Component.text("  NPC: ", NamedTextColor.WHITE)
                        .append(Component.text(npcCount + "体", npcCount > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY))
        );
    }

    private void showHelp(Player player, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR);

        switch (page) {
            case 1 -> {
                player.sendMessage(Component.empty());
                player.sendMessage(line);
                player.sendMessage(
                        Component.text("  ヘルプ ", GOLD, TextDecoration.BOLD)
                                .append(Component.text("- NPCの管理 (1/3)", DESC_COLOR)
                                        .decoration(TextDecoration.BOLD, false))
                );
                player.sendMessage(line);

                player.sendMessage(Component.text("  --- NPC管理コマンド (/counselor) ---", HEADER_COLOR));

                sendHelpEntry(player, "/counselor spawn <方角> <範囲> [性格]",
                        "NPCを現在地にスポーンします",
                        "方角: north/south/east/west\n範囲: パトロールの距離 (1~50)\n性格: counselor/guard/merchant/explorer",
                        "/counselor spawn ");

                sendHelpEntry(player, "/counselor remove nearest",
                        "最寄りのNPCを削除します",
                        "最も近いNPCを1体だけ削除します\n取り消せないので注意!",
                        "/counselor remove nearest");

                sendHelpEntry(player, "/counselor remove all",
                        "全NPCを削除します",
                        "全ての相談員NPCを一括削除します\n取り消せないので注意!",
                        "/counselor remove all");

                sendHelpEntry(player, "/counselor list",
                        "全NPCの一覧を表示します",
                        "NPCの座標・方角・範囲を表示",
                        "/counselor list");

                player.sendMessage(Component.empty());
                sendPageNav(player, 1, 3);
                player.sendMessage(line);
            }
            case 2 -> {
                player.sendMessage(Component.empty());
                player.sendMessage(line);
                player.sendMessage(
                        Component.text("  ヘルプ ", GOLD, TextDecoration.BOLD)
                                .append(Component.text("- AI管理 (2/3)", DESC_COLOR)
                                        .decoration(TextDecoration.BOLD, false))
                );
                player.sendMessage(line);

                player.sendMessage(Component.text("  --- AI管理コマンド (/ainpc) ---", HEADER_COLOR));

                sendHelpEntry(player, "/ainpc status",
                        "AI状態を表示します",
                        "APIキー・有効状態・NPC数・モデル情報を表示",
                        "/ainpc status");

                sendHelpEntry(player, "/ainpc reload",
                        "設定をリロードします",
                        "config.ymlの変更を反映します\nAIの再初期化も行います",
                        "/ainpc reload");

                player.sendMessage(Component.empty());
                sendPageNav(player, 2, 3);
                player.sendMessage(line);
            }
            case 3 -> {
                player.sendMessage(Component.empty());
                player.sendMessage(line);
                player.sendMessage(
                        Component.text("  ヘルプ ", GOLD, TextDecoration.BOLD)
                                .append(Component.text("- デバッグ・その他 (3/3)", DESC_COLOR)
                                        .decoration(TextDecoration.BOLD, false))
                );
                player.sendMessage(line);

                player.sendMessage(Component.text("  --- デバッグ・情報コマンド ---", HEADER_COLOR));

                sendHelpEntry(player, "/ainpc debug",
                        "全AI NPCのデバッグ情報",
                        "各NPCの状態・キュー・性格を表示",
                        "/ainpc debug");

                sendHelpEntry(player, "/ainpc debug <UUID>",
                        "特定NPCの詳細デバッグ",
                        "UUID指定でNPCの詳細情報を表示\n記憶数・アクション・会話相手など",
                        "/ainpc debug ");

                sendHelpEntry(player, "/ainpc personality",
                        "性格プロファイル一覧",
                        "利用可能な性格タイプと特性値を表示",
                        "/ainpc personality");

                sendHelpEntry(player, "/ainpc stats",
                        "AI NPC統計情報",
                        "全NPCの状態別集計を表示",
                        "/ainpc stats");

                player.sendMessage(Component.empty());

                player.sendMessage(Component.text("  --- 総合コマンド (/mineai) ---", HEADER_COLOR));

                sendHelpEntry(player, "/mineai",
                        "総合メニューを表示",
                        "メインメニューを開きます",
                        "/mineai");

                sendHelpEntry(player, "/mineai guide",
                        "初心者ガイド",
                        "プラグインの使い方を順番に説明します",
                        "/mineai guide");

                sendHelpEntry(player, "/mineai setup",
                        "AI初期セットアップ",
                        "AIを使い始めるための手順を表示",
                        "/mineai setup");

                player.sendMessage(Component.empty());
                sendPageNav(player, 3, 3);
                player.sendMessage(line);
            }
            default -> showHelp(player, new String[]{"help", "1"});
        }
    }

    private void showGuide(Player player) {
        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR);

        player.sendMessage(Component.empty());
        player.sendMessage(line);
        player.sendMessage(
                Component.text("  初心者ガイド ", GOLD, TextDecoration.BOLD)
                        .append(Component.text("- はじめての IF MineAI", DESC_COLOR)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(line);
        player.sendMessage(Component.empty());

        // Step 1
        player.sendMessage(Component.text("  Step 1: NPCをスポーンする", HEADER_COLOR, TextDecoration.BOLD));
        player.sendMessage(Component.text("  NPCを設置したい場所に立ち、コマンドを実行:", DESC_COLOR));
        sendClickableCmd(player, "/counselor spawn north 10");
        player.sendMessage(Component.text("  → 北方向に10ブロックの範囲でNPCが出現します", DESC_COLOR));
        player.sendMessage(Component.empty());

        // Step 2
        player.sendMessage(Component.text("  Step 2: NPCに話しかける", HEADER_COLOR, TextDecoration.BOLD));
        player.sendMessage(Component.text("  NPCを右クリックすると会話モードに入ります", DESC_COLOR));
        player.sendMessage(Component.text("  チャット欄にメッセージを入力するとNPCが応答します", DESC_COLOR));
        player.sendMessage(Component.text("  もう一度右クリックで会話終了", DESC_COLOR));
        player.sendMessage(Component.empty());

        // Step 3
        player.sendMessage(Component.text("  Step 3: AIを有効にする (上級)", HEADER_COLOR, TextDecoration.BOLD));
        player.sendMessage(Component.text("  AIを有効にするとNPCが自律的に行動・会話します!", DESC_COLOR));
        player.sendMessage(
                Component.text("  → ", DESC_COLOR)
                        .append(Component.text("/mineai setup", ACCENT)
                                .clickEvent(ClickEvent.runCommand("/mineai setup"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックでセットアップ手順を表示", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" で手順を確認", DESC_COLOR))
        );
        player.sendMessage(Component.empty());

        // Tips
        player.sendMessage(Component.text("  --- 便利なTips ---", HEADER_COLOR));
        player.sendMessage(Component.text("  - 方角は north/south/east/west の4方向", DESC_COLOR));
        player.sendMessage(Component.text("  - 性格は counselor/guard/merchant/explorer の4種類", DESC_COLOR));
        player.sendMessage(Component.text("  - NPCは不死身なのでMobに攻撃されません", DESC_COLOR));
        player.sendMessage(Component.text("  - サーバー再起動後もNPCは自動復元されます", DESC_COLOR));

        player.sendMessage(line);
        player.sendMessage(Component.empty());
    }

    private void showSetup(Player player) {
        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR);

        player.sendMessage(Component.empty());
        player.sendMessage(line);
        player.sendMessage(
                Component.text("  AI セットアップ ", GOLD, TextDecoration.BOLD)
                        .append(Component.text("- Gemini AI を有効にする", DESC_COLOR)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(line);
        player.sendMessage(Component.empty());

        AIConfig config = plugin.getAIConfig();
        boolean hasKey = config.isApiKeyValid();
        boolean enabled = config.isEnabledFlag();
        boolean running = plugin.getAIBrainManager() != null && plugin.getAIBrainManager().isEnabled();

        // Step 1: APIキー取得
        Component step1Status = hasKey
                ? Component.text(" [完了]", NamedTextColor.GREEN)
                : Component.text(" [未完了]", NamedTextColor.RED);
        player.sendMessage(
                Component.text("  1. Google AI StudioでAPIキーを取得", HEADER_COLOR, TextDecoration.BOLD)
                        .append(step1Status.decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(
                Component.text("     → ", DESC_COLOR)
                        .append(Component.text("https://aistudio.google.com/apikey", TextColor.color(0x6699FF))
                                .clickEvent(ClickEvent.openUrl("https://aistudio.google.com/apikey"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックでブラウザを開く", NamedTextColor.YELLOW)
                                )))
        );
        player.sendMessage(Component.text("     無料で取得できます (Google アカウントが必要)", DESC_COLOR));
        player.sendMessage(Component.empty());

        // Step 2: config.yml編集
        Component step2Status = (hasKey && enabled)
                ? Component.text(" [完了]", NamedTextColor.GREEN)
                : Component.text(" [未完了]", NamedTextColor.RED);
        player.sendMessage(
                Component.text("  2. config.yml を編集してAIを有効化", HEADER_COLOR, TextDecoration.BOLD)
                        .append(step2Status.decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(Component.text("     plugins/IFMineAI/config.yml を開き:", DESC_COLOR));
        player.sendMessage(Component.text("     - ai.gemini-api-key に取得したAPIキーを貼り付け", DESC_COLOR));
        player.sendMessage(Component.text("     - ai.enabled を true に変更", DESC_COLOR));
        player.sendMessage(Component.empty());

        // Step 3: サーバー再起動
        Component step3Status = running
                ? Component.text(" [完了]", NamedTextColor.GREEN)
                : Component.text(" [未完了]", NamedTextColor.RED);
        player.sendMessage(
                Component.text("  3. サーバーを再起動して設定を反映", HEADER_COLOR, TextDecoration.BOLD)
                        .append(step3Status.decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(Component.text("     サーバーを再起動、または /ainpc reload を実行", DESC_COLOR));
        player.sendMessage(Component.empty());

        // Step 4: NPCスポーン
        player.sendMessage(
                Component.text("  4. AI NPCをスポーン!", HEADER_COLOR, TextDecoration.BOLD)
        );
        sendClickableCmd(player, "/counselor spawn north 10 counselor");
        player.sendMessage(Component.text("     NPCが自律的に歩き回り、近づくと挨拶します!", DESC_COLOR));

        player.sendMessage(Component.empty());
        player.sendMessage(line);
        player.sendMessage(Component.empty());
    }

    private void showAbout(Player player) {
        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR);

        player.sendMessage(Component.empty());
        player.sendMessage(line);
        player.sendMessage(
                Component.text("  IF MineAI ", GOLD, TextDecoration.BOLD)
                        .append(Component.text("v" + plugin.getPluginMeta().getVersion(), DESC_COLOR)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(line);
        player.sendMessage(Component.text("  AI搭載NPCプラグイン for Minecraft", DESC_COLOR));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  機能:", HEADER_COLOR));
        player.sendMessage(Component.text("  - Gemini AIによる自律行動・自然な会話", DESC_COLOR));
        player.sendMessage(Component.text("  - Pathfinder APIによる自然な歩行", DESC_COLOR));
        player.sendMessage(Component.text("  - 4種類の性格 (相談員/衛兵/商人/探検家)", DESC_COLOR));
        player.sendMessage(Component.text("  - NPCの記憶システム (プレイヤーを覚える)", DESC_COLOR));
        player.sendMessage(Component.text("  - AI無効時は従来のパトロールモードで動作", DESC_COLOR));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  動作環境:", HEADER_COLOR));
        player.sendMessage(Component.text("  - Paper 1.21+ / Java 21", DESC_COLOR));
        player.sendMessage(Component.text("  - AI使用時: Gemini API (無料枠あり)", DESC_COLOR));
        player.sendMessage(Component.empty());
        player.sendMessage(line);
        player.sendMessage(Component.empty());
    }

    private void showAllCommands(Player player) {
        Component line = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LINE_COLOR);

        player.sendMessage(Component.empty());
        player.sendMessage(line);
        player.sendMessage(
                Component.text("  全コマンド一覧 ", GOLD, TextDecoration.BOLD)
        );
        player.sendMessage(line);
        player.sendMessage(Component.empty());

        // /mineai 系
        player.sendMessage(Component.text("  --- /mineai (総合) ---", HEADER_COLOR));
        sendCmdLine(player, "/mineai", "メインメニュー");
        sendCmdLine(player, "/mineai help [ページ]", "ヘルプ (1~3ページ)");
        sendCmdLine(player, "/mineai guide", "初心者ガイド");
        sendCmdLine(player, "/mineai setup", "AIセットアップ手順");
        sendCmdLine(player, "/mineai commands", "この一覧を表示");
        sendCmdLine(player, "/mineai about", "プラグイン情報");
        player.sendMessage(Component.empty());

        // /counselor 系
        player.sendMessage(Component.text("  --- /counselor (NPC管理) ---", HEADER_COLOR));
        sendCmdLine(player, "/counselor spawn <方角> <範囲> [性格]", "NPCスポーン");
        sendCmdLine(player, "/counselor remove nearest", "最寄NPC削除");
        sendCmdLine(player, "/counselor remove all", "全NPC削除");
        sendCmdLine(player, "/counselor list", "NPC一覧");
        player.sendMessage(Component.empty());

        // /ainpc 系
        if (player.hasPermission("ifmineai.ai.admin")) {
            player.sendMessage(Component.text("  --- /ainpc (AI管理) ---", HEADER_COLOR));
            sendCmdLine(player, "/ainpc status", "AI状態表示");
            sendCmdLine(player, "/ainpc reload", "設定リロード");
            sendCmdLine(player, "/ainpc debug [UUID]", "デバッグ情報");
            sendCmdLine(player, "/ainpc personality", "性格一覧");
            sendCmdLine(player, "/ainpc stats", "統計情報");
        }

        player.sendMessage(line);
        player.sendMessage(Component.empty());
    }

    // --- UI Helper ---

    private void sendMenuEntry(Player player, String command, String description, String runCommand) {
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text("> ", GOLD))
                        .append(Component.text(command, ACCENT)
                                .clickEvent(ClickEvent.runCommand(runCommand))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックで実行", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" - " + description, DESC_COLOR))
        );
    }

    private void sendHelpEntry(Player player, String command, String summary,
                                String tooltip, String suggestCmd) {
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text(command, ACCENT)
                                .clickEvent(ClickEvent.suggestCommand(suggestCmd))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text(command, NamedTextColor.WHITE)
                                                .append(Component.newline())
                                                .append(Component.text(tooltip, NamedTextColor.GRAY))
                                )))
        );
        player.sendMessage(Component.text("    " + summary, DESC_COLOR));
    }

    private void sendClickableCmd(Player player, String command) {
        player.sendMessage(
                Component.text("     ")
                        .append(Component.text(command, ACCENT)
                                .clickEvent(ClickEvent.suggestCommand(command.contains("<") ? command.split("<")[0] : command))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックでコマンドを入力欄にコピー", NamedTextColor.YELLOW)
                                )))
        );
    }

    private void sendCmdLine(Player player, String command, String description) {
        String suggestBase = command.contains("[") ? command.split("\\[")[0].trim()
                : command.contains("<") ? command.split("<")[0].trim()
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

    private void sendPageNav(Player player, int current, int total) {
        Component nav = Component.text("  ");

        if (current > 1) {
            nav = nav.append(
                    Component.text("[< 前へ]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/mineai help " + (current - 1)))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("ページ " + (current - 1) + " を表示", NamedTextColor.YELLOW)
                            ))
            );
        } else {
            nav = nav.append(Component.text("[< 前へ]", NamedTextColor.DARK_GRAY));
        }

        nav = nav.append(Component.text(" ページ " + current + "/" + total + " ", DESC_COLOR));

        if (current < total) {
            nav = nav.append(
                    Component.text("[次へ >]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/mineai help " + (current + 1)))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("ページ " + (current + 1) + " を表示", NamedTextColor.YELLOW)
                            ))
            );
        } else {
            nav = nav.append(Component.text("[次へ >]", NamedTextColor.DARK_GRAY));
        }

        player.sendMessage(nav);
    }
}
