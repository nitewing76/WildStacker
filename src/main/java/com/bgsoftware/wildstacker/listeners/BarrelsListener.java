package com.bgsoftware.wildstacker.listeners;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.enums.UnstackResult;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.menu.BarrelsPlaceMenu;
import com.bgsoftware.wildstacker.objects.WStackedBarrel;
import com.bgsoftware.wildstacker.utils.events.EventsCaller;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public final class BarrelsListener implements Listener {

    private final Set<UUID> barrelsToggleCommandPlayers = new HashSet<>();
    private final Set<UUID> alreadyBarrelsPlacedPlayers = new HashSet<>();
    private final WildStackerPlugin plugin;

    public BarrelsListener(WildStackerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBarrelPlace(BlockPlaceEvent e) {
        if (!plugin.getSettings().barrelsStackingEnabled)
            return;

        ItemStack inHand = e.getItemInHand();

        if (inHand == null || inHand.getType() != e.getBlock().getType())
            return;

        if (!isBarrelBlock(e.getBlock()))
            return;

        EquipmentSlot usedHand = ItemUtils.getHand(e);

        try {
            int toPlace = ItemUtils.getSpawnerItemAmount(inHand);

            if (toPlace <= 1 && plugin.getSettings().barrelsToggleCommand && !barrelsToggleCommandPlayers.contains(e.getPlayer().getUniqueId()))
                return;

            if (!plugin.getSettings().barrelsRequiredPermission.isEmpty() &&
                    !e.getPlayer().hasPermission(plugin.getSettings().barrelsRequiredPermission)) {
                e.setCancelled(true);
                Locale.BARREL_NO_PERMISSION.send(e.getPlayer());
                return;
            }

            if (!alreadyBarrelsPlacedPlayers.add(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                return;
            }

            Executor.sync(() -> alreadyBarrelsPlacedPlayers.remove(e.getPlayer().getUniqueId()), 2L);

            StackedBarrel stackedBarrel = WStackedBarrel.of(e.getBlockPlaced());

            if (!stackedBarrel.isCached()) {
                stackedBarrel.remove();
                return;
            }

            if (e.getBlockPlaced().getY() > e.getBlockAgainst().getY() && plugin.getSystemManager().isStackedBarrel(e.getBlockAgainst())) {
                e.setCancelled(true);
                stackedBarrel.remove();
                return;
            }

            boolean replaceAir = false;

            if (e.getPlayer().isSneaking() && plugin.getSettings().barrelsShiftPlaceStack) {
                toPlace *= inHand.getAmount();
                replaceAir = true;
            }

            stackedBarrel.setStackAmount(toPlace, false);

            Chunk chunk = e.getBlock().getChunk();

            Optional<Block> blockOptional = ((WStackedBarrel) stackedBarrel).runStack();

            e.setCancelled(true);

            if (blockOptional.isEmpty()) {
                if (isChunkLimit(chunk)) {
                    Locale.CHUNK_LIMIT_EXCEEDED.send(e.getPlayer(), ItemUtils.getFormattedType(stackedBarrel.getBarrelItem(1)) + " Barrels");
                    stackedBarrel.remove();
                    return;
                }

                if (!EventsCaller.callBarrelPlaceEvent(e.getPlayer(), stackedBarrel, inHand)) {
                    stackedBarrel.remove();
                    return;
                }

                if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
                    ItemUtils.removeItemFromHand(e.getPlayer(), 1, usedHand);

                ((WStackedBarrel) stackedBarrel).setReady(false);

                Executor.sync(() -> {
                    e.getBlock().setType(Material.CAULDRON);
                    stackedBarrel.createDisplayBlock();
                    ((WStackedBarrel) stackedBarrel).setReady(true);
                }, 1L);

                stackedBarrel.updateName();

                Locale.BARREL_PLACE.send(e.getPlayer(), ItemUtils.getFormattedType(stackedBarrel.getBarrelItem(1)));
            } else {
                if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
                    ItemUtils.removeItemFromHand(e.getPlayer(), 1, usedHand);

                StackedBarrel targetBarrel = WStackedBarrel.of(blockOptional.get());
                Locale.BARREL_UPDATE.send(e.getPlayer(), ItemUtils.getFormattedType(targetBarrel.getBarrelItem(1)), targetBarrel.getStackAmount());
            }

            finishBarrelPlace(e.getPlayer(), usedHand, replaceAir);
        } catch (Exception ex) {
            alreadyBarrelsPlacedPlayers.remove(e.getPlayer().getUniqueId());
            throw ex;
        }
    }

    private void finishBarrelPlace(Player player, EquipmentSlot usedHand, boolean replaceAir) {
        if (replaceAir && player.getGameMode() != GameMode.CREATIVE)
            ItemUtils.setItemInHand(player.getInventory(), usedHand, null);

        alreadyBarrelsPlacedPlayers.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBarrelBreak(BlockBreakEvent e) {
        if (!plugin.getSettings().barrelsStackingEnabled ||
                (e.getBlock().getType() != Material.CAULDRON && e.getBlock().getType() != Material.WATER_CAULDRON))
            return;

        if (!plugin.getSystemManager().isStackedBarrel(e.getBlock()))
            return;

        StackedBarrel stackedBarrel = WStackedBarrel.of(e.getBlock());
        int stackSize = stackedBarrel.getStackAmount();

        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ItemStack dropStack = EventsCaller.callBarrelDropEvent(stackedBarrel, e.getPlayer(), stackSize);

            if (plugin.getSettings().barrelsAutoPickup) {
                ItemUtils.addItem(dropStack, e.getPlayer().getInventory(), e.getBlock().getLocation());
            } else {
                ItemUtils.dropItem(dropStack, e.getBlock().getLocation());
            }
        }

        e.setCancelled(true);

        if (stackedBarrel.runUnstack(stackSize, e.getPlayer()) == UnstackResult.SUCCESS) {
            e.getBlock().setType(Material.AIR);
            Locale.BARREL_BREAK.send(e.getPlayer(), stackSize, ItemUtils.getFormattedType(stackedBarrel.getBarrelItem(1)));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBarrelClick(PlayerInteractEvent e) {
        if (!plugin.getSettings().barrelsStackingEnabled)
            return;

        if (ItemUtils.isOffHand(e) || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null)
            return;

        if (!plugin.getSystemManager().isStackedBarrel(e.getClickedBlock()))
            return;

        StackedBarrel stackedBarrel = WStackedBarrel.of(e.getClickedBlock());

        if (e.getItem() != null)
            return;

        ItemStack offHandItem = ItemUtils.getItemInOffhand(e.getPlayer().getInventory());

        if (offHandItem != null && offHandItem.getType() == stackedBarrel.getType())
            return;

        if (e.getPlayer().isSneaking() && plugin.getSettings().barrelsPlaceInventory) {
            BarrelsPlaceMenu.open(e.getPlayer(), stackedBarrel);
            e.setCancelled(true);
        } else if (stackedBarrel.runUnstack(1, e.getPlayer()) == UnstackResult.SUCCESS) {
            e.setCancelled(true);
            if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                ItemStack dropStack = EventsCaller.callBarrelDropEvent(stackedBarrel, e.getPlayer(), 1);

                if (plugin.getSettings().barrelsAutoPickup) {
                    ItemUtils.addItem(dropStack, e.getPlayer().getInventory(), e.getClickedBlock().getLocation());
                } else {
                    ItemUtils.dropItem(dropStack, e.getClickedBlock().getLocation());
                }
            }

            if (stackedBarrel.getStackAmount() <= 0)
                e.getClickedBlock().setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!plugin.getSettings().barrelsStackingEnabled)
            return;

        Iterator<Block> blockListIterator = e.blockList().iterator();
        while (blockListIterator.hasNext()) {
            Block block = blockListIterator.next();

            if (!plugin.getSystemManager().isStackedBarrel(block))
                continue;

            StackedBarrel stackedBarrel = WStackedBarrel.of(block);

            int amount = plugin.getSettings().explosionsBreakBarrelStack ? stackedBarrel.getStackAmount() : 1;

            if (stackedBarrel.runUnstack(amount, e.getEntity()) == UnstackResult.SUCCESS) {
                ItemStack barrelItem = EventsCaller.callBarrelDropEvent(stackedBarrel, null, amount);
                ItemUtils.dropItem(barrelItem, block.getLocation());
                if (stackedBarrel.getStackAmount() <= 0) {
                    block.setType(Material.AIR);
                }
            }

            blockListIterator.remove();
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (!plugin.getSettings().barrelsStackingEnabled || !plugin.getSettings().barrelsToggleCommand ||
                !e.getPlayer().hasPermission("wildstacker.toggle"))
            return;

        String commandSyntax = "/" + plugin.getSettings().barrelsToggleCommandSyntax;

        if (!e.getMessage().equalsIgnoreCase(commandSyntax) && !e.getMessage().startsWith(commandSyntax + " "))
            return;

        e.setCancelled(true);

        if (barrelsToggleCommandPlayers.remove(e.getPlayer().getUniqueId())) {
            Locale.BARREL_TOGGLE_OFF.send(e.getPlayer());
        } else {
            barrelsToggleCommandPlayers.add(e.getPlayer().getUniqueId());
            Locale.BARREL_TOGGLE_ON.send(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (!plugin.getSettings().barrelsStackingEnabled)
            return;

        for (Block block : e.getBlocks()) {
            if (plugin.getSystemManager().isStackedBarrel(block)) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (!plugin.getSettings().barrelsStackingEnabled)
            return;

        for (Block block : e.getBlocks()) {
            if (plugin.getSystemManager().isStackedBarrel(block)) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCauldronFill(CauldronLevelChangeEvent e) {
        if (!plugin.getSettings().barrelsStackingEnabled)
            return;

        if (plugin.getSystemManager().isStackedBarrel(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    private boolean isBarrelBlock(Block block) {
        Material type = block.getType();
        return (plugin.getSettings().whitelistedBarrels.size() == 0 ||
                plugin.getSettings().whitelistedBarrels.contains(type)) &&
                !plugin.getSettings().blacklistedBarrels.contains(type) &&
                !plugin.getSettings().barrelsDisabledWorlds.contains(block.getWorld().getName());
    }

    private boolean isChunkLimit(Chunk chunk) {
        int chunkLimit = plugin.getSettings().barrelsChunkLimit;

        if (chunkLimit <= 0)
            return false;

        return plugin.getSystemManager().getStackedBarrels(chunk).size() > chunkLimit;
    }

}