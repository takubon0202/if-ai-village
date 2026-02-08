package com.ifmineai;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import com.ifmineai.ai.AIBrainManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CounselorManager implements Listener {

    private static final String PDC_KEY_NAME = "counselor_npc";

    private final IFMineAIPlugin plugin;
    private final NamespacedKey counselorKey;
    private final Map<UUID, CounselorData> counselors = new HashMap<>();
    private final Map<UUID, BukkitTask> patrolTasks = new HashMap<>();
    private File counselorsFile;
    private YamlConfiguration counselorsConfig;
    private AIBrainManager aiBrainManager;

    public CounselorManager(IFMineAIPlugin plugin) {
        this.plugin = plugin;
        this.counselorKey = new NamespacedKey(plugin, PDC_KEY_NAME);
        this.counselorsFile = new File(plugin.getDataFolder(), "counselors.yml");
        if (!counselorsFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                counselorsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "counselors.yml の作成に失敗", e);
            }
        }
        this.counselorsConfig = YamlConfiguration.loadConfiguration(counselorsFile);
        loadCounselors();
    }

    public void setAIBrainManager(AIBrainManager aiBrainManager) {
        this.aiBrainManager = aiBrainManager;
    }

    public AIBrainManager getAIBrainManager() {
        return aiBrainManager;
    }

    public void spawnCounselor(org.bukkit.entity.Player player, String direction, int range) {
        spawnCounselor(player, direction, range, "counselor");
    }

    public void spawnCounselor(org.bukkit.entity.Player player, String direction, int range, String personalityType) {
        Location loc = player.getLocation();
        double snapX = Math.floor(loc.getX()) + 0.5;
        double snapY = loc.getY();
        double snapZ = Math.floor(loc.getZ()) + 0.5;

        World world = loc.getWorld();
        Location spawnLoc = new Location(world, snapX, snapY, snapZ);

        Villager villager = (Villager) world.spawnEntity(spawnLoc, EntityType.VILLAGER);

        villager.customName(Component.text("相談員", NamedTextColor.GOLD));
        villager.setCustomNameVisible(true);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.setSilent(true);

        String professionName = plugin.getConfig().getString("counselor.profession", "CLERIC");
        Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(
                NamespacedKey.minecraft(professionName.toLowerCase())
        );
        villager.setProfession(profession != null ? profession : Villager.Profession.CLERIC);
        villager.setVillagerLevel(plugin.getConfig().getInt("counselor.villager-level", 5));

        villager.getPersistentDataContainer().set(counselorKey, PersistentDataType.BYTE, (byte) 1);

        boolean aiEnabled = aiBrainManager != null && aiBrainManager.isEnabled();

        UUID uuid = villager.getUniqueId();
        CounselorData data = new CounselorData(
                uuid, world.getName(), snapX, snapY, snapZ, direction, range, personalityType, aiEnabled
        );
        counselors.put(uuid, data);

        if (aiEnabled) {
            // AI有効: Villager組み込みAIで自然に歩き回りつつ、ブレインが指示を出す
            villager.setAI(true);
            aiBrainManager.registerNPC(villager, data);
        } else {
            // AI無効: 従来のテレポートパトロール
            villager.setAI(false);
            startPatrol(villager, data);
        }
        saveCounselors();

        String modeStr = aiEnabled ? "AI自律行動" : "パトロール";
        player.sendMessage(Component.text(
                "相談員NPCをスポーンしました (方角: " + direction + ", 範囲: " + range
                        + ", 性格: " + personalityType + ", モード: " + modeStr + ")",
                NamedTextColor.GREEN));
    }

    private void startPatrol(Villager villager, CounselorData data) {
        double speed = plugin.getConfig().getDouble("counselor.patrol-speed", 0.08);
        int pauseTicks = plugin.getConfig().getInt("counselor.pause-ticks", 60);

        CounselorPatrolTask task = new CounselorPatrolTask(villager, data, speed, pauseTicks);
        BukkitTask bukkitTask = task.runTaskTimer(plugin, 0L, 1L);
        patrolTasks.put(villager.getUniqueId(), bukkitTask);
    }

    public void removeNearest(org.bukkit.entity.Player player) {
        UUID nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Map.Entry<UUID, CounselorData> entry : counselors.entrySet()) {
            CounselorData data = entry.getValue();
            if (!data.getWorldName().equals(player.getWorld().getName())) {
                continue;
            }
            double dist = player.getLocation().distanceSquared(
                    new Location(player.getWorld(), data.getOriginX(), data.getOriginY(), data.getOriginZ())
            );
            if (dist < minDist) {
                minDist = dist;
                nearest = entry.getKey();
            }
        }

        if (nearest != null) {
            removeCounselor(nearest);
            player.sendMessage(Component.text("最寄りの相談員NPCを削除しました", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("近くに相談員NPCが見つかりません", NamedTextColor.RED));
        }
    }

    public void removeAll() {
        for (UUID uuid : new HashMap<>(counselors).keySet()) {
            removeCounselor(uuid);
        }
    }

    private void removeCounselor(UUID uuid) {
        BukkitTask task = patrolTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        if (aiBrainManager != null) {
            aiBrainManager.unregisterNPC(uuid);
        }

        CounselorData data = counselors.remove(uuid);
        if (data != null) {
            World world = Bukkit.getWorld(data.getWorldName());
            if (world != null) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getUniqueId().equals(uuid)) {
                        entity.remove();
                        break;
                    }
                }
            }
        }

        saveCounselors();
    }

    public boolean isCounselor(Entity entity) {
        return entity.getPersistentDataContainer().has(counselorKey, PersistentDataType.BYTE)
                || counselors.containsKey(entity.getUniqueId());
    }

    public void listCounselors(org.bukkit.entity.Player player) {
        if (counselors.isEmpty()) {
            player.sendMessage(Component.text("相談員NPCは存在しません", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("=== 相談員NPC一覧 (" + counselors.size() + "体) ===", NamedTextColor.GOLD));
        int i = 1;
        for (Map.Entry<UUID, CounselorData> entry : counselors.entrySet()) {
            CounselorData data = entry.getValue();
            player.sendMessage(Component.text(
                    "" + i + ". " + data.getWorldName()
                            + " [" + (int) data.getOriginX() + ", " + (int) data.getOriginY() + ", " + (int) data.getOriginZ() + "]"
                            + " " + data.getDirection() + " range=" + data.getRange(),
                    NamedTextColor.WHITE
            ));
            i++;
        }
    }

    private void saveCounselors() {
        counselorsConfig = new YamlConfiguration();
        int index = 0;
        for (CounselorData data : counselors.values()) {
            ConfigurationSection section = counselorsConfig.createSection("counselors." + index);
            data.toConfigSection(section);
            index++;
        }
        try {
            counselorsConfig.save(counselorsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "counselors.yml の保存に失敗", e);
        }
    }

    private void loadCounselors() {
        ConfigurationSection root = counselorsConfig.getConfigurationSection("counselors");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            CounselorData data = CounselorData.fromConfigSection(section);
            if (data != null) {
                counselors.put(data.getEntityUUID(), data);
            }
        }

        if (!counselors.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, this::restoreAllCounselors, 20L);
        }
    }

    private void restoreAllCounselors() {
        int restored = 0;
        for (Map.Entry<UUID, CounselorData> entry : counselors.entrySet()) {
            UUID uuid = entry.getKey();
            CounselorData data = entry.getValue();
            World world = Bukkit.getWorld(data.getWorldName());
            if (world == null) continue;

            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid) && entity instanceof Villager villager) {
                    villager.getPersistentDataContainer().set(counselorKey, PersistentDataType.BYTE, (byte) 1);
                    if (data.isAiEnabled() && aiBrainManager != null && aiBrainManager.isEnabled()) {
                        villager.setAI(true);
                        aiBrainManager.registerNPC(villager, data);
                    } else {
                        startPatrol(villager, data);
                    }
                    restored++;
                    break;
                }
            }
        }
        plugin.getLogger().info("相談員NPC " + restored + "/" + counselors.size() + "体を復元しました");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Villager villager && counselors.containsKey(entity.getUniqueId())) {
                UUID uuid = entity.getUniqueId();
                CounselorData data = counselors.get(uuid);
                villager.getPersistentDataContainer().set(counselorKey, PersistentDataType.BYTE, (byte) 1);

                if (data.isAiEnabled() && aiBrainManager != null && aiBrainManager.isEnabled()) {
                    if (!aiBrainManager.hasBrain(uuid)) {
                        villager.setAI(true);
                        aiBrainManager.registerNPC(villager, data);
                    }
                } else if (!patrolTasks.containsKey(uuid)) {
                    startPatrol(villager, data);
                }
            }
        }
    }

    public Map<UUID, CounselorData> getCounselors() {
        return counselors;
    }

    public CounselorData getCounselorData(UUID uuid) {
        return counselors.get(uuid);
    }

    public IFMineAIPlugin getPlugin() {
        return plugin;
    }

    public NamespacedKey getCounselorKey() {
        return counselorKey;
    }

    public void shutdown() {
        for (BukkitTask task : patrolTasks.values()) {
            task.cancel();
        }
        patrolTasks.clear();
        saveCounselors();
    }
}
