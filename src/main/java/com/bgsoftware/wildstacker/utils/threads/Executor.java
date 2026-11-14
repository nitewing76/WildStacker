package com.bgsoftware.wildstacker.utils.threads;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public final class Executor {

    private static final WildStackerPlugin plugin = WildStackerPlugin.getPlugin();
    private static boolean stop = false;

    private Executor() {
    }

    public static void sync(Runnable runnable) {
        if (stop)
            return;

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, runnable);
        } else {
            runnable.run();
        }
    }

    public static BukkitTask sync(Runnable runnable, long delay) {
        if (stop)
            return null;

        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public static void data(Runnable runnable) {
        async(runnable);
    }

    public static void async(Runnable runnable) {
        if (stop)
            return;

        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        } else {
            runnable.run();
        }
    }

    public static BukkitTask timer(Runnable runnable, long period) {
        if (stop)
            return null;

        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, period, period);
    }

    public static void stop() {
        stop = true;
    }

    public static void stopData() {
    }

}