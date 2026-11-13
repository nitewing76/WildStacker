package com.bgsoftware.wildstacker.api.objects;

import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface StackedSpawner extends StackedObject<CreatureSpawner> {

    CreatureSpawner getSpawner();

    EntityType getSpawnedType();

    ItemStack getDropItem();

    ItemStack getDropItem(int amount);

    List<StackedSpawner> getNearbySpawners();

}
