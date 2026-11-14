package com.bgsoftware.wildstacker.utils.events;

import com.bgsoftware.wildstacker.api.events.BarrelPlaceEvent;
import com.bgsoftware.wildstacker.api.events.BarrelStackEvent;
import com.bgsoftware.wildstacker.api.events.BarrelUnstackEvent;
import com.bgsoftware.wildstacker.api.events.ItemStackEvent;
import com.bgsoftware.wildstacker.api.events.SpawnerPlaceEvent;
import com.bgsoftware.wildstacker.api.events.SpawnerStackEvent;
import com.bgsoftware.wildstacker.api.events.SpawnerUnstackEvent;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class EventsCaller {

    private EventsCaller() {
    }

    public static ItemStack callBarrelDropEvent(StackedBarrel stackedBarrel, Player player, int amount) {
        return stackedBarrel.getBarrelItem(amount);
    }

    public static boolean callBarrelPlaceEvent(Player player, StackedBarrel stackedBarrel, ItemStack inHand) {
        BarrelPlaceEvent barrelPlaceEvent = new BarrelPlaceEvent(player, stackedBarrel, inHand);
        Bukkit.getPluginManager().callEvent(barrelPlaceEvent);
        return !barrelPlaceEvent.isCancelled();
    }

    public static boolean callBarrelPlaceInventoryEvent(Player player, StackedBarrel stackedBarrel, int increaseAmount) {
        return true;
    }

    public static boolean callBarrelStackEvent(StackedBarrel barrel, StackedBarrel target) {
        BarrelStackEvent barrelStackEvent = new BarrelStackEvent(barrel, target);
        Bukkit.getPluginManager().callEvent(barrelStackEvent);
        return !barrelStackEvent.isCancelled();
    }

    public static boolean callBarrelUnstackEvent(StackedBarrel barrel, Entity unstackSource, int unstackAmount) {
        BarrelUnstackEvent barrelUnstackEvent = new BarrelUnstackEvent(barrel, unstackSource, unstackAmount);
        Bukkit.getPluginManager().callEvent(barrelUnstackEvent);
        return !barrelUnstackEvent.isCancelled();
    }

    public static boolean callItemStackEvent(StackedItem item, StackedItem target) {
        ItemStackEvent itemStackEvent = new ItemStackEvent(item, target);
        Bukkit.getPluginManager().callEvent(itemStackEvent);
        return !itemStackEvent.isCancelled();
    }

    public static ItemStack callSpawnerDropEvent(StackedSpawner stackedSpawner, Player player, int amount) {
        return stackedSpawner.getDropItem(amount);
    }

    public static boolean callSpawnerPlaceEvent(Player player, StackedSpawner stackedSpawner, ItemStack inHand) {
        SpawnerPlaceEvent spawnerPlaceEvent = new SpawnerPlaceEvent(player, stackedSpawner, inHand);
        Bukkit.getPluginManager().callEvent(spawnerPlaceEvent);
        return !spawnerPlaceEvent.isCancelled();
    }

    public static boolean callSpawnerPlaceInventoryEvent(Player player, StackedSpawner stackedSpawner, int increaseAmount) {
        return true;
    }

    public static boolean callSpawnerStackEvent(StackedSpawner spawner, StackedSpawner target) {
        SpawnerStackEvent spawnerStackEvent = new SpawnerStackEvent(spawner, target);
        Bukkit.getPluginManager().callEvent(spawnerStackEvent);
        return !spawnerStackEvent.isCancelled();
    }

    public static boolean callSpawnerUnstackEvent(StackedSpawner spawner, Entity unstackSource, int unstackAmount) {
        SpawnerUnstackEvent spawnerUnstackEvent = new SpawnerUnstackEvent(spawner, unstackSource, unstackAmount);
        Bukkit.getPluginManager().callEvent(spawnerUnstackEvent);
        return !spawnerUnstackEvent.isCancelled();
    }

}