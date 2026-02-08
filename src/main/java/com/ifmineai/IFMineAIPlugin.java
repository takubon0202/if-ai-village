package com.ifmineai;

import com.ifmineai.ai.AIBrainManager;
import com.ifmineai.command.AICommandHandler;
import com.ifmineai.command.AITabCompleter;
import com.ifmineai.config.AIConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class IFMineAIPlugin extends JavaPlugin implements Listener {

    private CounselorManager counselorManager;
    private AIConfig aiConfig;
    private AIBrainManager aiBrainManager;

    // 会話中のプレイヤー -> NPC UUID マッピング
    private final Map<UUID, UUID> activeConversations = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // AI設定
        aiConfig = new AIConfig(this);
        aiConfig.logStartupStatus(getLogger());

        // カウンセラーマネージャ
        counselorManager = new CounselorManager(this);
        getServer().getPluginManager().registerEvents(counselorManager, this);
        getServer().getPluginManager().registerEvents(this, this);

        // AI Brain Manager
        aiBrainManager = new AIBrainManager(this, aiConfig);
        counselorManager.setAIBrainManager(aiBrainManager);
        aiBrainManager.initialize();

        // /ainpc コマンド登録
        PluginCommand ainpcCmd = getCommand("ainpc");
        if (ainpcCmd != null) {
            AICommandHandler cmdHandler = new AICommandHandler(aiBrainManager, aiConfig, this);
            AITabCompleter tabCompleter = new AITabCompleter(aiBrainManager);
            ainpcCmd.setExecutor(cmdHandler);
            ainpcCmd.setTabCompleter(tabCompleter);
        }

        getLogger().info("IFMineAI プラグインが有効になりました");
    }

    @Override
    public void onDisable() {
        if (aiBrainManager != null) {
            aiBrainManager.shutdown();
        }
        if (counselorManager != null) {
            counselorManager.shutdown();
        }
        getLogger().info("IFMineAI プラグインが無効になりました");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        if (counselorManager != null && counselorManager.isCounselor(villager)) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            // AI有効の場合: Gemini経由の会話開始
            UUID npcUUID = villager.getUniqueId();
            CounselorData data = counselorManager.getCounselorData(npcUUID);

            if (data != null && data.isAiEnabled() && aiBrainManager != null && aiBrainManager.isEnabled()) {
                // 既に会話中かチェック
                if (activeConversations.containsKey(player.getUniqueId())) {
                    UUID currentNPC = activeConversations.get(player.getUniqueId());
                    if (currentNPC.equals(npcUUID)) {
                        // 同じNPCなら会話終了
                        activeConversations.remove(player.getUniqueId());
                        aiBrainManager.getBrain(npcUUID).endConversation();
                        player.sendMessage(Component.text("会話を終了しました", NamedTextColor.GRAY));
                        return;
                    }
                }

                // 会話開始
                activeConversations.put(player.getUniqueId(), npcUUID);
                aiBrainManager.startConversation(npcUUID, player.getName());
                player.sendMessage(Component.text(
                        "[チャットでNPCに話しかけてください。もう一度右クリックで会話終了]",
                        NamedTextColor.GRAY
                ));
            } else {
                // AI無効: 従来の固定メッセージ
                player.sendMessage(
                        Component.text("相談員: ", NamedTextColor.GOLD)
                                .append(Component.text("何かお手伝いできることはありますか？", NamedTextColor.WHITE))
                );
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!activeConversations.containsKey(playerUUID)) return;

        UUID npcUUID = activeConversations.get(playerUUID);
        if (aiBrainManager == null || !aiBrainManager.isEnabled()) return;

        // NPCが存在するか確認
        if (aiBrainManager.getBrain(npcUUID) == null) {
            activeConversations.remove(playerUUID);
            return;
        }

        String message = event.getMessage();

        // 会話メッセージをAIに送信
        aiBrainManager.handlePlayerMessage(npcUUID, player.getName(), message);

        // 他のプレイヤーには通常のチャットとして見える
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("counselor")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行できます", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> counselorManager.listCounselors(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleSpawn(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text(
                    "使い方: /counselor spawn <north|south|east|west> <範囲> [性格タイプ]", NamedTextColor.RED));
            return;
        }

        String direction = args[1].toLowerCase();
        if (!List.of("north", "south", "east", "west").contains(direction)) {
            player.sendMessage(Component.text("方角は north, south, east, west のいずれかを指定してください", NamedTextColor.RED));
            return;
        }

        int range;
        try {
            range = Integer.parseInt(args[2]);
            if (range < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("範囲は1以上の整数で指定してください", NamedTextColor.RED));
            return;
        }

        // 性格タイプ (オプション)
        String personalityType = "counselor";
        if (args.length >= 4) {
            personalityType = args[3].toLowerCase();
        }

        counselorManager.spawnCounselor(player, direction, range, personalityType);
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("使い方: /counselor remove <nearest|all>", NamedTextColor.RED));
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

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("counselor")) {
            return List.of();
        }

        if (args.length == 1) {
            return filterStartsWith(List.of("spawn", "remove", "list"), args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn")) {
                return filterStartsWith(List.of("north", "south", "east", "west"), args[1]);
            }
            if (args[0].equalsIgnoreCase("remove")) {
                return filterStartsWith(List.of("nearest", "all"), args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("spawn")) {
            return filterStartsWith(List.of("3", "5", "10", "15", "20"), args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("spawn")) {
            // 性格タイプのサジェスト
            List<String> types = new ArrayList<>(List.of("counselor", "guard", "merchant", "explorer"));
            if (aiBrainManager != null && aiBrainManager.getPersonalityLoader() != null) {
                types = new ArrayList<>(aiBrainManager.getPersonalityLoader().getAllProfiles().keySet());
            }
            return filterStartsWith(types, args[3]);
        }

        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("=== /counselor コマンド ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/counselor spawn <方角> <範囲> [性格]", NamedTextColor.WHITE)
                .append(Component.text(" - NPCをスポーン", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/counselor remove <nearest|all>", NamedTextColor.WHITE)
                .append(Component.text(" - NPCを削除", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/counselor list", NamedTextColor.WHITE)
                .append(Component.text(" - NPC一覧表示", NamedTextColor.GRAY)));
    }

    // --- Getters ---

    public CounselorManager getCounselorManager() { return counselorManager; }
    public AIConfig getAIConfig() { return aiConfig; }
    public AIBrainManager getAIBrainManager() { return aiBrainManager; }
}
