package com.ifmineai.command;

import com.ifmineai.IFMineAIPlugin;
import com.ifmineai.ai.AIBrainManager;
import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.NPCState;
import com.ifmineai.ai.personality.PersonalityProfile;
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

public class AICommandHandler implements CommandExecutor {

    private final AIBrainManager brainManager;
    private final AIConfig aiConfig;
    private final IFMineAIPlugin plugin;

    public AICommandHandler(AIBrainManager brainManager, AIConfig aiConfig, IFMineAIPlugin plugin) {
        this.brainManager = brainManager;
        this.aiConfig = aiConfig;
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
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "status" -> handleStatus(player);
            case "debug" -> handleDebug(player, args);
            case "personality" -> handlePersonality(player, args);
            case "reload" -> handleReload(player);
            case "stats" -> handleStats(player);
            case "setkey" -> handleSetKey(player, args);
            case "enable" -> handleEnable(player);
            case "disable" -> handleDisable(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleSetKey(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("使い方: /ainpc setkey <Gemini APIキー>", NamedTextColor.RED));
            player.sendMessage(Component.text("APIキー取得先: https://aistudio.google.com/apikey", NamedTextColor.GRAY));
            return;
        }

        String key = args[1];

        if (!AIConfig.validateApiKeyFormat(key)) {
            player.sendMessage(Component.text("APIキーの形式が不正です", NamedTextColor.RED));
            player.sendMessage(Component.text("Google AI Studioで取得した「AIza」で始まるキーを入力してください", NamedTextColor.GRAY));
            return;
        }

        if (aiConfig.setApiKey(key)) {
            // キーをマスク表示
            String masked = key.substring(0, 8) + "..." + key.substring(key.length() - 4);
            player.sendMessage(Component.text("APIキーを設定しました: " + masked, NamedTextColor.GREEN));

            if (!aiConfig.isEnabledFlag()) {
                player.sendMessage(Component.text("AIを有効にするには /ainpc enable を実行してください", NamedTextColor.YELLOW));
            } else if (!brainManager.isEnabled()) {
                player.sendMessage(Component.text("設定を反映するには /ainpc reload を実行してください", NamedTextColor.YELLOW));
            }
        } else {
            player.sendMessage(Component.text("APIキーの保存に失敗しました", NamedTextColor.RED));
        }
    }

    private void handleEnable(Player player) {
        if (brainManager.isEnabled()) {
            player.sendMessage(Component.text("AIは既に有効です", NamedTextColor.YELLOW));
            return;
        }

        if (!aiConfig.isApiKeyValid()) {
            player.sendMessage(Component.text("先にAPIキーを設定してください: /ainpc setkey <キー>", NamedTextColor.RED));
            return;
        }

        aiConfig.setAiEnabled(true);
        player.sendMessage(Component.text("AIを有効にしました", NamedTextColor.GREEN));
        player.sendMessage(Component.text("反映するには /ainpc reload を実行してください", NamedTextColor.YELLOW));
    }

    private void handleDisable(Player player) {
        if (!aiConfig.isEnabledFlag()) {
            player.sendMessage(Component.text("AIは既に無効です", NamedTextColor.YELLOW));
            return;
        }

        aiConfig.setAiEnabled(false);
        player.sendMessage(Component.text("AIを無効にしました。NPCは従来のパトロールモードで動作します", NamedTextColor.GREEN));
        player.sendMessage(Component.text("反映するには /ainpc reload を実行してください", NamedTextColor.YELLOW));
    }

