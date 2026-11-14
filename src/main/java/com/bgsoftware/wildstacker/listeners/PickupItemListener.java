package com.bgsoftware.wildstacker.listeners;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.bgsoftware.wildstacker.objects.WStackedItem;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public final class PickupItemListener implements Listener {

    private final WildStackerPlugin plugin;

    public PickupItemListener(WildStackerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent e) {
        Item item = e.getItem();
        if (!ItemUtils.isStackable(item))
            return;

        WStackedItem stackedItem = (WStackedItem) WStackedItem.of(item);

        if (stackedItem.isRemoved()) {
            e.setCancelled(true);
            return;
        }

        ItemStack itemStack = stackedItem.getItemStack();
        LivingEntity entityPicker = e.getEntity();

        boolean customPickupRequired = plugin.getSettings().itemsStackingEnabled ||
                stackedItem.getStackAmount() > itemStack.getMaxStackSize() ||
                (plugin.getSettings().bucketsStackerEnabled && ItemUtils.isBucket(itemStack.getType())) ||
                (plugin.getSettings().stewsStackingEnabled && ItemUtils.isSoup(itemStack));

        if (!customPickupRequired)
            return;

        e.setCancelled(true);

        int initialAmount = stackedItem.getStackAmount();

        if (entityPicker instanceof Player player) {
            stackedItem.giveItemStack(player.getInventory());
            int pickedUp = initialAmount - stackedItem.getStackAmount();
            if (pickedUp > 0) {
                if (plugin.getSettings().itemsSoundEnabled) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F,
                            ((float) Math.random() - (float) Math.random()) * 0.7F + 1.0F);
                }
                player.updateInventory();
            }
        } else {
            EntityEquipment equipment = entityPicker.getEquipment();
            if (equipment != null) {
                ItemStack singleItem = itemStack.clone();
                singleItem.setAmount(1);
                equipment.setItemInMainHand(singleItem);
                stackedItem.decreaseStackAmount(1, true);
                if (stackedItem.getStackAmount() <= 0) {
                    stackedItem.remove();
                }
            }
        }
    }

}