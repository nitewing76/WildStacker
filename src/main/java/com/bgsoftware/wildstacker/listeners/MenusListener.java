package com.bgsoftware.wildstacker.listeners;

import com.bgsoftware.wildstacker.menu.WildMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

@SuppressWarnings("unused")
public final class MenusListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMenuClick(InventoryClickEvent e) {
        Inventory topInventory = e.getView().getTopInventory();
        if (topInventory.getHolder() instanceof WildMenu wildMenu) {
            wildMenu.onButtonClick(e);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMenuClose(InventoryCloseEvent e) {
        Inventory topInventory = e.getView().getTopInventory();
        if (topInventory.getHolder() instanceof WildMenu wildMenu) {
            wildMenu.onMenuClose(e);
        }
    }

}