    private void handleStatus(Player player) {
        player.sendMessage(Component.text("=== AI NPC ステータス ===", NamedTextColor.GOLD));

        // AI有効/無効
        boolean running = brainManager.isEnabled();
        player.sendMessage(Component.text("AI稼働: ", NamedTextColor.WHITE)
                .append(Component.text(running ? "はい" : "いいえ",
                        running ? NamedTextColor.GREEN : NamedTextColor.RED)));

        // config上のenable
        player.sendMessage(Component.text("ai.enabled: ", NamedTextColor.WHITE)
                .append(Component.text(String.valueOf(aiConfig.isEnabledFlag()),
                        aiConfig.isEnabledFlag() ? NamedTextColor.GREEN : NamedTextColor.RED)));

        // APIキー状態
        if (aiConfig.isApiKeyValid()) {
            String key = aiConfig.getGeminiApiKey();
            String masked = key.substring(0, 8) + "..." + key.substring(key.length() - 4);
            player.sendMessage(Component.text("APIキー: ", NamedTextColor.WHITE)
                    .append(Component.text(masked, NamedTextColor.GREEN)));
        } else if (!aiConfig.getGeminiApiKey().isEmpty()) {
            player.sendMessage(Component.text("APIキー: ", NamedTextColor.WHITE)
                    .append(Component.text("形式不正", NamedTextColor.RED)));
        } else {
            player.sendMessage(Component.text("APIキー: ", NamedTextColor.WHITE)
                    .append(Component.text("未設定 (/ainpc setkey <キー>)", NamedTextColor.RED)));
        }

        // ブレイン数
        player.sendMessage(Component.text("アクティブ頭脳数: ", NamedTextColor.WHITE)
                .append(Component.text(String.valueOf(brainManager.getBrains().size()), NamedTextColor.AQUA)));

        // モデル
        player.sendMessage(Component.text("モデル(行動): ", NamedTextColor.WHITE)
                .append(Component.text(aiConfig.getGeminiFlashModel(), NamedTextColor.GRAY)));
        player.sendMessage(Component.text("モデル(会話): ", NamedTextColor.WHITE)
                .append(Component.text(aiConfig.getGeminiProModel(), NamedTextColor.GRAY)));
    }

