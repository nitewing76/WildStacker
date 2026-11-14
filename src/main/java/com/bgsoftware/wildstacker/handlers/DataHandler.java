package com.bgsoftware.wildstacker.handlers;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.handlers.DataManager;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedObject;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.data.StackedBarrelStore;
import com.bgsoftware.wildstacker.data.StackedItemStore;
import com.bgsoftware.wildstacker.data.StackedSpawnerStore;
import com.bgsoftware.wildstacker.database.DBSession;
import com.bgsoftware.wildstacker.database.sql.Column;
import com.bgsoftware.wildstacker.database.sql.DeleteSQLDatabaseTransaction;
import com.bgsoftware.wildstacker.database.sql.IDatabaseTransaction;
import com.bgsoftware.wildstacker.database.sql.InsertSQLDatabaseTransaction;
import com.bgsoftware.wildstacker.database.sql.QueryResult;
import com.bgsoftware.wildstacker.database.sql.SQLDatabaseTransaction;
import com.bgsoftware.wildstacker.objects.WStackedBarrel;
import com.bgsoftware.wildstacker.objects.WStackedSpawner;
import com.bgsoftware.wildstacker.objects.WUnloadedStackedBarrel;
import com.bgsoftware.wildstacker.objects.WUnloadedStackedSpawner;
import com.bgsoftware.wildstacker.utils.data.structures.Location2ObjectMap;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"WeakerAccess", "all"})
public final class DataHandler implements DataManager {

    public final StackedItemStore stackedItemStore = new StackedItemStore();
    public final StackedSpawnerStore stackedSpawnerStore = new StackedSpawnerStore();
    public final StackedBarrelStore stackedBarrelStore = new StackedBarrelStore();

    public final Set<StackedObject> OBJECTS_TO_SAVE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final WildStackerPlugin plugin;

