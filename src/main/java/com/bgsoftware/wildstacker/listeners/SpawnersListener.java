package com.bgsoftware.wildstacker.listeners;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.enums.UnstackResult;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.menu.SpawnersManageMenu;
import com.bgsoftware.wildstacker.objects.WStackedSpawner;
import com.bgsoftware.wildstacker.utils.GeneralUtils;
import com.bgsoftware.wildstacker.utils.events.EventsCaller;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")
public final class SpawnersListener implements Listener {

    private static final BlockFace[] BLOCK_FACES = new BlockFace[]{
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    private final Set<UUID> inventoryTweaksToggleCommandPlayers = new HashSet<>();
    private final Set<UUID> alreadySpawnersPlacedPlayers = new HashSet<>();
    private final Map<Entity, UUID> explodableSources = new WeakHashMap<>();

    private final WildStackerPlugin plugin;
    private boolean listenToSpawnEvent = true;

    public SpawnersListener(WildStackerPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean handleSpawnerBreak(WildStackerPlugin plugin, StackedSpawner stackedSpawner, int breakAmount, Player player, boolean breakMenu) {
        EntityType entityType = stackedSpawner.getSpawnedType();

        if (stackedSpawner.runUnstack(breakAmount, player) == UnstackResult.SUCCESS) {
            Block block = stackedSpawner.getLocation().getBlock();

            handleSpawnerBreakDrop(plugin, stackedSpawner, player, breakAmount, breakMenu);

            if (stackedSpawner.getStackAmount() <= 0)
                block.setType(Material.AIR);

            Locale.SPAWNER_BREAK.send(player, ItemUtils.getFormattedEntityType(entityType), breakAmount, "0.00");

            return true;
        }

        return false;
    }

    private static void handleSpawnerBreakDrop(WildStackerPlugin plugin, StackedSpawner stackedSpawner, Player player, int breakAmount, boolean breakMenu) {
        if (!breakMenu && player.getGameMode() == GameMode.CREATIVE)
            return;

        if (!breakMenu) {
            if (!plugin.getSettings().silkTouchSpawners)
                return;

            World world = stackedSpawner.getWorld();
            if (world != null && !GeneralUtils.containsOrEmpty(plugin.getSettings().silkWorlds, world.getName()))
                return;

            boolean hasSilkTouch = ItemUtils.isPickaxeAndHasSilkTouch(player.getInventory().getItemInMainHand());
            boolean canDropWithoutSilk = plugin.getSettings().dropSpawnerWithoutSilk && player.hasPermission("wildstacker.nosilkdrop");

            if (!hasSilkTouch && !canDropWithoutSilk)
                return;

            int breakChance = plugin.getSettings().silkTouchBreakChance;
            if (breakChance < 100 && ThreadLocalRandom.current().nextInt(100) >= breakChance)
                return;
        }

        Location dropLocation = ItemUtils.getSafeDropLocation(stackedSpawner.getLocation());

        if (plugin.getSettings().getStackedItem) {
            ItemStack dropItem = EventsCaller.callSpawnerDropEvent(stackedSpawner, player, breakAmount);
            if (plugin.getSettings().dropToInventory || breakMenu) {
                ItemUtils.addItem(dropItem, player.getInventory(), dropLocation);
            } else {
                ItemUtils.dropItem(dropItem, dropLocation);
            }
        } else {
            ItemStack dropItem = EventsCaller.callSpawnerDropEvent(stackedSpawner, player, 1);
            dropItem.setAmount(breakAmount);
            if (plugin.getSettings().dropToInventory || breakMenu) {
                ItemUtils.addItem(dropItem, player.getInventory(), dropLocation);
            } else {
                ItemUtils.dropItem(dropItem, dropLocation);
            }
        }
    }

    private static void handleSpawnerExplodeDrop(WildStackerPlugin plugin, StackedSpawner stackedSpawner, Entity explodeEntity, Player sourcePlayer, int dropAmount) {
        if (!plugin.getSettings().explosionsDropSpawner || dropAmount <= 0)
            return;

        World world = stackedSpawner.getWorld();
        if (world != null && !GeneralUtils.containsOrEmpty(plugin.getSettings().explosionsWorlds, world.getName()))
            return;

        int breakChance = plugin.getSettings().explosionsBreakChance;
        if (breakChance < 100 && ThreadLocalRandom.current().nextInt(100) >= breakChance)
            return;

        Location dropLocation = ItemUtils.getSafeDropLocation(stackedSpawner.getLocation());
        boolean toInventory = plugin.getSettings().explosionsDropToInventory && sourcePlayer != null && sourcePlayer.isOnline();

        if (plugin.getSettings().getStackedItem) {
            ItemStack dropItem = EventsCaller.callSpawnerDropEvent(stackedSpawner, sourcePlayer, dropAmount);
            if (toInventory) {
                ItemUtils.addItem(dropItem, sourcePlayer.getInventory(), dropLocation);
            } else {
                ItemUtils.dropItem(dropItem, dropLocation);
            }
        } else {
            ItemStack dropItem = EventsCaller.callSpawnerDropEvent(stackedSpawner, sourcePlayer, 1);
            dropItem.setAmount(dropAmount);
            if (toInventory) {
                ItemUtils.addItem(dropItem, sourcePlayer.getInventory(), dropLocation);
            } else {
                ItemUtils.dropItem(dropItem, dropLocation);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!plugin.getSettings().spawnersStackingEnabled || e.getBlockPlaced().getType() != Material.SPAWNER)
            return;

        if (!alreadySpawnersPlacedPlayers.add(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            return;
        }

        Executor.sync(() -> alreadySpawnersPlacedPlayers.remove(e.getPlayer().getUniqueId()), 2L);

        try {
            StackedSpawner stackedSpawner = WStackedSpawner.of(e.getBlockPlaced());

            if (!stackedSpawner.isCached())
                return;

            ItemStack itemInHand = e.getItemInHand().clone();
            EquipmentSlot usedHand = ItemUtils.getHand(e);

            EntityType spawnerType = ItemUtils.getSpawnerItemEntityType(itemInHand);
            if (spawnerType == null)
                spawnerType = EntityType.PIG;

            ((WStackedSpawner) stackedSpawner).setSpawnedType(spawnerType);

            int spawnerItemAmount = ItemUtils.getSpawnerItemAmount(itemInHand);

            if (plugin.getSettings().spawnersPlacementPermission && !e.getPlayer().hasPermission("wildstacker.place.*") &&
                    !e.getPlayer().hasPermission("wildstacker.place." + spawnerType.name().toLowerCase(java.util.Locale.ENGLISH))) {
                Locale.SPAWNER_PLACE_BLOCKED.send(e.getPlayer(), "wildstacker.place." + spawnerType.name().toLowerCase(java.util.Locale.ENGLISH));
                e.setCancelled(true);
                stackedSpawner.remove();
                return;
            }

            boolean replaceAir = false;
            ItemStack limitItem = null;

            boolean stackPermission = e.getPlayer().hasPermission("wildstacker.stack.*") ||
                    e.getPlayer().hasPermission("wildstacker.stack." + spawnerType.name().toLowerCase(java.util.Locale.ENGLISH));

            if (stackPermission && e.getPlayer().isSneaking() && plugin.getSettings().spawnersShiftPlaceStack) {
                replaceAir = true;
                spawnerItemAmount *= itemInHand.getAmount();
            }

            if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                int limit = stackedSpawner.getStackLimit();
                if (limit < spawnerItemAmount) {
                    limitItem = ItemUtils.getSpawnerItem(spawnerType, spawnerItemAmount - limit);
                    spawnerItemAmount = limit;
                }
            }

            stackedSpawner.setStackAmount(spawnerItemAmount, false);

            Chunk chunk = e.getBlock().getChunk();

            Optional<CreatureSpawner> spawnerOptional = stackPermission ?
                    ((WStackedSpawner) stackedSpawner).runStack() : Optional.empty();

            if (spawnerOptional.isEmpty()) {
                if (isChunkLimit(chunk, spawnerType)) {
                    e.setCancelled(true);
                    Locale.CHUNK_LIMIT_EXCEEDED.send(e.getPlayer(), ItemUtils.getFormattedEntityType(spawnerType) + " Spawners");
                    stackedSpawner.remove();
                    return;
                }

                if (!plugin.getSettings().nextSpawnerPlacement && !e.getPlayer().hasPermission("wildstacker.nextplace.*") &&
                        !e.getPlayer().hasPermission("wildstacker.nextplace")) {
                    for (BlockFace blockFace : BLOCK_FACES) {
                        if (e.getBlockPlaced().getRelative(blockFace).getType() == Material.SPAWNER) {
                            Locale.NEXT_SPAWNER_PLACEMENT.send(e.getPlayer());
                            e.setCancelled(true);
                            stackedSpawner.remove();
                            return;
                        }
                    }
                }

                if (plugin.getSettings().onlyOneSpawner && !stackedSpawner.getNearbySpawners().isEmpty()) {
                    Locale.ONLY_ONE_SPAWNER.send(e.getPlayer());
                    e.setCancelled(true);
                    stackedSpawner.remove();
                    return;
                }

                if (!EventsCaller.callSpawnerPlaceEvent(e.getPlayer(), stackedSpawner, itemInHand)) {
                    e.setCancelled(true);
                    stackedSpawner.remove();
                    return;
                }

                stackedSpawner.updateName();
            } else {
                e.setCancelled(true);

                if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
                    ItemUtils.removeItemFromHand(e.getPlayer(), 1, usedHand);

                StackedSpawner targetSpawner = WStackedSpawner.of(spawnerOptional.get());
                spawnerItemAmount = targetSpawner.getStackAmount();
            }

            finishSpawnerPlace(e.getPlayer(), replaceAir, usedHand, limitItem, spawnerType, spawnerItemAmount);
        } catch (Exception ex) {
            alreadySpawnersPlacedPlayers.remove(e.getPlayer().getUniqueId());
            throw ex;
        }
    }

    private void finishSpawnerPlace(Player player, boolean replaceAir, EquipmentSlot usedHand,
                                    ItemStack limitItem, EntityType spawnerType, int spawnerItemAmount) {
        if (replaceAir && player.getGameMode() != GameMode.CREATIVE)
            ItemUtils.setItemInHand(player.getInventory(), usedHand, null);

        if (limitItem != null)
            ItemUtils.addItem(limitItem, player.getInventory(), player.getLocation());

        Locale.SPAWNER_PLACE.send(player, ItemUtils.getFormattedEntityType(spawnerType), spawnerItemAmount, "0.00");

        alreadySpawnersPlacedPlayers.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.SPAWNER || !plugin.getSettings().spawnersStackingEnabled)
            return;

        StackedSpawner stackedSpawner = WStackedSpawner.of(e.getBlock());

        e.setCancelled(true);

        if (e.getPlayer().getGameMode() != GameMode.CREATIVE && plugin.getSettings().spawnersMineRequireSilk &&
                !e.getPlayer().hasPermission("wildstacker.silktouch") &&
                !ItemUtils.isPickaxeAndHasSilkTouch(e.getPlayer().getInventory().getItemInMainHand())) {
            Locale.SPAWNER_BREAK_WITHOUT_SILK.send(e.getPlayer());
            return;
        }

        int originalAmount = stackedSpawner.getStackAmount();
        int stackAmount = e.getPlayer().isSneaking() && plugin.getSettings().shiftGetWholeSpawnerStack ? originalAmount : 1;

        handleSpawnerBreak(plugin, stackedSpawner, stackAmount, e.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!plugin.getSettings().spawnersStackingEnabled)
            return;

        UUID explodeSource = explodableSources.get(e.getEntity());
        Player sourcePlayer = null;

        if (e.getEntity() instanceof TNTPrimed tnt) {
            Entity igniter = tnt.getSource();
            if (igniter instanceof Player player) {
                sourcePlayer = player;
            }
        } else {
            sourcePlayer = explodeSource == null ? null : Bukkit.getPlayer(explodeSource);
        }

        Iterator<Block> blockIterator = e.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();

            if (block.getType() != Material.SPAWNER)
                continue;

            StackedSpawner stackedSpawner = WStackedSpawner.of(block);

            int breakAmount = plugin.getSettings().explosionsBreakPercentage == -1 ? 1 :
                    (int) Math.round((plugin.getSettings().explosionsBreakPercentage / 100.0) * stackedSpawner.getStackAmount());
            breakAmount = Math.max(breakAmount, plugin.getSettings().explosionsBreakMinimum);
            breakAmount = Math.min(breakAmount, stackedSpawner.getStackAmount());

            if (stackedSpawner.runUnstack(breakAmount, e.getEntity()) == UnstackResult.SUCCESS) {
                int dropAmount = (int) Math.round((plugin.getSettings().explosionsAmountPercentage / 100.0) * breakAmount);
                dropAmount = Math.max(dropAmount, plugin.getSettings().explosionsAmountMinimum);
                dropAmount = Math.min(dropAmount, breakAmount);

                handleSpawnerExplodeDrop(plugin, stackedSpawner, e.getEntity(), sourcePlayer, dropAmount);

                if (stackedSpawner.getStackAmount() > 0)
                    blockIterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplodeMonitor(EntityExplodeEvent e) {
        explodableSources.remove(e.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplodableLight(PlayerInteractEntityEvent e) {
        if (plugin.getSettings().explosionsDropToInventory && e.getRightClicked() instanceof Creeper &&
                e.getPlayer().getInventory().getItemInMainHand().getType() == Material.FLINT_AND_STEEL)
            explodableSources.put(e.getRightClicked(), e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplodableLight(PlayerInteractEvent e) {
        if (plugin.getSettings().explosionsDropToInventory && e.getClickedBlock() != null &&
                e.getClickedBlock().getType() == Material.TNT && e.getItem() != null &&
                e.getItem().getType() == Material.FLINT_AND_STEEL) {
            Location location = e.getClickedBlock().getLocation();
            World world = location.getWorld();
            if (world == null)
                return;
            Executor.sync(() -> {
                Collection<TNTPrimed> nearbyTnt = world.getNearbyEntitiesByType(TNTPrimed.class, location, 1.5, 1.5, 1.5);
                if (!nearbyTnt.isEmpty()) {
                    explodableSources.put(nearbyTnt.iterator().next(), e.getPlayer().getUniqueId());
                }
            }, 2L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplodableLight(EntityTargetEvent e) {
        if (e.getEntity() instanceof Creeper) {
            if (e.getTarget() instanceof Player player)
                explodableSources.put(e.getEntity(), player.getUniqueId());
            else
                explodableSources.remove(e.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent e) {
        if (!listenToSpawnEvent || !plugin.getSettings().spawnersStackingEnabled)
            return;

        if (!(e.getEntity() instanceof LivingEntity) || e.getSpawner() == null)
            return;

        StackedSpawner stackedSpawner = WStackedSpawner.of(e.getSpawner());
        int stackAmount = stackedSpawner.getStackAmount();

        if (stackAmount <= 1)
            return;

        int additionalMobs = stackAmount - 1;
        Location spawnerLocation = stackedSpawner.getLocation();
        World world = spawnerLocation.getWorld();
        if (world == null)
            return;

        EntityType entityType = e.getEntityType();
        int spawnRange = e.getSpawner().getSpawnRange() > 0 ? e.getSpawner().getSpawnRange() : 4;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        try {
            listenToSpawnEvent = false;

            for (int i = 0; i < additionalMobs; i++) {
                Location spawnLocation = findValidSpawnLocation(world, spawnerLocation, e.getLocation(), spawnRange, random);
                Entity spawned = world.spawnEntity(spawnLocation, entityType, CreatureSpawnEvent.SpawnReason.SPAWNER);
                if (spawned.isValid()) {
                    world.playEffect(spawnLocation, Effect.MOBSPAWNER_FLAMES, 0);
                }
            }
        } finally {
            listenToSpawnEvent = true;
        }
    }

    private Location findValidSpawnLocation(World world, Location spawnerLocation, Location baseSpawnLocation,
                                            int spawnRange, ThreadLocalRandom random) {
        for (int tries = 0; tries < 5; tries++) {
            double x = spawnerLocation.getBlockX() + 0.5D + ((random.nextDouble() - random.nextDouble()) * (double) spawnRange);
            double y = spawnerLocation.getBlockY() + random.nextInt(3) - 1;
            double z = spawnerLocation.getBlockZ() + 0.5D + ((random.nextDouble() - random.nextDouble()) * (double) spawnRange);

            Location candidate = new Location(world, x, y, z, random.nextFloat() * 360.0F, 0.0F);
            Block block = candidate.getBlock();
            Block above = block.getRelative(BlockFace.UP);

            if (block.isPassable() && !block.isLiquid() && above.isPassable() && !above.isLiquid()) {
                return candidate;
            }
        }
        return baseSpawnLocation.clone();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerChange(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null ||
                !ItemUtils.isValidAndSpawnEgg(e.getItem()) || e.getClickedBlock().getType() != Material.SPAWNER)
            return;

        if (!plugin.getSettings().changeUsingEggs || !e.getPlayer().hasPermission("wildstacker.eggchange")) {
            e.setCancelled(true);
            return;
        }

        StackedSpawner stackedSpawner = WStackedSpawner.of(e.getClickedBlock());
        EntityType eggEntityType = ItemUtils.getEntityType(e.getItem());

        if (eggEntityType == null || stackedSpawner.getSpawnedType() == eggEntityType) {
            e.setCancelled(true);
            return;
        }

        if (plugin.getSettings().eggsStackMultiply && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            int eggsCountInInventory = ItemUtils.countItem(e.getPlayer().getInventory(), e.getItem());
            int requiredCount = stackedSpawner.getStackAmount();
            if (requiredCount > eggsCountInInventory) {
                Locale.SPAWNER_CHANGE_NOT_ENOUGH_ITEMS.send(e.getPlayer(), eggsCountInInventory, requiredCount);
                e.setCancelled(true);
                return;
            }
        }

        ItemStack inHand = e.getItem().clone();

        Executor.sync(() -> {
            if (e.getClickedBlock().getType() == Material.SPAWNER &&
                    e.getClickedBlock().getState() instanceof CreatureSpawner liveSpawner) {
                ((WStackedSpawner) stackedSpawner).setSpawnedType(liveSpawner.getSpawnedType());
            }
            stackedSpawner.updateName();
            if (e.getPlayer().getGameMode() != GameMode.CREATIVE && plugin.getSettings().eggsStackMultiply && stackedSpawner.getStackAmount() > 1) {
                ItemUtils.removeItem(e.getPlayer().getInventory(), inHand, stackedSpawner.getStackAmount() - 1);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null ||
                e.getClickedBlock().getType() != Material.SPAWNER ||
                !plugin.getSettings().spawnersStackingEnabled || ItemUtils.isOffHand(e))
            return;

        if (e.getItem() != null && (e.getItem().getType() == Material.SPAWNER || ItemUtils.isValidAndSpawnEgg(e.getItem())))
            return;

        StackedSpawner stackedSpawner = WStackedSpawner.of(e.getClickedBlock());

        if (plugin.getSettings().manageMenuEnabled && (!plugin.getSettings().sneakingOpenMenu || e.getPlayer().isSneaking())) {
            SpawnersManageMenu.open(e.getPlayer(), stackedSpawner);
            e.setCancelled(true);
        } else if (plugin.getSettings().floatingSpawnerNames) {
            int spawnerAmount = stackedSpawner.getStackAmount();

            if (spawnerAmount < 1 || (spawnerAmount == 1 && !plugin.getSettings().spawnersUnstackedCustomName))
                return;

            String customName = plugin.getSettings().spawnersCustomName;

            if (customName == null || customName.isEmpty())
                return;

            ((WStackedSpawner) stackedSpawner).setCachedDisplayName(
                    ItemUtils.getFormattedEntityType(stackedSpawner.getSpawnedType()));

            customName = plugin.getSettings().spawnersNameBuilder.build(stackedSpawner);
            ((WStackedSpawner) stackedSpawner).setHologramName(customName, true);

            Executor.sync(((WStackedSpawner) stackedSpawner)::removeHologram, 60L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!plugin.getSettings().inventoryTweaksEnabled || e.getClickedInventory() == null)
            return;

        String permission = plugin.getSettings().inventoryTweaksPermission;

        if (!permission.isEmpty() && !e.getWhoClicked().hasPermission(permission))
            return;

        if (!plugin.getSettings().inventoryTweaksCommand.isEmpty() &&
                !inventoryTweaksToggleCommandPlayers.contains(e.getWhoClicked().getUniqueId()))
            return;

        InventoryAction action = e.getAction();

        switch (action) {
            case PICKUP_HALF:
                if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.SPAWNER) {
                    int spawnersAmount = ItemUtils.getSpawnerItemAmount(e.getCurrentItem());
                    if (spawnersAmount > 1 && e.getCurrentItem().getAmount() == 1) {
                        e.setCancelled(true);
                        EntityType entityType = ItemUtils.getSpawnerItemEntityType(e.getCurrentItem());
                        e.getClickedInventory().setItem(e.getSlot(), ItemUtils.getSpawnerItem(entityType, spawnersAmount / 2));
                        e.getWhoClicked().setItemOnCursor(ItemUtils.getSpawnerItem(entityType, spawnersAmount - (spawnersAmount / 2)));
                    }
                }
                break;
            case PLACE_ALL:
            case PLACE_ONE:
                if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) {
                    if (e.getCursor().getType() == Material.SPAWNER) {
                        int cursorAmount = ItemUtils.getSpawnerItemAmount(e.getCursor());
                        if (cursorAmount > 1 && e.getCursor().getAmount() == 1 && action == InventoryAction.PLACE_ONE) {
                            e.setCancelled(true);
                            EntityType entityType = ItemUtils.getSpawnerItemEntityType(e.getCursor());
                            e.getWhoClicked().setItemOnCursor(ItemUtils.getSpawnerItem(entityType, cursorAmount - 1));
                            e.getClickedInventory().setItem(e.getSlot(), ItemUtils.getSpawnerItem(entityType, 1));
                        }
                    }
                    break;
                }
                // Fallthrough to SWAP_WITH_CURSOR when clicking onto an existing item
            case SWAP_WITH_CURSOR:
                if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.SPAWNER &&
                        e.getCursor().getType() == Material.SPAWNER) {
                    int currentAmount = ItemUtils.getSpawnerItemAmount(e.getCurrentItem()) * e.getCurrentItem().getAmount();
                    int cursorAmount = ItemUtils.getSpawnerItemAmount(e.getCursor()) * e.getCursor().getAmount();
                    EntityType currentType = ItemUtils.getSpawnerItemEntityType(e.getCurrentItem());
                    EntityType cursorType = ItemUtils.getSpawnerItemEntityType(e.getCursor());
                    if (currentType == cursorType) {
                        e.setCancelled(true);
                        e.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
                        e.getClickedInventory().setItem(e.getSlot(), ItemUtils.getSpawnerItem(currentType, currentAmount + cursorAmount));
                    }
                }
                break;
            case COLLECT_TO_CURSOR:
                if (e.getCursor().getType() == Material.SPAWNER) {
                    int newCursorAmount = ItemUtils.getSpawnerItemAmount(e.getCursor());
                    if (e.getCursor().getAmount() == 1) {
                        e.setCancelled(true);
                        EntityType entityType = ItemUtils.getSpawnerItemEntityType(e.getCursor());
                        for (int i = 0; i < e.getClickedInventory().getSize(); i++) {
                            ItemStack itemStack = e.getClickedInventory().getItem(i);
                            if (itemStack != null && itemStack.getType() == Material.SPAWNER) {
                                if (ItemUtils.getSpawnerItemEntityType(itemStack) == entityType) {
                                    newCursorAmount += ItemUtils.getSpawnerItemAmount(itemStack) * itemStack.getAmount();
                                    e.getClickedInventory().setItem(i, new ItemStack(Material.AIR));
                                }
                            }
                        }
                        e.getWhoClicked().setItemOnCursor(ItemUtils.getSpawnerItem(entityType, newCursorAmount));
                    }
                }
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (plugin.getSettings().inventoryTweaksCommand.isEmpty())
            return;

        String permission = plugin.getSettings().inventoryTweaksPermission;

        if (!permission.isEmpty() && !e.getPlayer().hasPermission(permission))
            return;

        for (String commandSyntax : plugin.getSettings().inventoryTweaksCommand.split(",")) {
            commandSyntax = "/" + commandSyntax.trim();

            if (!e.getMessage().equalsIgnoreCase(commandSyntax) && !e.getMessage().startsWith(commandSyntax + " "))
                continue;

            e.setCancelled(true);

            if (inventoryTweaksToggleCommandPlayers.remove(e.getPlayer().getUniqueId())) {
                Locale.INVENTORY_TWEAKS_TOGGLE_OFF.send(e.getPlayer());
            } else {
                inventoryTweaksToggleCommandPlayers.add(e.getPlayer().getUniqueId());
                Locale.INVENTORY_TWEAKS_TOGGLE_ON.send(e.getPlayer());
            }

            return;
        }
    }

    private boolean isChunkLimit(Chunk chunk, EntityType entityType) {
        int chunkLimit = plugin.getSettings().spawnersChunkLimit;

        if (chunkLimit <= 0)
            return false;

        return plugin.getSystemManager().getStackedSpawners(chunk).stream()
                .filter(stackedSpawner -> !plugin.getSettings().perSpawnerLimit || stackedSpawner.getSpawnedType() == entityType)
                .count() > chunkLimit;
    }

}