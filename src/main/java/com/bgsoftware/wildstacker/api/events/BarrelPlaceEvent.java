package com.bgsoftware.wildstacker.api.events;

import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BarrelPlaceEvent extends PlaceEvent {

    public BarrelPlaceEvent(Player player, StackedBarrel barrel, ItemStack itemInHand) {
        super(player, barrel, itemInHand);
    }

    public StackedBarrel getBarrel() {
        return (StackedBarrel) object;
    }

}