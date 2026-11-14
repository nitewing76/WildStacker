package com.bgsoftware.wildstacker.utils.holograms;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.objects.WStackedHologramObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OnDemandHologramManager {

    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private static BukkitTask task;
    private static final Map<Location, ActiveHologram> activeHolograms = new HashMap<>();

    private OnDemandHologramManager() {
    }

    public static void start(WildStackerPlugin plugin) {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(plugin), 5L, 5L);
    }

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (ActiveHologram active : activeHolograms.values()) {
            active.remove();
        }
        activeHolograms.clear();
    }

    public static Hologram createTemporaryHologram(Location location, String name) {
        World world = location.getWorld();
        if (world == null)
            return null;

        Location spawnLoc = location.getBlock().getLocation().add(0.5, 1.25, 0.5);
        int viewRangeBlocks = WildStackerPlugin.getPlugin().getSettings().cmiHologramViewRange;
        float viewRangeMultiplier = Math.max(0.1F, viewRangeBlocks / 64.0F);

        TextDisplay display = world.spawn(spawnLoc, TextDisplay.class, td -> {
            td.setPersistent(false);
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(false);
            td.setShadowed(true);
            td.setDefaultBackground(false);
            td.setBackgroundColor(Color.fromARGB(64, 0, 0, 0));
            td.setViewRange(viewRangeMultiplier);
            td.text(deserialize(name));
        });

        return new Hologram() {
            @Override
            public void setHologramName(String updatedName) {
                if (display.isValid()) {
                    display.text(deserialize(updatedName));
                }
            }

            @Override
            public void removeHologram() {
                if (display.isValid()) {
                    display.remove();
                }
            }
        };
    }

    private static void tick(WildStackerPlugin plugin) {
        int maxDist = plugin.getSettings().cmiHologramViewRange;
        Set<Location> lookedAtLocations = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead())
                continue;

            Block targetBlock = player.getTargetBlockExact(maxDist, FluidCollisionMode.NEVER);
            if (targetBlock == null)
                continue;

            Material type = targetBlock.getType();
            WStackedHologramObject<?> targetObj = null;

            if (type == Material.SPAWNER && !plugin.getSettings().floatingSpawnerNames) {
                if (plugin.getSystemManager().isStackedSpawner(targetBlock)) {
                    StackedSpawner spawner = plugin.getSystemManager().getStackedSpawner(targetBlock.getLocation());
                    if (spawner instanceof WStackedHologramObject<?> holoObj) {
                        targetObj = holoObj;
                    }
                }
            } else if (type == Material.CAULDRON || type == Material.WATER_CAULDRON) {
                if (plugin.getSystemManager().isStackedBarrel(targetBlock)) {
                    StackedBarrel barrel = plugin.getSystemManager().getStackedBarrel(targetBlock.getLocation());
                    if (barrel instanceof WStackedHologramObject<?> holoObj) {
                        targetObj = holoObj;
                    }
                }
            }

            if (targetObj == null)
                continue;

            String holoText = targetObj.getCachedHologramName();
            if (holoText == null || holoText.isEmpty())
                continue;

            Location blockLoc = targetBlock.getLocation();
            lookedAtLocations.add(blockLoc);

            ActiveHologram active = activeHolograms.get(blockLoc);
            if (active == null || !active.isValid()) {
                if (active != null) {
                    active.remove();
                }
                Hologram created = createTemporaryHologram(blockLoc, holoText);
                if (created != null) {
                    activeHolograms.put(blockLoc, new ActiveHologram(targetObj, created, holoText));
                    targetObj.setHologram(created);
                }
            } else if (!active.lastText.equals(holoText)) {
                active.hologram.setHologramName(holoText);
                active.lastText = holoText;
            }
        }

        Iterator> iterator = activeHolograms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, ActiveHologram> entry = iterator.next();
            if (!lookedAtLocations.contains(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }
    }

    private static Component deserialize(String text) {
        if (text == null || text.isEmpty())
            return Component.empty();
        if (text.indexOf('\u00A7') != -1) {
            return SECTION_SERIALIZER.deserialize(text);
        }
        return AMPERSAND_SERIALIZER.deserialize(text);
    }

    private static final class ActiveHologram {
        private final WStackedHologramObject<?> owner;
        private final Hologram hologram;
        private String lastText;

        private ActiveHologram(WStackedHologramObject<?> owner, Hologram hologram, String lastText) {
            this.owner = owner;
            this.hologram = hologram;
            this.lastText = lastText;
        }

        private boolean isValid() {
            return owner.getHologram() == hologram;
        }

        private void remove() {
            hologram.removeHologram();
            if (owner.getHologram() == hologram) {
                owner.setHologram(null);
            }
        }
    }

}