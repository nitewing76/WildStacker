package com.bgsoftware.wildstacker.api.handlers;

import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.bgsoftware.wildstacker.api.objects.StackedObject;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.api.objects.UnloadedStackedBarrel;
import com.bgsoftware.wildstacker.api.objects.UnloadedStackedSpawner;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface SystemManager {

    void removeStackObject(StackedObject<?> stackedObject);

    StackedItem getStackedItem(Item item);

    StackedSpawner getStackedSpawner(CreatureSpawner spawner);

    StackedSpawner getStackedSpawner(Location location);

    StackedBarrel getStackedBarrel(Block block);

    StackedBarrel getStackedBarrel(Location location);

    List<StackedItem> getStackedItems();

    List<StackedSpawner> getStackedSpawners();

    List<StackedSpawner> getStackedSpawners(Chunk chunk);

    List<StackedSpawner> getStackedSpawners(World world, int chunkX, int chunkZ);

    List<UnloadedStackedSpawner> getAllSpawners();

    List<StackedBarrel> getStackedBarrels();

    List<StackedBarrel> getStackedBarrels(Chunk chunk);

    List<StackedBarrel> getStackedBarrels(World world, int chunkX, int chunkZ);

    List<UnloadedStackedBarrel> getAllBarrels();

    boolean isStackedSpawner(Block block);

    boolean isStackedSpawner(Location location);

    boolean isStackedBarrel(Block block);

    boolean isStackedBarrel(Location location);

    void performCacheClear();

    Item spawnItemWithAmount(Location location, ItemStack itemStack, int amount);

    ItemStack getFormattedBarrelItem(ItemStack barrelItem, int amount);

    void handleChunkLoad(Chunk chunk);

    void handleChunkUnload(Chunk chunk);

}
