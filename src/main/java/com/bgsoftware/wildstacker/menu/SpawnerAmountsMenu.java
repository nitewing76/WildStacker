package com.bgsoftware.wildstacker.menu;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.listeners.SpawnersListener;
import com.bgsoftware.wildstacker.utils.events.EventsCaller;
import com.bgsoftware.wildstacker.utils.files.FileUtils;
import com.bgsoftware.wildstacker.utils.files.SoundWrapper;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpawnerAmountsMenu extends WildMenu {

    private static Map<Integer, Integer> depositSpawners = new HashMap<>(), withdrawSpawners = new HashMap<>();
    private static SoundWrapper successSound, failureSound;

    private final WeakReference<StackedSpawner> stackedSpawner;

    private SpawnerAmountsMenu(StackedSpawner stackedSpawner) {
        super("amountsMenu");
        this.stackedSpawner = new WeakReference<>(stackedSpawner);
    }

    public static void open(Player player, StackedSpawner stackedSpawner) {
        new SpawnerAmountsMenu(stackedSpawner).openMenu(player);
    }

    public static void loadMenu() {
        SpawnerAmountsMenu spawnerAmountsMenu = new SpawnerAmountsMenu(null);

        File file = new File(plugin.getDataFolder(), "menus/spawner-amounts.yml");

        if (!file.exists())
            FileUtils.saveResource("menus/spawner-amounts.yml");

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        Map<Character, List<Integer>> charSlots = FileUtils.loadGUI(spawnerAmountsMenu, "spawner-amounts.yml", cfg);
        Map<Integer, Integer> depositMap = new HashMap<>();
        Map<Integer, Integer> withdrawMap = new HashMap<>();

        ConfigurationSection itemsSection = cfg.getConfigurationSection("items");
        if (!charSlots.isEmpty() && itemsSection != null) {
            for (String itemChar : itemsSection.getKeys(false)) {
                if (cfg.contains("items." + itemChar + ".deposit")) {
                    List<Integer> slots = charSlots.get(itemChar.toCharArray()[0]);
                    if (slots != null)
                        slots.forEach(slot -> depositMap.put(slot, cfg.getInt("items." + itemChar + ".deposit")));
                } else if (cfg.contains("items." + itemChar + ".withdraw")) {
                    List<Integer> slots = charSlots.get(itemChar.toCharArray()[0]);
                    if (slots != null)
                        slots.forEach(slot -> withdrawMap.put(slot, cfg.getInt("items." + itemChar + ".withdraw")));
                }
            }
        }

        SpawnerAmountsMenu.depositSpawners = depositMap;
        SpawnerAmountsMenu.withdrawSpawners = withdrawMap;
        SpawnerAmountsMenu.successSound = FileUtils.getSound(cfg.getConfigurationSection("success-sound"));
        SpawnerAmountsMenu.failureSound = FileUtils.getSound(cfg.getConfigurationSection("failure-sound"));
    }

    @Override
    public void onPlayerClick(InventoryClickEvent e) {
        StackedSpawner spawner = this.stackedSpawner.get();

        if (spawner == null) {
            e.getWhoClicked().closeInventory();
            return;
        }

        if (handleBreak(e, spawner))
            return;

        handlePlace(e, spawner);
    }

    @Override
    public void onMenuClose(InventoryCloseEvent e) {
    }

    private boolean handleBreak(InventoryClickEvent e, StackedSpawner spawner) {
        Integer withdrawAmount = withdrawSpawners.get(e.getRawSlot());

        if (withdrawAmount == null)
            return false;

        withdrawAmount = Math.min(spawner.getStackAmount(), withdrawAmount);

        if (withdrawAmount <= 0) {
            if (failureSound != null)
                failureSound.playSound(e.getWhoClicked());
            return true;
        }

        SoundWrapper soundToPlay = SpawnersListener.handleSpawnerBreak(plugin, spawner, withdrawAmount,
                (Player) e.getWhoClicked(), true) ? successSound : failureSound;

        if (soundToPlay != null)
            soundToPlay.playSound(e.getWhoClicked());

        if (spawner.getStackAmount() <= 0)
            e.getWhoClicked().closeInventory();

        return true;
    }

    private void handlePlace(InventoryClickEvent e, StackedSpawner spawner) {
        Integer depositAmount = depositSpawners.get(e.getRawSlot());

        if (depositAmount == null)
            return;

        if (!spawner.isCached()) {
            if (failureSound != null)
                failureSound.playSound(e.getWhoClicked());
            return;
        }

        Player player = (Player) e.getWhoClicked();
        EntityType spawnerType = spawner.getSpawnedType();
        Inventory playerInventory = player.getInventory();
        int currentStackAmount = spawner.getStackAmount();
        int spawnerStackLimit = spawner.getStackLimit();

        Map<Integer, ItemStack> itemsToRemove = new LinkedHashMap<>();

        int toAdd = 0;

        if (player.getGameMode() == GameMode.CREATIVE) {
            toAdd = Math.min(spawnerStackLimit - currentStackAmount, depositAmount);
        } else {
            for (int i = 0; i < playerInventory.getSize(); i++) {
                ItemStack itemStack = playerInventory.getItem(i);

                if (itemStack == null || itemStack.getType() != Material.SPAWNER)
                    continue;

                if (spawnerType != ItemUtils.getSpawnerItemEntityType(itemStack))
                    continue;

                int itemAmount = ItemUtils.getSpawnerItemAmount(itemStack) * itemStack.getAmount();

                if (toAdd + itemAmount > depositAmount)
                    itemAmount = depositAmount - toAdd;

                if (currentStackAmount + toAdd + itemAmount > spawnerStackLimit)
                    itemAmount = (spawnerStackLimit - currentStackAmount - toAdd);

                if (itemAmount <= 0)
                    break;

                toAdd += itemAmount;
                itemsToRemove.put(i, itemStack);
            }
        }

        if (toAdd <= 0 || !EventsCaller.callSpawnerPlaceInventoryEvent(player, spawner, toAdd)) {
            if (failureSound != null)
                failureSound.playSound(e.getWhoClicked());
            return;
        }

        spawner.increaseStackAmount(toAdd, true);
        Locale.SPAWNER_UPDATE.send(player, spawner.getStackAmount());

        if (successSound != null)
            successSound.playSound(e.getWhoClicked());

        int leftToRemove = toAdd;

        for (Map.Entry<Integer, ItemStack> itemEntry : itemsToRemove.entrySet()) {
            int slot = itemEntry.getKey();
            ItemStack itemStack = itemEntry.getValue();
            int itemAmount = ItemUtils.getSpawnerItemAmount(itemStack) * itemStack.getAmount();

            if (itemAmount <= leftToRemove) {
                playerInventory.setItem(slot, new ItemStack(Material.AIR));
                leftToRemove -= itemAmount;
            } else {
                if (itemStack.getAmount() == 1) {
                    playerInventory.setItem(slot, spawner.getDropItem(itemAmount - leftToRemove));
                } else {
                    int itemsToTake = leftToRemove / ItemUtils.getSpawnerItemAmount(itemStack);
                    int leftOvers = leftToRemove % ItemUtils.getSpawnerItemAmount(itemStack);
                    ItemStack clone = itemStack.clone();
                    clone.setAmount(clone.getAmount() - itemsToTake - (leftOvers > 0 ? 1 : 0));
                    playerInventory.setItem(slot, clone);
                    if (leftOvers > 0) {
                        ItemUtils.addItem(spawner.getDropItem(ItemUtils.getSpawnerItemAmount(itemStack) - leftOvers),
                                playerInventory, spawner.getLocation());
                    }
                }
                break;
            }
        }
    }

}