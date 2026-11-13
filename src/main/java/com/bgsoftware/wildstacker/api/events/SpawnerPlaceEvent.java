package com.bgsoftware.wildstacker.api.events;

import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SpawnerPlaceEvent extends PlaceEvent {

    public SpawnerPlaceEvent(Player player, StackedSpawner spawner, ItemStack itemInHand) {
        super(player, spawner, itemInHand);
    }

    public StackedSpawner getSpawner() {
        return (StackedSpawner) object;
    }

}