package com.bgsoftware.wildstacker.menu;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.objects.WStackedBarrel;
import com.bgsoftware.wildstacker.utils.events.EventsCaller;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.ref.WeakReference;

public final class BarrelsPlaceMenu extends WildMenu {

    private static final WildStackerPlugin plugin = WildStackerPlugin.getPlugin();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final WeakReference<StackedBarrel> stackedBarrel;

    private BarrelsPlaceMenu(StackedBarrel stackedBarrel) {
        super("barrelsPlaceMenu");
        this.stackedBarrel = new WeakReference<>(stackedBarrel);

        String title = plugin.getSettings().barrelsPlaceInventoryTitle
                .replace("{0}", ((WStackedBarrel) stackedBarrel).getCachedDisplayName());
        this.inventory = Bukkit.createInventory(this, 36, SECTION_SERIALIZER.deserialize(title));
        ((WStackedBarrel) stackedBarrel).linkInventory(this.inventory);
    }

    public static void open(Player player, StackedBarrel stackedBarrel) {
        new BarrelsPlaceMenu(stackedBarrel).openMenu(player);
    }

    @Override
    public void onButtonClick(InventoryClickEvent e) {
        // Allow players to place items into this inventory freely
    }

    @Override
    public void onPlayerClick(InventoryClickEvent e) {
    }

    @Override
    public void onMenuClose(InventoryCloseEvent e) {
        StackedBarrel barrel = this.stackedBarrel.get();
        Player player = (Player) e.getPlayer();

        if (barrel != null) {
            ((WStackedBarrel) barrel).unlinkInventory(e.getInventory());
        }

        int toAdd = 0;
        int limit = barrel == null ? 0 : barrel.getStackLimit() - barrel.getStackAmount();
        ItemStack barrelItem = barrel == null ? null : barrel.getBarrelItem(1);

        for (ItemStack itemStack : e.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR)
                continue;

            if (barrel != null && barrel.isCached() && barrelItem != null && isSimilarBarrelItem(itemStack, barrelItem)) {
                int itemAmount = ItemUtils.getSpawnerItemAmount(itemStack) * itemStack.getAmount();
                if (toAdd + itemAmount <= limit) {
                    toAdd += itemAmount;
                } else {
                    int canAdd = Math.max(0, limit - toAdd);
                    if (canAdd > 0) {
                        toAdd += canAdd;
                    }
                    int leftOver = itemAmount - canAdd;
                    if (leftOver > 0) {
                        ItemStack refund = barrel.getBarrelItem(leftOver);
                        ItemUtils.addItem(refund, player.getInventory(), player.getLocation());
                    }
                }
            } else {
                ItemUtils.addItem(itemStack, player.getInventory(), player.getLocation());
            }
        }

        if (barrel != null && toAdd > 0) {
            if (EventsCaller.callBarrelPlaceInventoryEvent(player, barrel, toAdd)) {
                barrel.increaseStackAmount(toAdd, true);
                Locale.BARREL_UPDATE.send(player, ItemUtils.getFormattedType(barrelItem), barrel.getStackAmount());
            } else {
                ItemStack refund = barrel.getBarrelItem(toAdd);
                ItemUtils.addItem(refund, player.getInventory(), player.getLocation());
            }
        }
    }

    @Override
    public void openMenu(Player player) {
        if (this.inventory != null) {
            player.openInventory(this.inventory);
        }
    }

    private boolean isSimilarBarrelItem(ItemStack candidate, ItemStack barrelBase) {
        if (candidate.getType() != barrelBase.getType())
            return false;
        ItemStack cleanCandidate = ItemUtils.setSpawnerItemAmount(candidate.clone(), 1);
        if (cleanCandidate.hasItemMeta() && cleanCandidate.getItemMeta().hasDisplayName()) {
            var meta = cleanCandidate.getItemMeta();
            meta.displayName(null);
            cleanCandidate.setItemMeta(meta);
        }
        return cleanCandidate.isSimilar(barrelBase);
    }

}