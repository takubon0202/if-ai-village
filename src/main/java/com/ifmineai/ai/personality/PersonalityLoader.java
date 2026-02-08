package com.ifmineai.ai.personality;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * personalities.yml から性格プロファイルを読み込む
 */
public class PersonalityLoader {

    private final JavaPlugin plugin;
    private final Map<String, PersonalityProfile> profiles = new HashMap<>();

    public PersonalityLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        profiles.clear();

        // デフォルトプロファイルを先に登録
        profiles.put("counselor", PersonalityProfile.defaultProfile());

        File file = new File(plugin.getDataFolder(), "personalities.yml");
        if (!file.exists()) {
            plugin.saveResource("personalities.yml", false);
        }

        if (!file.exists()) {
            plugin.getLogger().info("personalities.yml が見つかりません。デフォルトプロファイルを使用します。");
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("personalities");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;

            PersonalityProfile profile = new PersonalityProfile(
                    key,
                    section.getString("display-name", key),
                    section.getString("description", ""),
                    section.getInt("friendliness", 5),
                    section.getInt("curiosity", 5),
                    section.getInt("sociability", 5),
                    section.getString("speech-style", "普通に話す")
            );
            profiles.put(key, profile);
        }

        plugin.getLogger().info("性格プロファイル " + profiles.size() + "件を読み込みました");
    }

    public PersonalityProfile getProfile(String type) {
        return profiles.getOrDefault(type, PersonalityProfile.defaultProfile());
    }

    public Map<String, PersonalityProfile> getAllProfiles() {
        return Map.copyOf(profiles);
    }

    public boolean hasProfile(String type) {
        return profiles.containsKey(type);
    }
}
