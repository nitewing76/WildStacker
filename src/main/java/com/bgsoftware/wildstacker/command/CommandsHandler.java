package com.bgsoftware.wildstacker.command;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.command.commands.CommandGive;
import com.bgsoftware.wildstacker.command.commands.CommandInfo;
import com.bgsoftware.wildstacker.command.commands.CommandReload;
import com.bgsoftware.wildstacker.command.commands.CommandSave;
import com.bgsoftware.wildstacker.command.commands.CommandStats;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class CommandsHandler implements CommandExecutor, TabCompleter {

    private final WildStackerPlugin plugin;
    private final List<ICommand> subCommands = new ArrayList<>();

    public CommandsHandler(WildStackerPlugin plugin) {
        this.plugin = plugin;
        subCommands.add(new CommandGive());
        subCommands.add(new CommandInfo());
        subCommands.add(new CommandReload());
        subCommands.add(new CommandSave());
        subCommands.add(new CommandStats());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0) {
            for (ICommand subCommand : subCommands) {
                if (subCommand.getLabel().equalsIgnoreCase(args[0])) {
                    if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
                        Locale.NO_PERMISSION.send(sender);
                        return false;
                    }
                    if (args.length < subCommand.getMinArgs() || args.length > subCommand.getMaxArgs()) {
                        Locale.COMMAND_USAGE.send(sender, subCommand.getUsage());
                        return false;
                    }
                    subCommand.perform(plugin, sender, args);
                    return true;
                }
            }
        }

        for (ICommand subCommand : subCommands) {
            if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                Locale.HELP_COMMAND_HEADER.send(sender);

                for (ICommand cmd : subCommands) {
                    if (cmd.getPermission() == null || sender.hasPermission(cmd.getPermission()))
                        Locale.HELP_COMMAND_LINE.send(sender, cmd.getUsage(), cmd.getDescription());
                }

                Locale.HELP_COMMAND_FOOTER.send(sender);
                return false;
            }
        }

        Locale.NO_PERMISSION.send(sender);
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length > 1) {
            for (ICommand subCommand : subCommands) {
                if (subCommand.getLabel().equalsIgnoreCase(args[0])) {
                    if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
                        return new ArrayList<>();
                    }
                    return subCommand.tabComplete(plugin, sender, args);
                }
            }
            return new ArrayList<>();
        }

        List<String> list = new ArrayList<>();
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ENGLISH);

        for (ICommand subCommand : subCommands) {
            if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                if (subCommand.getLabel().startsWith(prefix)) {
                    list.add(subCommand.getLabel());
                }
            }
        }

        return list;
    }

}