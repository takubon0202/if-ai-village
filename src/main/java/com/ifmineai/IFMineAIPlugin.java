package com.ifmineai;

import com.ifmineai.ai.AIBrainManager;
import com.ifmineai.command.AICommandHandler;
import com.ifmineai.command.AITabCompleter;
import com.ifmineai.command.CounselorCommandHandler;
import com.ifmineai.command.CounselorTabCompleter;
import com.ifmineai.command.MineAICommandHandler;
import com.ifmineai.command.MineAITabCompleter;
import com.ifmineai.config.AIConfig;
import com.ifmineai.listener.JoinGuideListener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

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

        // ジョインガイドリスナー
        getServer().getPluginManager().registerEvents(new JoinGuideListener(this), this);

        // /counselor コマンド登録
        PluginCommand counselorCmd = getCommand("counselor");
        if (counselorCmd != null) {
            counselorCmd.setExecutor(new CounselorCommandHandler(this, counselorManager));
            counselorCmd.setTabCompleter(new CounselorTabCompleter(this));
        }

        // /ainpc コマンド登録
        PluginCommand ainpcCmd = getCommand("ainpc");
        if (ainpcCmd != null) {
            ainpcCmd.setExecutor(new AICommandHandler(aiBrainManager, aiConfig, this));
            ainpcCmd.setTabCompleter(new AITabCompleter(aiBrainManager));
        }

        // /mineai コマンド登録
        PluginCommand mineaiCmd = getCommand("mineai");
        if (mineaiCmd != null) {
            mineaiCmd.setExecutor(new MineAICommandHandler(this));
            mineaiCmd.setTabCompleter(new MineAITabCompleter());
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
                        aiBrainManager.endConversation(npcUUID);
                        player.sendMessage(Component.text("会話を終了しました", NamedTextColor.GRAY));
                        return;
                    }
                }

                // 別のNPCと会話中なら先にそちらを終了
                if (activeConversations.containsKey(player.getUniqueId())) {
                    UUID prevNPC = activeConversations.get(player.getUniqueId());
                    aiBrainManager.endConversation(prevNPC);
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        UUID npcUUID = activeConversations.remove(playerUUID);
        if (npcUUID != null && aiBrainManager != null) {
            aiBrainManager.endConversation(npcUUID);
        }
    }

    // --- Getters ---

    public CounselorManager getCounselorManager() { return counselorManager; }
    public AIConfig getAIConfig() { return aiConfig; }
    public AIBrainManager getAIBrainManager() { return aiBrainManager; }
}
