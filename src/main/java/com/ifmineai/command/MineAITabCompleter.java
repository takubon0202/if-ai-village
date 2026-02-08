package com.ifmineai.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MineAITabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStartsWith(
                    List.of("help", "guide", "setup", "commands", "about"),
                    args[0]
            );
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("help")) {
            return filterStartsWith(List.of("1", "2", "3"), args[1]);
        }

        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
