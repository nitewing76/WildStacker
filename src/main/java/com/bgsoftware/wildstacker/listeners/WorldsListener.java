package com.bgsoftware.wildstacker.listeners;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.handlers.SystemHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

public final class WorldsListener implements Listener {

    private final WildStackerPlugin plugin;

    public WorldsListener(WildStackerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        plugin.getSystemManager().handleChunkLoad(e.getChunk(), SystemHandler.ENTITIES_STAGE, e.getEntities());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitiesUnload(EntitiesUnloadEvent e) {
        plugin.getSystemManager().handleChunkUnload(e.getChunk(), SystemHandler.ENTITIES_STAGE, e.getEntities());
    }

}