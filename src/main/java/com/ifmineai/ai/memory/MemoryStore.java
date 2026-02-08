package com.ifmineai.ai.memory;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * NPC毎の短期/長期記憶ストア + YAML永続化
 */
public class MemoryStore {

    private static final int SHORT_TERM_SIZE = 50;
    private static final int LONG_TERM_SIZE = 200;

    private final JavaPlugin plugin;
    private final File memoriesFile;
    private final Map<UUID, List<MemoryEntry>> memories = new ConcurrentHashMap<>();

    public MemoryStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.memoriesFile = new File(plugin.getDataFolder(), "memories.yml");
        load();
    }

    public void addMemory(UUID npcUUID, MemoryEntry entry) {
        List<MemoryEntry> list = memories.computeIfAbsent(npcUUID, k -> new ArrayList<>());
        list.add(entry);

        // 短期記憶サイズ超過時、重要度の低いものから削除
        while (list.size() > SHORT_TERM_SIZE + LONG_TERM_SIZE) {
            // 最古かつ最低重要度のものを除去
            list.stream()
                    .min(Comparator.comparingInt(MemoryEntry::importance)
                            .thenComparingLong(MemoryEntry::timestamp))
                    .ifPresent(list::remove);
        }
    }

    public List<MemoryEntry> getRecentMemories(UUID npcUUID, int limit) {
        List<MemoryEntry> list = memories.getOrDefault(npcUUID, List.of());
        if (list.isEmpty()) return List.of();

        // 最新のlimit件を返す (重要度も考慮)
        return list.stream()
                .sorted(Comparator.comparingLong(MemoryEntry::timestamp).reversed())
                .limit(limit)
                .toList();
    }

    public List<MemoryEntry> getImportantMemories(UUID npcUUID, int limit) {
        List<MemoryEntry> list = memories.getOrDefault(npcUUID, List.of());
        if (list.isEmpty()) return List.of();

        return list.stream()
                .sorted(Comparator.comparingInt(MemoryEntry::importance).reversed()
                        .thenComparing(Comparator.comparingLong(MemoryEntry::timestamp).reversed()))
                .limit(limit)
                .toList();
    }

    public void clearMemories(UUID npcUUID) {
        memories.remove(npcUUID);
    }

    public int getMemoryCount(UUID npcUUID) {
        return memories.getOrDefault(npcUUID, List.of()).size();
    }

    public void saveAll() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, List<MemoryEntry>> entry : memories.entrySet()) {
            String uuidStr = entry.getKey().toString();
            List<MemoryEntry> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                MemoryEntry mem = list.get(i);
                String path = "memories." + uuidStr + "." + i;
                yaml.set(path + ".type", mem.type().name());
                yaml.set(path + ".content", mem.content());
                yaml.set(path + ".importance", mem.importance());
                yaml.set(path + ".timestamp", mem.timestamp());
            }
        }
        try {
            yaml.save(memoriesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "memories.yml の保存に失敗", e);
        }
    }

    private void load() {
        if (!memoriesFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(memoriesFile);
        ConfigurationSection root = yaml.getConfigurationSection("memories");
        if (root == null) return;

        for (String uuidStr : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection npcSection = root.getConfigurationSection(uuidStr);
            if (npcSection == null) continue;

            List<MemoryEntry> list = new ArrayList<>();
            for (String key : npcSection.getKeys(false)) {
                ConfigurationSection memSection = npcSection.getConfigurationSection(key);
                if (memSection == null) continue;

                String typeStr = memSection.getString("type", "OBSERVATION");
                MemoryType type;
                try {
                    type = MemoryType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    type = MemoryType.OBSERVATION;
                }

                String content = memSection.getString("content", "");
                int importance = memSection.getInt("importance", 1);
                long timestamp = memSection.getLong("timestamp", System.currentTimeMillis());

                list.add(new MemoryEntry(type, content, importance, timestamp));
            }
            memories.put(uuid, list);
        }
    }
}
