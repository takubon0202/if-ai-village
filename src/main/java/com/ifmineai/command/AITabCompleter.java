package com.ifmineai.command;

import com.ifmineai.ai.AIBrainManager;
import com.ifmineai.ai.NPCBrain;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class AITabCompleter implements TabCompleter {

    private final AIBrainManager brainManager;

    public AITabCompleter(AIBrainManager brainManager) {
        this.brainManager = brainManager;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStartsWith(
                    List.of("help", "status", "reload", "debug", "personality", "stats"),
                    args[0]
            );
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("debug")) {
                // UUID一覧をサジェスト
                return brainManager.getBrains().keySet().stream()
                        .map(UUID::toString)
                        .filter(s -> s.startsWith(args[1]))
                        .limit(10)
                        .toList();
            }
        }

        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
