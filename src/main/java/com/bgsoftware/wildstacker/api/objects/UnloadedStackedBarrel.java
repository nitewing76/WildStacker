package com.bgsoftware.wildstacker.api.objects;

import org.bukkit.inventory.ItemStack;

public interface UnloadedStackedBarrel extends UnloadedStackedObject {

    ItemStack getBarrelItem(int amount);

}
