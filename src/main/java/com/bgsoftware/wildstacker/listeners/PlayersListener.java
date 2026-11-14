package com.bgsoftware.wildstacker.listeners;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.tasks.KillTask;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

@SuppressWarnings("unused")
public final class PlayersListener implements Listener {

    private final WildStackerPlugin plugin;

    public PlayersListener(WildStackerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (plugin.getSettings().killTaskInterval <= 0 || plugin.getSettings().killTaskTimeCommand.isEmpty())
            return;

        for (String commandSyntax : plugin.getSettings().killTaskTimeCommand.split(",")) {
            commandSyntax = "/" + commandSyntax.trim();

            if (!e.getMessage().equalsIgnoreCase(commandSyntax) && !e.getMessage().startsWith(commandSyntax + " "))
                continue;

            e.setCancelled(true);

            Locale.KILL_ALL_REMAINING_TIME.send(e.getPlayer(), KillTask.getTimeLeft());

            return;
        }
    }

}