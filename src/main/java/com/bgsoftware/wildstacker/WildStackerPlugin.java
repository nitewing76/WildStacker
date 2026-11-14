package com.bgsoftware.wildstacker;

import com.bgsoftware.wildstacker.api.WildStacker;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.handlers.DataManager;
import com.bgsoftware.wildstacker.command.CommandsHandler;
import com.bgsoftware.wildstacker.handlers.DataHandler;
import com.bgsoftware.wildstacker.handlers.SettingsHandler;
import com.bgsoftware.wildstacker.handlers.SystemHandler;
import com.bgsoftware.wildstacker.listeners.BarrelsListener;
import com.bgsoftware.wildstacker.listeners.BucketsListener;
import com.bgsoftware.wildstacker.listeners.ChunksListener;
import com.bgsoftware.wildstacker.listeners.ItemsListener;
import com.bgsoftware.wildstacker.listeners.MenusListener;
import com.bgsoftware.wildstacker.listeners.PickupItemListener;
import com.bgsoftware.wildstacker.listeners.PlayersListener;
import com.bgsoftware.wildstacker.listeners.SpawnersListener;
import com.bgsoftware.wildstacker.listeners.StewListener;
import com.bgsoftware.wildstacker.listeners.WorldsListener;
import com.bgsoftware.wildstacker.tasks.KillTask;
import com.bgsoftware.wildstacker.utils.holograms.OnDemandHologramManager;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WildStackerPlugin extends JavaPlugin implements WildStacker {

    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private static WildStackerPlugin plugin;

    private SettingsHandler settingsHandler;
    private SystemHandler systemManager;
    private DataHandler dataHandler;

    private boolean shouldEnable = true;

    public static void log(String message) {
        if (message == null)
            return;
        if (message.contains("&") || message.contains("\u00A7")) {
            Bukkit.getConsoleSender().sendMessage(AMPERSAND_SERIALIZER.deserialize("[WildStacker] " + message));
        } else if (plugin != null) {
            plugin.getLogger().info(message);
        }
    }

    public static WildStackerPlugin getPlugin() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        loadAPI();
    }

    @Override
    public void onEnable() {
        if (!shouldEnable) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        log("******** ENABLE START ********");

        settingsHandler = new SettingsHandler(this);
        dataHandler = new DataHandler(this);
        systemManager = new SystemHandler(this);

        Locale.reload();
        KillTask.start();
        OnDemandHologramManager.start(this);

        getServer().getPluginManager().registerEvents(new BarrelsListener(this), this);
        getServer().getPluginManager().registerEvents(new BucketsListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunksListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemsListener(this), this);
        getServer().getPluginManager().registerEvents(new MenusListener(), this);
        getServer().getPluginManager().registerEvents(new PickupItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayersListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnersListener(this), this);
        getServer().getPluginManager().registerEvents(new StewListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldsListener(this), this);

        CommandsHandler commandsHandler = new CommandsHandler(this);
        PluginCommand stackerCommand = getCommand("stacker");
        if (stackerCommand != null) {
            stackerCommand.setExecutor(commandsHandler);
            stackerCommand.setTabCompleter(commandsHandler);
        }

        log("******** ENABLE DONE ********");
    }

    @Override
    public void onDisable() {
        log("Cancelling tasks...");

        OnDemandHologramManager.stop();

        try {
            Bukkit.getScheduler().cancelTasks(this);
        } catch (Throwable ignored) {
        }

        if (shouldEnable && systemManager != null && dataHandler != null) {
            log("Performing items, spawners & barrels save...");

            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks())
                    systemManager.handleChunkUnload(chunk, SystemHandler.CHUNK_FULL_STAGE);
            }

            systemManager.performCacheSave();

            log("Clearing database...");
            dataHandler.clearDatabase();
        }

        log("Stopping executor...");
        Executor.stop();
    }

    private void loadAPI() {
        try {
            WildStackerAPI.setPluginInstance(this);
        } catch (UnsupportedOperationException ex) {
            log("Failed to set-up API - disabling plugin...");
            ex.printStackTrace();
            shouldEnable = false;
        }
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    @Override
    public DataManager getDataManager() {
        return dataHandler;
    }

    @Override
    public SystemHandler getSystemManager() {
        return systemManager;
    }

    public SettingsHandler getSettings() {
        return settingsHandler;
    }

    public void setSettings(SettingsHandler settingsHandler) {
        this.settingsHandler = settingsHandler;
    }

}