    private void handleDebug(Player player, String[] args) {
        if (args.length < 2) {
            Map<UUID, NPCBrain> brains = brainManager.getBrains();
            if (brains.isEmpty()) {
                player.sendMessage(Component.text("アクティブなAI NPCはいません", NamedTextColor.YELLOW));
                return;
            }

            player.sendMessage(Component.text("=== AI NPC デバッグ ===", NamedTextColor.GOLD));
            for (NPCBrain brain : brains.values()) {
                String entityName = brain.getNpcEntity() != null ? brain.getNpcEntity().getName() : "不明";
                player.sendMessage(Component.text(
                        entityName + " [" + brain.getState() + "] "
                                + "キュー:" + brain.getActionQueueSize()
                                + " AI待ち:" + brain.isAwaitingAIResponse()
                                + " 性格:" + brain.getData().getPersonalityType(),
                        NamedTextColor.WHITE
                ));
            }
            return;
        }

        try {
            UUID uuid = UUID.fromString(args[1]);
            NPCBrain brain = brainManager.getBrain(uuid);
            if (brain == null) {
                player.sendMessage(Component.text("指定UUIDのAI NPCが見つかりません", NamedTextColor.RED));
                return;
            }

            player.sendMessage(Component.text("=== AI NPC 詳細 ===", NamedTextColor.GOLD));
            player.sendMessage(Component.text("UUID: " + uuid, NamedTextColor.GRAY));
            player.sendMessage(Component.text("状態: " + brain.getState(), NamedTextColor.WHITE));
            player.sendMessage(Component.text("性格: " + brain.getData().getPersonalityType(), NamedTextColor.WHITE));
            player.sendMessage(Component.text("アクションキュー: " + brain.getActionQueueSize(), NamedTextColor.WHITE));
            player.sendMessage(Component.text("AI応答待ち: " + brain.isAwaitingAIResponse(), NamedTextColor.WHITE));
            player.sendMessage(Component.text("会話相手: " + (brain.getConversationPartner() != null ? brain.getConversationPartner() : "なし"), NamedTextColor.WHITE));
            player.sendMessage(Component.text("最終判断からのtick: " + brain.getTicksSinceLastDecision(), NamedTextColor.WHITE));

            if (brain.getCurrentAction() != null) {
                player.sendMessage(Component.text("現在のアクション: " + brain.getCurrentAction().describe(), NamedTextColor.AQUA));
            }

            int memCount = brainManager.getMemoryStore() != null ?
                    brainManager.getMemoryStore().getMemoryCount(uuid) : 0;
            player.sendMessage(Component.text("記憶数: " + memCount, NamedTextColor.WHITE));

        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("無効なUUID形式です", NamedTextColor.RED));
        }
    }

    private void handlePersonality(Player player, String[] args) {
        if (brainManager.getPersonalityLoader() == null) {
            player.sendMessage(Component.text("AI システムが初期化されていません", NamedTextColor.RED));
            return;
        }

        Map<String, PersonalityProfile> profiles = brainManager.getPersonalityLoader().getAllProfiles();
        player.sendMessage(Component.text("=== 性格プロファイル (" + profiles.size() + "件) ===", NamedTextColor.GOLD));
        for (PersonalityProfile p : profiles.values()) {
            player.sendMessage(Component.text(
                    p.type() + " (" + p.displayName() + "): " + p.description(),
                    NamedTextColor.WHITE
            ));
            player.sendMessage(Component.text(
                    "  親切:" + p.friendliness() + " 好奇心:" + p.curiosity()
                            + " 社交性:" + p.sociability(),
                    NamedTextColor.GRAY
            ));
        }
    }

    private void handleReload(Player player) {
        // 既存のAIを停止
        brainManager.shutdown();

        // 設定リロード
        aiConfig.reload();
        aiConfig.logStartupStatus(plugin.getLogger());

        // AI再初期化
        brainManager.initialize();

        if (brainManager.getPersonalityLoader() != null) {
            brainManager.getPersonalityLoader().load();
        }

        if (brainManager.isEnabled()) {
            player.sendMessage(Component.text("AI設定をリロードしました (AI: 有効)", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("AI設定をリロードしました (AI: 無効)", NamedTextColor.YELLOW));
        }
    }

    private void handleStats(Player player) {
        player.sendMessage(Component.text("=== AI NPC 統計 ===", NamedTextColor.GOLD));

        Map<UUID, NPCBrain> brains = brainManager.getBrains();
        int total = brains.size();
        long idle = brains.values().stream().filter(b -> b.getState() == NPCState.IDLE).count();
        long walking = brains.values().stream().filter(b -> b.getState() == NPCState.WALKING).count();
        long talking = brains.values().stream().filter(b -> b.getState() == NPCState.TALKING).count();
        long thinking = brains.values().stream().filter(b -> b.getState() == NPCState.THINKING).count();

        player.sendMessage(Component.text("合計: " + total + "体", NamedTextColor.WHITE));
        player.sendMessage(Component.text("待機中: " + idle + " / 歩行中: " + walking
                + " / 会話中: " + talking + " / 思考中: " + thinking, NamedTextColor.GRAY));
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("不明なサブコマンドです。", NamedTextColor.RED)
                .append(Component.text(" /ainpc help", TextColor.color(0x55FFFF))
                        .clickEvent(ClickEvent.runCommand("/ainpc help"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("クリックでヘルプを表示", NamedTextColor.YELLOW)
                        )))
                .append(Component.text(" でヘルプを表示", NamedTextColor.RED)));
    }

    private void sendHelp(Player player) {
        TextColor accent = TextColor.color(0x55FFFF);
        TextColor desc = TextColor.color(0xAAAAAA);

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", TextColor.color(0x555555)));
        player.sendMessage(
                Component.text("  /ainpc ヘルプ ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("- AI管理コマンド", desc)
                                .decoration(TextDecoration.BOLD, false))
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", TextColor.color(0x555555)));

        sendAinpcHelpLine(player, "/ainpc setkey <APIキー>", "Gemini APIキーを設定", accent, desc);
        sendAinpcHelpLine(player, "/ainpc enable", "AIを有効化", accent, desc);
        sendAinpcHelpLine(player, "/ainpc disable", "AIを無効化 (パトロールに戻る)", accent, desc);
        sendAinpcHelpLine(player, "/ainpc status", "AI状態・APIキー・NPC数を表示", accent, desc);
        sendAinpcHelpLine(player, "/ainpc reload", "設定リロード・AI再初期化", accent, desc);
        sendAinpcHelpLine(player, "/ainpc debug [UUID]", "NPCデバッグ情報", accent, desc);
        sendAinpcHelpLine(player, "/ainpc personality", "性格プロファイル一覧", accent, desc);
        sendAinpcHelpLine(player, "/ainpc stats", "NPC統計 (状態別集計)", accent, desc);

        player.sendMessage(Component.empty());
        player.sendMessage(
                Component.text("  → ", desc)
                        .append(Component.text("/mineai help", accent)
                                .clickEvent(ClickEvent.runCommand("/mineai help"))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックで全体ヘルプ", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" で全コマンドのヘルプ", desc))
        );
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", TextColor.color(0x555555)));
    }

    private void sendAinpcHelpLine(Player player, String command, String description,
                                    TextColor accent, TextColor desc) {
        String suggestBase = command.contains("<") ? command.split("<")[0].trim()
                : command.contains("[") ? command.split("\\[")[0].trim()
                : command;
        player.sendMessage(
                Component.text("  ")
                        .append(Component.text(command, accent)
                                .clickEvent(ClickEvent.suggestCommand(suggestBase))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("クリックでコマンド入力", NamedTextColor.YELLOW)
                                )))
                        .append(Component.text(" - " + description, desc))
        );
    }
}
