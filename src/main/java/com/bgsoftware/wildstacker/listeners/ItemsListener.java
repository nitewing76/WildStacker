package com.bgsoftware.wildstacker.listeners;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.bgsoftware.wildstacker.objects.WStackedItem;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import org.bukkit.Chunk;
import org.bukkit.block.Hopper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class ItemsListener implements Listener {

    private final WildStackerPlugin plugin;

    public ItemsListener(WildStackerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        if (!plugin.getSettings().itemsStackingEnabled || e.getEntity().getType() != EntityType.ITEM)
            return;

        StackedItem stackedItem = WStackedItem.ofBypass(e.getEntity());

        if (!stackedItem.isCached())
            return;

        int limit = stackedItem.getStackLimit();

        if (stackedItem.getStackAmount() > limit) {
            int overLimit = stackedItem.getStackAmount() - limit;
            ItemStack cloned = stackedItem.getItemStack().clone();
            cloned.setAmount(Math.min(cloned.getMaxStackSize(), overLimit));
            stackedItem.setStackAmount(limit, true);
            Item spawnedItem = plugin.getSystemManager().spawnItemWithAmount(e.getEntity().getLocation(), cloned, overLimit);
            if (spawnedItem != null) {
                spawnedItem.setPickupDelay(40);
            }
        }

        ((WStackedItem) stackedItem).runStackAsync(optionalItem -> {
            if (optionalItem.isPresent())
                return;

            Executor.sync(() -> {
                PersistentDataContainer pdc = e.getEntity().getPersistentDataContainer();
                if (pdc.has(WStackedItem.KEY_DROPPED_BY_PLAYER, PersistentDataType.BYTE)) {
                    pdc.remove(WStackedItem.KEY_DROPPED_BY_PLAYER);
                } else if (isChunkLimit(e.getLocation().getChunk())) {
                    stackedItem.remove();
                }
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemDrop(PlayerDropItemEvent e) {
        e.getItemDrop().getPersistentDataContainer().set(WStackedItem.KEY_DROPPED_BY_PLAYER, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent e) {
        if (ItemUtils.isStackable(e.getEntity()))
            WStackedItem.of(e.getEntity()).remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent e) {
        if (!ItemUtils.isStackable(e.getItem()))
            return;

        StackedItem stackedItem = WStackedItem.of(e.getItem());
        if (((WStackedItem) stackedItem).isRemoved()) {
            e.getItem().remove();
            e.setCancelled(true);
            return;
        }

        if (stackedItem.getStackAmount() > 1) {
            e.setCancelled(true);
            stackedItem.giveItemStack(e.getInventory());
            InventoryHolder inventoryHolder = e.getInventory().getHolder();
            if (inventoryHolder instanceof Hopper hopper)
                hopper.getBlock().getState().update();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent e) {
        if (!plugin.getSettings().itemsStackingEnabled || !ItemUtils.isStackable(e.getEntity()) ||
                !ItemUtils.isStackable(e.getTarget()))
            return;

        StackedItem stackedItem = WStackedItem.ofBypass(e.getEntity());

        if (!stackedItem.isCached())
            return;

        e.setCancelled(true);

        Executor.sync(() -> {
            if (e.getEntity().isValid() && e.getTarget().isValid()) {
                StackedItem targetItem = WStackedItem.ofBypass(e.getTarget());
                stackedItem.runStackAsync(targetItem, null);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent e) {
        if (!plugin.getSettings().itemsStackingEnabled)
            return;

        Map<ItemStack, Item> itemsMap = new HashMap<>();
        List<Item> itemsToRemove = new ArrayList<>();

        for (Item item : e.getItems()) {
            ItemStack clone = item.getItemStack().clone();
            clone.setAmount(1);

            StackedItem stackedItem = WStackedItem.ofBypass(item);

            if (stackedItem.isCached()) {
                Item currentItem = itemsMap.get(clone);

                if (currentItem == null) {
                    itemsMap.put(clone, item);
                } else {
                    itemsToRemove.add(item);
                    int combinedAmount = ItemUtils.loadItemStackAmount(currentItem) + ItemUtils.loadItemStackAmount(item);
                    ItemStack updatedStack = currentItem.getItemStack().clone();
                    updatedStack.setAmount(Math.min(updatedStack.getMaxStackSize(), combinedAmount));
                    currentItem.setItemStack(updatedStack);
                    ItemUtils.saveItemStackAmount(currentItem, combinedAmount);
                }

                plugin.getSystemManager().removeStackObject(stackedItem);
            }
        }

        e.getItems().removeAll(itemsToRemove);
    }

    private boolean isChunkLimit(Chunk chunk) {
        int chunkLimit = plugin.getSettings().itemsChunkLimit;

        if (chunkLimit <= 0)
            return false;

        return (int) Arrays.stream(chunk.getEntities()).filter(entity -> entity instanceof Item).count() > chunkLimit;
    }

}