    public DataHandler(WildStackerPlugin plugin) {
        this.plugin = plugin;

        Executor.sync(() -> {
            try {
                if (!DBSession.createConnection(plugin)) {
                    WildStackerPlugin.log("&cCouldn't connect to database, closing server...");
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().disablePlugin(plugin));
                    return;
                }

                loadDatabase();
            } catch (Exception ex) {
                ex.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().disablePlugin(plugin));
            }
        }, 1L);
    }

    public void clearDatabase() {
        DBSession.close();
    }

    @Override
    public void addStackedSpawner(StackedSpawner stackedSpawner) {
        this.stackedSpawnerStore.store(stackedSpawner.getLocation(), stackedSpawner);
    }

    @Override
    public void removeStackedSpawner(StackedSpawner stackedSpawner) {
        this.stackedSpawnerStore.remove(stackedSpawner.getLocation());
        Executor.sync(() -> ((WStackedSpawner) stackedSpawner).removeHologram());
    }

    @Override
    public void addStackedBarrel(StackedBarrel stackedBarrel) {
        this.stackedBarrelStore.store(stackedBarrel.getLocation(), stackedBarrel);
    }

    @Override
    public void removeStackedBarrel(StackedBarrel stackedBarrel) {
        this.stackedBarrelStore.remove(stackedBarrel.getLocation());
        stackedBarrel.removeDisplayBlock();
        Executor.sync(() -> ((WStackedBarrel) stackedBarrel).removeHologram());
    }

    public List<StackedObject> getStackedObjects() {
        List<StackedObject> stackedObjects = new LinkedList<>();
        this.stackedItemStore.collect(stackedObjects);
        this.stackedSpawnerStore.collect(stackedObjects);
        this.stackedBarrelStore.collect(stackedObjects);
        return stackedObjects.isEmpty() ? Collections.emptyList() : stackedObjects;
    }

    public SQLDatabaseTransaction<?> insertSpawner(WStackedSpawner stackedSpawner,
                                                   @Nullable SQLDatabaseTransaction<?> transaction) {
        if (transaction == null) {
            transaction = new InsertSQLDatabaseTransaction(
                    "spawners", Arrays.asList("location", "stackAmount", "upgrade"));
        }

        transaction
                .bindObject(serializeLocationInternal(stackedSpawner.getLocation()))
                .bindObject(stackedSpawner.getStackAmount())
                .bindObject(0)
                .newBatch();

        return transaction;
    }

    public void insertSpawner(WStackedSpawner stackedSpawner) {
        DBSession.execute(insertSpawner(stackedSpawner, null));
    }

    public void deleteSpawner(@Nullable Location location) {
        DBSession.execute(new DeleteSQLDatabaseTransaction("spawners", Arrays.asList("location"))
                .bindObject(serializeLocationInternal(location)));
    }

    public void deleteSpawner(Location2ObjectMap.ILocationEntity locationEntity) {
        DBSession.execute(new DeleteSQLDatabaseTransaction("spawners", Arrays.asList("location"))
                .bindObject(serializeLocationInternal(locationEntity.getWorldName(),
                        locationEntity.getX(), locationEntity.getY(), locationEntity.getZ())));
    }

    public SQLDatabaseTransaction<?> insertBarrel(StackedBarrel stackedBarrel,
                                                  @Nullable SQLDatabaseTransaction<?> transaction) {
        if (transaction == null) {
            transaction = new InsertSQLDatabaseTransaction(
                    "barrels", Arrays.asList("location", "stackAmount", "item"));
        }

        transaction
                .bindObject(serializeLocationInternal(stackedBarrel.getLocation()))
                .bindObject(stackedBarrel.getStackAmount())
                .bindObject(serializeItemInternal(stackedBarrel.getBarrelItem(1)))
                .newBatch();

        return transaction;
    }

    public void insertBarrel(StackedBarrel stackedBarrel) {
        DBSession.execute(insertBarrel(stackedBarrel, null));
    }

    public void deleteBarrel(@Nullable Location location) {
        DBSession.execute(new DeleteSQLDatabaseTransaction("barrels", Arrays.asList("location"))
                .bindObject(serializeLocationInternal(location)));
    }

    public void deleteBarrel(Location2ObjectMap.ILocationEntity locationEntity) {
        DBSession.execute(new DeleteSQLDatabaseTransaction("barrels", Arrays.asList("location"))
                .bindObject(serializeLocationInternal(locationEntity.getWorldName(),
                        locationEntity.getX(), locationEntity.getY(), locationEntity.getZ())));
    }

    private void loadDatabase() {
        prepareDatabase();

        loadItems();

        List<IDatabaseTransaction> transactionsToExecute = new LinkedList<>();

        try {
            loadSpawners(transactionsToExecute);
            loadBarrles(transactionsToExecute);
        } finally {
            if (!transactionsToExecute.isEmpty())
                DBSession.execute(transactionsToExecute);
        }

        plugin.getSystemManager().setDataLoaded();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks())
                plugin.getSystemManager().handleChunkLoad(chunk, SystemHandler.CHUNK_FULL_STAGE);
        }
    }

    private void prepareDatabase() {
        DBSession.createTable("spawners",
                new Column("location", "LONG_UNIQUE_TEXT PRIMARY KEY"),
                new Column("stackAmount", "INTEGER"),
                new Column("upgrade", "INTEGER"));

        addColumnIfNotExists("upgrade", "spawners", "0", "INTEGER");

        DBSession.createTable("barrels",
                new Column("location", "LONG_UNIQUE_TEXT PRIMARY KEY"),
                new Column("stackAmount", "INTEGER"),
                new Column("item", "TEXT"));

        DBSession.createTable("items",
                new Column("uuid", "LONG_UNIQUE_TEXT PRIMARY KEY"),
                new Column("stackAmount", "INTEGER"));
    }

    private void loadItems() {
        if (!plugin.getSettings().storeItems)
            return;

        long startTime = System.currentTimeMillis();

        WildStackerPlugin.log("Starting to load items...");

        DBSession.select("items", "", new QueryResult<ResultSet>().onSuccess(resultSet -> {
            while (resultSet.next()) {
                int stackAmount = resultSet.getInt("stackAmount");
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                this.stackedItemStore.storeUnloaded(uuid, new StackedItemStore.Unloaded(stackAmount));
            }
        }));

        WildStackerPlugin.log("Loading items done! Took " + (System.currentTimeMillis() - startTime) + " ms.");
    }

    private void loadSpawners(List<IDatabaseTransaction> transactionsToExecute) {
        long startTime = System.currentTimeMillis();

        WildStackerPlugin.log("Starting to load spawners...");

        DBSession.select("spawners", "", new QueryResult<ResultSet>().onSuccess(resultSet -> {
            DeleteSQLDatabaseTransaction deleteNullWorldTransaction = new DeleteSQLDatabaseTransaction(
                    "spawners", Arrays.asList("location"));

            boolean calledDeleteTransaction = false;

            while (resultSet.next()) {
                String location = resultSet.getString("location");
                String[] locationSections = location.split(",");

                String worldName = locationSections[0];
                int locX = Integer.parseInt(locationSections[1]);
                int locY = Integer.parseInt(locationSections[2]);
                int locZ = Integer.parseInt(locationSections[3]);

                String exceptionReason = null;

                try {
                    int stackAmount = resultSet.getInt("stackAmount");
                    int upgradeId = resultSet.getInt("upgrade");
                    WUnloadedStackedSpawner unloadedStackedSpawner =
                            new WUnloadedStackedSpawner(worldName, locX, locY, locZ, stackAmount, upgradeId);
                    this.stackedSpawnerStore.storeUnloaded(unloadedStackedSpawner);
                    continue;
                } catch (Exception ex) {
                    exceptionReason = ex.getMessage() != null ? ex.getMessage() : "Exception was thrown.";
                }

                WildStackerPlugin.log("Couldn't load spawner: " + location);
                WildStackerPlugin.log(exceptionReason);

                if (exceptionReason.contains("Null") && plugin.getSettings().deleteInvalidWorlds) {
                    deleteNullWorldTransaction.bindObject(location).newBatch();
                    WildStackerPlugin.log("Deleted spawner (" + location + ") from database.");
                    calledDeleteTransaction = true;
                }
            }

            if (calledDeleteTransaction)
                transactionsToExecute.add(deleteNullWorldTransaction);

        }));

        WildStackerPlugin.log("Loading spawners done! Took " + (System.currentTimeMillis() - startTime) + " ms.");
    }

    private void loadBarrles(List<IDatabaseTransaction> transactionsToExecute) {
        long startTime = System.currentTimeMillis();

        WildStackerPlugin.log("Starting to load barrels...");

        DBSession.select("barrels", "", new QueryResult<ResultSet>().onSuccess(resultSet -> {
            DeleteSQLDatabaseTransaction deleteNullWorldTransaction = new DeleteSQLDatabaseTransaction(
                    "barrels", Arrays.asList("location"));

            boolean calledDeleteTransaction = false;

            while (resultSet.next()) {
                String location = resultSet.getString("location");
                String[] locationSections = location.split(",");

                String worldName = locationSections[0];
                int locX = Integer.parseInt(locationSections[1]);
                int locY = Integer.parseInt(locationSections[2]);
                int locZ = Integer.parseInt(locationSections[3]);

                String exceptionReason = null;

                try {
                    int stackAmount = resultSet.getInt("stackAmount");
                    String rawItem = resultSet.getString("item");
                    ItemStack barrelItem = (rawItem == null || rawItem.isEmpty()) ? null : deserializeItemInternal(rawItem);
                    WUnloadedStackedBarrel unloadedStackedBarrel =
                            new WUnloadedStackedBarrel(worldName, locX, locY, locZ, stackAmount, barrelItem);
                    this.stackedBarrelStore.storeUnloaded(unloadedStackedBarrel);
                    continue;
                } catch (Exception ex) {
                    exceptionReason = ex.getMessage() != null ? ex.getMessage() : "Exception was thrown.";
                }

                WildStackerPlugin.log("Couldn't load barrel: " + location);
                WildStackerPlugin.log(exceptionReason);

                if (exceptionReason.contains("Null") && plugin.getSettings().deleteInvalidWorlds) {
                    deleteNullWorldTransaction.bindObject(location).newBatch();
                    WildStackerPlugin.log("Deleted barrel (" + location + ") from database.");
                    calledDeleteTransaction = true;
                }
            }

            if (calledDeleteTransaction)
                transactionsToExecute.add(deleteNullWorldTransaction);
        }));

        WildStackerPlugin.log("Loading barrels done! Took " + (System.currentTimeMillis() - startTime) + " ms.");
    }

    public static String serializeItemInternal(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(itemStack.serializeAsBytes());
    }

    @Nullable
    public static ItemStack deserializeItemInternal(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception ex) {
            return null;
        }
    }

    private static void addColumnIfNotExists(String column, String table, String def, String type) {
        String defaultSection = " DEFAULT " + def;
        DBSession.addColumn(table, column, type + defaultSection);
    }

    private static String serializeLocationInternal(@Nullable Location location) {
        return location == null || location.getWorld() == null ? "" : serializeLocationInternal(
                location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static String serializeLocationInternal(String worldName, int x, int y, int z) {
        return worldName + "," + x + "," + y + "," + z;
    }

}
