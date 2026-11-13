package com.bgsoftware.wildstacker.api;

import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public final class WildStackerAPI {

    private static WildStacker instance;

    private WildStackerAPI() {
    }

    public static void setPluginInstance(WildStacker instance) {
        if (WildStackerAPI.instance != null) {
            throw new UnsupportedOperationException("Cannot redefine singleton WildStacker instance.");
        }
        WildStackerAPI.instance = instance;
    }

    public static StackedItem getStackedItem(Item item) {
        return instance.getSystemManager().getStackedItem(item);
    }

    public static int getItemAmount(Item item) {
        StackedItem stackedItem = getStackedItem(item);
        return stackedItem == null ? item.getItemStack().getAmount() : stackedItem.getStackAmount();
    }

    public static StackedSpawner getStackedSpawner(CreatureSpawner spawner) {
        return instance.getSystemManager().getStackedSpawner(spawner);
    }

    public static StackedSpawner getStackedSpawner(Location location) {
        return instance.getSystemManager().getStackedSpawner(location);
    }

    public static int getSpawnersAmount(CreatureSpawner spawner) {
        StackedSpawner stackedSpawner = getStackedSpawner(spawner);
        return stackedSpawner == null ? 1 : stackedSpawner.getStackAmount();
    }

    public static StackedBarrel getStackedBarrel(Block block) {
        return instance.getSystemManager().getStackedBarrel(block);
    }

    public static StackedBarrel getStackedBarrel(Location location) {
        return instance.getSystemManager().getStackedBarrel(location);
    }

    public static int getBarrelAmount(Block block) {
        StackedBarrel stackedBarrel = getStackedBarrel(block);
        return stackedBarrel == null ? 1 : stackedBarrel.getStackAmount();
    }

    public static ItemStack getFormattedBarrelItem(ItemStack barrelItem, int amount) {
        return instance.getSystemManager().getFormattedBarrelItem(barrelItem, amount);
    }

    public static WildStacker getWildStacker() {
        return instance;
    }

}
