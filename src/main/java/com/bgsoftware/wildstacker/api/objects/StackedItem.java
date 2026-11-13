package com.bgsoftware.wildstacker.api.objects;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public interface StackedItem extends StackedObject<Item> {

    Item getItem();

    UUID getUniqueId();

    String getCustomName();

    void setCustomName(String customName);

    ItemStack getItemStack();

    void setItemStack(ItemStack itemStack);

    void giveItemStack(Inventory inventory);

    void giveItemStack(PlayerInventory playerInventory, Location dropLocation);

}
