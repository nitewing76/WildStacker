package com.bgsoftware.wildstacker.menu;

import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.objects.WStackedSpawner;
import com.bgsoftware.wildstacker.utils.files.FileUtils;
import com.bgsoftware.wildstacker.utils.items.ItemBuilder;
import com.bgsoftware.wildstacker.utils.pair.Pair;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SpawnersManageMenu extends WildMenu {

    private static List<Integer> amountsMenuSlots = new ArrayList<>();
    private static List<Pair<Integer, ItemBuilder>> statisticSlots = new ArrayList<>();
    private final WeakReference<StackedSpawner> stackedSpawner;
    private final BukkitTask bukkitTask;

    private boolean onBuild = false;

    private SpawnersManageMenu(StackedSpawner stackedSpawner) {
        super("manageMenu");
        this.stackedSpawner = new WeakReference<>(stackedSpawner);
        this.bukkitTask = stackedSpawner == null ? null : Executor.timer(this::tick, 20L);
    }

    public static void open(Player player, StackedSpawner stackedSpawner) {
        SpawnersManageMenu spawnersManageMenu = ((WStackedSpawner) stackedSpawner).getLinkedInventory();

        if (spawnersManageMenu == null) {
            spawnersManageMenu = new SpawnersManageMenu(stackedSpawner);
            ((WStackedSpawner) stackedSpawner).linkInventory(spawnersManageMenu);
        }

        spawnersManageMenu.openMenu(player);
    }

    public static void loadMenu() {
        SpawnersManageMenu spawnersManageMenu = new SpawnersManageMenu(null);

        File file = new File(plugin.getDataFolder(), "menus/spawner-manage.yml");

        if (!file.exists())
            FileUtils.saveResource("menus/spawner-manage.yml");

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        Map<Character, List<Integer>> charSlots = FileUtils.loadGUI(spawnersManageMenu, "spawner-manage.yml", cfg);

        amountsMenuSlots = getSlots(cfg, "amounts-menu", charSlots);
        statisticSlots = new ArrayList<>();

        for (Character character : cfg.getString("statistics", "").toCharArray()) {
            for (int slot : charSlots.getOrDefault(character, new ArrayList<>())) {
                ItemBuilder fillItem = spawnersManageMenu.getData().fillItems.get(slot);
                if (fillItem != null) {
                    statisticSlots.add(new Pair<>(slot, fillItem));
                }
            }
        }
    }

    @Override
    public void onPlayerClick(InventoryClickEvent e) {
        StackedSpawner spawner = this.stackedSpawner.get();

        if (spawner == null) {
            e.getWhoClicked().closeInventory();
            stop();
            return;
        }

        if (amountsMenuSlots.contains(e.getRawSlot())) {
            if (plugin.getSettings().amountsMenuEnabled) {
                SpawnerAmountsMenu.open((Player) e.getWhoClicked(), spawner);
            }
        }
    }

    @Override
    public void onMenuClose(InventoryCloseEvent e) {
    }

    @Override
    public void onInventoryBuild() {
        onBuild = true;
        tick();
        onBuild = false;
    }

    public void tick() {
        if (inventory == null || (!onBuild && inventory.getViewers().isEmpty()))
            return;

        StackedSpawner spawner = this.stackedSpawner.get();

        if (spawner == null) {
            stop();
            return;
        }

        CreatureSpawner creatureSpawner = spawner.getSpawner();

        for (Pair<Integer, ItemBuilder> statisticItem : statisticSlots) {
            inventory.setItem(statisticItem.getKey(), statisticItem.getValue().copy()
                    .replaceAll("%min-spawn-delay%", String.valueOf(creatureSpawner.getMinSpawnDelay()))
                    .replaceAll("%max-spawn-delay%", String.valueOf(creatureSpawner.getMaxSpawnDelay()))
                    .replaceAll("%spawn-count%", String.valueOf(creatureSpawner.getSpawnCount()))
                    .replaceAll("%max-nearby-entities%", String.valueOf(creatureSpawner.getMaxNearbyEntities()))
                    .replaceAll("%player-range%", String.valueOf(creatureSpawner.getRequiredPlayerRange()))
                    .replaceAll("%required-player-range%", String.valueOf(creatureSpawner.getRequiredPlayerRange()))
                    .replaceAll("%spawn-range%", String.valueOf(creatureSpawner.getSpawnRange()))
                    .replaceAll("%ticks-left%", String.valueOf(Math.max(0, creatureSpawner.getDelay())))
                    .replaceAll("%failure-reason%", "None")
                    .build());
        }
    }

    public void stop() {
        if (bukkitTask != null)
            bukkitTask.cancel();
    }

}