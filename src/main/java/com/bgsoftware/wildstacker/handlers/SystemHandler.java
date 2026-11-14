package com.bgsoftware.wildstacker.handlers;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.handlers.SystemManager;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.bgsoftware.wildstacker.api.objects.StackedObject;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.api.objects.UnloadedStackedBarrel;
import com.bgsoftware.wildstacker.api.objects.UnloadedStackedSpawner;
import com.bgsoftware.wildstacker.database.DBSession;
import com.bgsoftware.wildstacker.database.sql.IDatabaseTransaction;
import com.bgsoftware.wildstacker.database.sql.InsertSQLDatabaseTransaction;
import com.bgsoftware.wildstacker.database.sql.SQLDatabaseTransaction;
import com.bgsoftware.wildstacker.objects.WStackedBarrel;
import com.bgsoftware.wildstacker.objects.WStackedItem;
import com.bgsoftware.wildstacker.objects.WStackedObject;
import com.bgsoftware.wildstacker.objects.WStackedSpawner;
import com.bgsoftware.wildstacker.objects.WUnloadedStackedBarrel;
import com.bgsoftware.wildstacker.objects.WUnloadedStackedSpawner;
import com.bgsoftware.wildstacker.tasks.ItemsMerger;
import com.bgsoftware.wildstacker.utils.GeneralUtils;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class SystemHandler implements SystemManager {

    public static final int ENTITIES_STAGE = (1 << 0);
    public static final int CHUNK_STAGE = (1 << 1);
    public static final int CHUNK_FULL_STAGE = ENTITIES_STAGE | CHUNK_STAGE;

    private final WildStackerPlugin plugin;
    private final DataHandler dataHandler;

    private final Set<UUID> itemsDisabledNames = new HashSet<>();
    private boolean loadedData = false;

    public SystemHandler(WildStackerPlugin plugin) {
        this.plugin = plugin;
        this.dataHandler = plugin.getDataHandler();

        Executor.sync(ItemsMerger::start, 1L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::performCacheClear, 100L, 100L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::performCacheSave, 300L, 300L);
    }

    @Override
    public void removeStackObject(StackedObject<?> stackedObject) {
        if (stackedObject instanceof StackedItem)
            dataHandler.stackedItemStore.remove(((StackedItem) stackedObject).getItem().getEntityId());
        else if (stackedObject instanceof StackedSpawner)
            dataHandler.removeStackedSpawner((StackedSpawner) stackedObject);
        else if (stackedObject instanceof StackedBarrel)
            dataHandler.removeStackedBarrel((StackedBarrel) stackedObject);

        markToBeUnsaved(stackedObject);
    }

    @Override
    public StackedItem getStackedItem(Item item) {
        StackedItem stackedItem = dataHandler.stackedItemStore.get(item.getEntityId());
        if (stackedItem != null) {
            return stackedItem;
        }

        stackedItem = new WStackedItem(item);

        if (stackedItem.isCached()) {
            dataHandler.stackedItemStore.store(item.getEntityId(), stackedItem);

            Executor.sync(() -> {
                if (item.isDead())
                    dataHandler.stackedItemStore.remove(item.getEntityId());
            }, 10L);
        }

        dataHandler.stackedItemStore.loadUnloaded(item.getUniqueId(), stackedItem);

        return stackedItem;
    }

    @Override
    public StackedSpawner getStackedSpawner(CreatureSpawner spawner) {
        return spawner == null ? null : getStackedSpawner(spawner.getLocation());
    }

    @Override
    public StackedSpawner getStackedSpawner(Location location) {
        if (location == null)
            return null;

        StackedSpawner stackedSpawner = dataHandler.stackedSpawnerStore.get(location);
        if (stackedSpawner != null)
            return stackedSpawner;

        if (location.getBlock().getType() != Material.SPAWNER)
            return null;

        stackedSpawner = new WStackedSpawner((CreatureSpawner) location.getBlock().getState());

        if (stackedSpawner.isCached())
            dataHandler.addStackedSpawner(stackedSpawner);

        return stackedSpawner;
    }

    @Override
    public StackedBarrel getStackedBarrel(Block block) {
        return getStackedBarrel(block == null ? null : block.getLocation());
    }

    @Override
    public StackedBarrel getStackedBarrel(Location location) {
        if (location == null)
            return null;

        StackedBarrel stackedBarrel = dataHandler.stackedBarrelStore.get(location);
        if (stackedBarrel != null)
            return stackedBarrel;

        stackedBarrel = new WStackedBarrel(location.getBlock(), ItemUtils.getFromBlock(location.getBlock()));

        if (stackedBarrel.isCached())
            dataHandler.addStackedBarrel(stackedBarrel);

        return stackedBarrel;
    }

    @Override
    public List<StackedItem> getStackedItems() {
        return dataHandler.stackedItemStore.getAll();
    }

    @Override
    public List<StackedSpawner> getStackedSpawners() {
        return dataHandler.stackedSpawnerStore.getAll();
    }

    @Override
    public List<StackedSpawner> getStackedSpawners(Chunk chunk) {
        return getStackedSpawners(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    @Override
    public List<StackedSpawner> getStackedSpawners(World world, int chunkX, int chunkZ) {
        return dataHandler.stackedSpawnerStore.getAllFromChunk(world.getName(), chunkX, chunkZ);
    }

    @Override
    public List<UnloadedStackedSpawner> getAllSpawners() {
        return getAllStackedSpawners();
    }

    public List<UnloadedStackedSpawner> getAllStackedSpawners() {
        if (dataHandler.stackedSpawnerStore.size() == 0 && dataHandler.stackedSpawnerStore.sizeUnloaded() == 0)
            return Collections.emptyList();

        List<UnloadedStackedSpawner> spawners = new LinkedList<>();

        dataHandler.stackedSpawnerStore.iterateUnloaded(spawners::add);
        dataHandler.stackedSpawnerStore.getAll().forEach(stackedSpawner ->
                spawners.add(new WUnloadedStackedSpawner(stackedSpawner)));

        return spawners.isEmpty() ? Collections.emptyList() : spawners;
    }

    @Override
    public List<StackedBarrel> getStackedBarrels() {
        return dataHandler.stackedBarrelStore.getAll();
    }

    @Override
    public List<StackedBarrel> getStackedBarrels(Chunk chunk) {
        return getStackedBarrels(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    @Override
    public List<StackedBarrel> getStackedBarrels(World world, int chunkX, int chunkZ) {
        return dataHandler.stackedBarrelStore.getAllFromChunk(world.getName(), chunkX, chunkZ);
    }

    @Override
    public List<UnloadedStackedBarrel> getAllBarrels() {
        return getAllStackedBarrels();
    }

    public List<UnloadedStackedBarrel> getAllStackedBarrels() {
        if (dataHandler.stackedBarrelStore.size() == 0 && dataHandler.stackedBarrelStore.sizeUnloaded() == 0)
            return Collections.emptyList();

        List<UnloadedStackedBarrel> barrels = new LinkedList<>();

        dataHandler.stackedBarrelStore.iterateUnloaded(barrels::add);
        dataHandler.stackedBarrelStore.getAll().forEach(stackedBarrel ->
                barrels.add(new WUnloadedStackedBarrel(stackedBarrel)));

        return barrels.isEmpty() ? Collections.emptyList() : barrels;
    }

    @Override
    public boolean isStackedSpawner(Block block) {
        return block != null && block.getType() == Material.SPAWNER && isStackedSpawner(block.getLocation());
    }

    @Override
    public boolean isStackedSpawner(Location location) {
        return location != null && dataHandler.stackedSpawnerStore.get(location) != null;
    }

    @Override
    public boolean isStackedBarrel(Block block) {
        return block != null && (block.getType() == Material.CAULDRON || block.getType() == Material.WATER_CAULDRON)
                && isStackedBarrel(block.getLocation());
    }

    @Override
    public boolean isStackedBarrel(Location location) {
        return location != null && dataHandler.stackedBarrelStore.get(location) != null;
    }

    @Override
    public void performCacheClear() {
        List<StackedObject> stackedObjects = dataHandler.getStackedObjects();

        for (StackedObject stackedObject : stackedObjects) {
            if (!((WStackedObject<?>) stackedObject).isReady()) {
                continue;
            }

            if (stackedObject instanceof StackedItem) {
                StackedItem stackedItem = (StackedItem) stackedObject;
                if (stackedItem.getItem() == null || (GeneralUtils.isChunkLoaded(stackedItem.getItem().getLocation()) && stackedItem.getItem().isDead()))
                    removeStackObject(stackedObject);
            } else if (stackedObject instanceof StackedSpawner) {
                StackedSpawner stackedSpawner = (StackedSpawner) stackedObject;
                if (GeneralUtils.isChunkLoaded(stackedSpawner.getLocation()) && !isStackedSpawner(stackedSpawner.getSpawner().getBlock())) {
                    removeStackObject(stackedObject);
                }
            } else if (stackedObject instanceof StackedBarrel) {
                StackedBarrel stackedBarrel = (StackedBarrel) stackedObject;
                Block block = stackedBarrel.getBlock();
                if (GeneralUtils.isChunkLoaded(stackedBarrel.getLocation()) && !isStackedBarrel(block)) {
                    if (block.getType() == Material.WATER_CAULDRON) {
                        Executor.sync(() -> block.setType(Material.CAULDRON));
                    } else {
                        removeStackObject(stackedObject);
                        stackedBarrel.removeDisplayBlock();
                    }
                }
            }
        }
    }

    public void performCacheSave() {
        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(this::performCacheSave);
            return;
        }

        Set<StackedObject> savedObjects = new HashSet<>(dataHandler.OBJECTS_TO_SAVE);
        dataHandler.OBJECTS_TO_SAVE.clear();

        SQLDatabaseTransaction<?> spawnersInsertTransaction = null;
        SQLDatabaseTransaction<?> barrelsInsertTransaction = null;
        SQLDatabaseTransaction<?> itemsInsertTransaction = null;

        for (StackedObject<?> stackedObject : savedObjects) {
            if (stackedObject instanceof StackedItem) {
                StackedItem stackedItem = (StackedItem) stackedObject;
                saveItem(stackedItem);
                if (plugin.getSettings().storeItems && stackedItem.getItem() != null && !stackedItem.getItem().isDead()) {
                    if (itemsInsertTransaction == null) {
                        itemsInsertTransaction = new InsertSQLDatabaseTransaction("items", Arrays.asList("uuid", "stackAmount"));
                    }
                    itemsInsertTransaction
                            .bindObject(stackedItem.getUniqueId().toString())
                            .bindObject(stackedItem.getStackAmount())
                            .newBatch();
                }
            } else if (stackedObject instanceof StackedSpawner) {
                spawnersInsertTransaction = plugin.getDataHandler()
                        .insertSpawner(((WStackedSpawner) stackedObject), spawnersInsertTransaction);
            } else if (stackedObject instanceof StackedBarrel) {
                barrelsInsertTransaction = plugin.getDataHandler()
                        .insertBarrel(((StackedBarrel) stackedObject), barrelsInsertTransaction);
            }
        }

        List<IDatabaseTransaction> transactionsToExecute = new LinkedList<>();
        if (spawnersInsertTransaction != null)
            transactionsToExecute.add(spawnersInsertTransaction);
        if (barrelsInsertTransaction != null)
            transactionsToExecute.add(barrelsInsertTransaction);
        if (itemsInsertTransaction != null)
            transactionsToExecute.add(itemsInsertTransaction);
        if (!transactionsToExecute.isEmpty())
            DBSession.execute(transactionsToExecute);
    }

    public StackedItem spawnStackedItemWithAmount(Location location, ItemStack itemStack, int amount) {
        Item item = spawnItemWithAmount(location, itemStack, amount);
        return item == null ? null : getStackedItem(item);
    }

    @Override
    public Item spawnItemWithAmount(Location location, ItemStack itemStack, int amount) {
        if (location == null || location.getWorld() == null || itemStack == null || amount <= 0) {
            return null;
        }

        int limit = ItemUtils.canBeStacked(itemStack, location.getWorld()) ?
                plugin.getSettings().itemsLimits.getOrDefault(itemStack.getType(), Integer.MAX_VALUE) :
                itemStack.getMaxStackSize();

        limit = limit < 1 ? Integer.MAX_VALUE : limit;

        int amountOfItems = amount / limit;
        int itemLimit = limit;

        Item lastDroppedItem = null;

        for (int i = 0; i < amountOfItems; i++) {
            ItemStack clone = itemStack.clone();
            clone.setAmount(Math.min(clone.getMaxStackSize(), itemLimit));
            lastDroppedItem = location.getWorld().dropItem(location, clone, dropped -> {
                StackedItem stackedItem = getStackedItem(dropped);
                if (plugin.getSettings().itemsStackingEnabled) {
                    stackedItem.setStackAmount(itemLimit, stackedItem.isCached());
                }
            });
        }

        int leftOvers = amount % limit;

        if (leftOvers > 0) {
            ItemStack clone = itemStack.clone();
            clone.setAmount(Math.min(clone.getMaxStackSize(), leftOvers));
            lastDroppedItem = location.getWorld().dropItem(location, clone, dropped -> {
                StackedItem stackedItem = getStackedItem(dropped);
                if (plugin.getSettings().itemsStackingEnabled) {
                    stackedItem.setStackAmount(leftOvers, stackedItem.isCached());
                }
            });
        }

        return lastDroppedItem;
    }

    @Override
    public ItemStack getFormattedBarrelItem(ItemStack barrelItem, int amount) {
        if (barrelItem == null || barrelItem.getType().isAir()) {
            return barrelItem;
        }
        ItemStack clone = barrelItem.clone();
        clone.setAmount(1);
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            String displayName = ItemUtils.getFormattedType(clone);
            String formattedName = plugin.getSettings().giveItemName
                    .replace("{0}", String.valueOf(amount))
                    .replace("{1}", displayName)
                    .replace("{2}", "Barrel");
            Component nameComp = LegacyComponentSerializer.legacySection().deserialize(formattedName);
            meta.displayName(nameComp);
            clone.setItemMeta(meta);
        }
        return ItemUtils.setSpawnerItemAmount(clone, amount);
    }

    public void performKillAll(boolean applyTaskFilter) {
        performKillAll(item -> true, applyTaskFilter);
    }

    public void performKillAll(Predicate<Item> itemPredicate, boolean applyTaskFilter) {
        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(() -> performKillAll(itemPredicate, applyTaskFilter));
            return;
        }

        List<Entity> entityList = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            if (!applyTaskFilter || plugin.getSettings().killTaskItemsWorlds.isEmpty() ||
                    plugin.getSettings().killTaskItemsWorlds.contains(world.getName())) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    entityList.addAll(Arrays.stream(chunk.getEntities())
                            .filter(entity -> entity instanceof Item).collect(Collectors.toList()));
                }
            }
        }

        Executor.async(() -> {
            if (plugin.getSettings().killTaskStackedItems) {
                entityList.stream()
                        .filter(entity -> ItemUtils.isStackable(entity) && ItemUtils.canPickup((Item) entity) && itemPredicate.test((Item) entity) &&
                                (!applyTaskFilter || (GeneralUtils.containsOrEmpty(plugin.getSettings().killTaskItemsWhitelist, ((Item) entity).getItemStack().getType()) &&
                                        !plugin.getSettings().killTaskItemsBlacklist.contains(((Item) entity).getItemStack().getType()))))
                        .forEach(entity -> {
                            StackedItem stackedItem = WStackedItem.of(entity);
                            int maxStackSize = ((Item) entity).getItemStack().getMaxStackSize();
                            if (!applyTaskFilter || stackedItem.getStackAmount() > maxStackSize ||
                                    (plugin.getSettings().killTaskUnstackedItems && stackedItem.getStackAmount() <= maxStackSize))
                                Executor.sync(stackedItem::remove);
                        });
            }

            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.isOp())
                    Locale.KILL_ALL_OPS.send(pl);
            }
        });
    }

    public boolean hasItemNamesToggledOff(Player player) {
        return itemsDisabledNames.contains(player.getUniqueId());
    }

    public void toggleItemNames(Player player) {
        if (!itemsDisabledNames.remove(player.getUniqueId()))
            itemsDisabledNames.add(player.getUniqueId());
    }

    public boolean isBarrelBlock(Material blockType, World world) {
        return (plugin.getSettings().whitelistedBarrels.size() == 0 ||
                plugin.getSettings().whitelistedBarrels.contains(blockType)) &&
                !plugin.getSettings().blacklistedBarrels.contains(blockType) &&
                !plugin.getSettings().barrelsDisabledWorlds.contains(world.getName());
    }

    public void markToBeSaved(StackedObject<?> stackedObject) {
        dataHandler.OBJECTS_TO_SAVE.add(stackedObject);
    }

    public void markToBeUnsaved(StackedObject<?> stackedObject) {
        dataHandler.OBJECTS_TO_SAVE.remove(stackedObject);
    }

    public void loadSpawners(Chunk chunk) {
        World world = chunk.getWorld();
        dataHandler.stackedSpawnerStore.loadUnloaded(world.getName(), chunk.getX(), chunk.getZ(),
                unloadedStackedSpawner -> {
                    Location spawnerLocation = new Location(world, unloadedStackedSpawner.getX(),
                            unloadedStackedSpawner.getY(), unloadedStackedSpawner.getZ());
                    Block block = spawnerLocation.getBlock();

                    if (block.getType() == Material.SPAWNER) {
                        WStackedSpawner stackedSpawner = new WStackedSpawner((CreatureSpawner) block.getState());
                        try {
                            stackedSpawner.setSaveData(false);
                            stackedSpawner.setStackAmount(unloadedStackedSpawner.getStackAmount(), true);
                            dataHandler.addStackedSpawner(stackedSpawner);
                        } finally {
                            stackedSpawner.setSaveData(true);
                        }
                    }
                });
    }

    public void loadBarrels(Chunk chunk) {
        World world = chunk.getWorld();
        dataHandler.stackedBarrelStore.loadUnloaded(world.getName(), chunk.getX(), chunk.getZ(), unloadedStackedBarrel -> {
            Location barrelLocation = new Location(world, unloadedStackedBarrel.getX(),
                    unloadedStackedBarrel.getY(), unloadedStackedBarrel.getZ());
            Block block = barrelLocation.getBlock();

            if (block.getType() == Material.CAULDRON || block.getType() == Material.WATER_CAULDRON) {
                WStackedBarrel stackedBarrel = new WStackedBarrel(block, unloadedStackedBarrel.getBarrelItem(1));
                try {
                    stackedBarrel.setSaveData(false);
                    stackedBarrel.setStackAmount(unloadedStackedBarrel.getStackAmount(), true);
                    stackedBarrel.createDisplayBlock();
                    dataHandler.addStackedBarrel(stackedBarrel);
                } finally {
                    stackedBarrel.setSaveData(true);
                }
            }
        });
    }

    @Override
    public void handleChunkLoad(Chunk chunk) {
        handleChunkLoad(chunk, CHUNK_FULL_STAGE);
    }

    public void handleChunkLoad(Chunk chunk, int loadStage) {
        this.handleChunkLoad(chunk, loadStage, Arrays.asList(chunk.getEntities()));
    }

    public void handleChunkLoad(Chunk chunk, int loadStage, List<Entity> loadedEntities) {
        if (!this.loadedData)
            return;

        boolean isChunkLoad = (loadStage & CHUNK_STAGE) != 0;

        if (isChunkLoad) {
            loadSpawners(chunk);
            loadBarrels(chunk);
        }
    }

    @Override
    public void handleChunkUnload(Chunk chunk) {
        handleChunkUnload(chunk, CHUNK_FULL_STAGE);
    }

    public void handleChunkUnload(Chunk chunk, int unloadStage) {
        this.handleChunkUnload(chunk, unloadStage, Arrays.asList(chunk.getEntities()));
    }

    public void saveItem(StackedItem stackedItem) {
        if (stackedItem != null && stackedItem.getItem() != null) {
            ItemUtils.saveItemStackAmount(stackedItem.getItem(), stackedItem.getStackAmount());
        }
    }

    public void handleChunkUnload(Chunk chunk, int unloadStage, List<Entity> unloadedEntities) {
        if (!this.loadedData)
            return;

        boolean isChunkLoad = (unloadStage & CHUNK_STAGE) != 0;
        boolean isEntitiesLoad = (unloadStage & ENTITIES_STAGE) != 0;

        if (isEntitiesLoad) {
            for (Entity entity : unloadedEntities) {
                if (entity instanceof Item) {
                    StackedItem stackedItem = dataHandler.stackedItemStore.remove(entity.getEntityId());
                    if (stackedItem != null)
                        saveItem(stackedItem);
                }
            }
        }

        if (isChunkLoad) {
            for (StackedSpawner stackedSpawner : getStackedSpawners(chunk)) {
                dataHandler.removeStackedSpawner(stackedSpawner);
                if (stackedSpawner.getStackAmount() > 1) {
                    WUnloadedStackedSpawner unloadedStackedSpawner = new WUnloadedStackedSpawner(stackedSpawner);
                    dataHandler.stackedSpawnerStore.storeUnloaded(unloadedStackedSpawner);
                }
            }

            for (StackedBarrel stackedBarrel : getStackedBarrels(chunk)) {
                dataHandler.removeStackedBarrel(stackedBarrel);
                WUnloadedStackedBarrel unloadedStackedBarrel = new WUnloadedStackedBarrel(stackedBarrel);
                dataHandler.stackedBarrelStore.storeUnloaded(unloadedStackedBarrel);
                stackedBarrel.removeDisplayBlock();
            }
        }
    }

    public void setDataLoaded() {
        this.loadedData = true;
    }

}
