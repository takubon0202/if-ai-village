package com.ifmineai.command;

import com.ifmineai.IFMineAIPlugin;
import com.ifmineai.ai.AIBrainManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CounselorTabCompleter implements TabCompleter {

    private final IFMineAIPlugin plugin;

    public CounselorTabCompleter(IFMineAIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStartsWith(List.of("spawn", "remove", "list", "help", "menu", "wizard"), args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "spawn" -> {
                    return filterStartsWith(List.of("north", "south", "east", "west"), args[1]);
                }
                case "remove" -> {
                    return filterStartsWith(List.of("nearest", "all"), args[1]);
                }
                case "wizard" -> {
                    return filterStartsWith(List.of("spawn", "remove"), args[1]);
                }
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("spawn")) {
                return filterStartsWith(List.of("3", "5", "10", "15", "20", "30", "50"), args[2]);
            }
            if (args[0].equalsIgnoreCase("wizard") && args[1].equalsIgnoreCase("spawn")) {
                return filterStartsWith(List.of("north", "south", "east", "west"), args[2]);
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("spawn")) {
                List<String> types = new ArrayList<>(List.of("counselor", "guard", "merchant", "explorer"));
                AIBrainManager brainManager = plugin.getAIBrainManager();
                if (brainManager != null && brainManager.getPersonalityLoader() != null) {
                    types = new ArrayList<>(brainManager.getPersonalityLoader().getAllProfiles().keySet());
                }
                return filterStartsWith(types, args[3]);
            }
            if (args[0].equalsIgnoreCase("wizard") && args[1].equalsIgnoreCase("spawn")) {
                return filterStartsWith(List.of("3", "5", "10", "15", "20", "30", "50"), args[3]);
            }
        }

        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
