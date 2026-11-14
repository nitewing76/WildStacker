package com.bgsoftware.wildstacker.objects;

import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.UnloadedStackedBarrel;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class WUnloadedStackedBarrel extends WUnloadedStackedObject implements UnloadedStackedBarrel {

    private final ItemStack barrelItem;

    public WUnloadedStackedBarrel(StackedBarrel stackedBarrel) {
        super(stackedBarrel);
        this.barrelItem = stackedBarrel.getBarrelItem(1);
    }

    public WUnloadedStackedBarrel(String worldName, int locX, int locY, int locZ, int stackAmount, ItemStack barrelItem) {
        super(worldName, locX, locY, locZ, stackAmount);
        this.barrelItem = barrelItem;
    }

    @Override
    public ItemStack getBarrelItem(int amount) {
        if (this.barrelItem == null) {
            return new ItemStack(Material.AIR);
        }
        ItemStack item = this.barrelItem.clone();
        item.setAmount(amount);
        return item;
    }

    @Override
    public void remove() {
        plugin.getDataHandler().stackedBarrelStore.removeUnloaded(this);
        plugin.getDataHandler().deleteBarrel(this);
    }

}
