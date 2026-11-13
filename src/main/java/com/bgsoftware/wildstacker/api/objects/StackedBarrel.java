package com.bgsoftware.wildstacker.api.objects;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public interface StackedBarrel extends StackedObject<Block> {

    Block getBlock();

    Material getType();

    short getData();

    void createDisplayBlock();

    void removeDisplayBlock();

    ItemStack getBarrelItem(int amount);

}
