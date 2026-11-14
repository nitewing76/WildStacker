package com.bgsoftware.wildstacker.objects;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.objects.StackedObject;
import com.bgsoftware.wildstacker.api.objects.UnloadedStackedObject;
import com.bgsoftware.wildstacker.utils.data.structures.Location2ObjectMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public abstract class WUnloadedStackedObject implements UnloadedStackedObject, Location2ObjectMap.ILocationEntity {

    protected static final WildStackerPlugin plugin = WildStackerPlugin.getPlugin();

    private final String worldName;
    private final int locX;
    private final int locY;
    private final int locZ;
    private int stackAmount;

    protected WUnloadedStackedObject(StackedObject<?> stackedObject) {
        this(
                stackedObject.getWorld().getName(),
                stackedObject.getLocation().getBlockX(),
                stackedObject.getLocation().getBlockY(),
                stackedObject.getLocation().getBlockZ(),
                stackedObject.getStackAmount()
        );
    }

    protected WUnloadedStackedObject(String worldName, int locX, int locY, int locZ, int stackAmount) {
        this.worldName = worldName;
        this.locX = locX;
        this.locY = locY;
        this.locZ = locZ;
        this.stackAmount = stackAmount;
    }

    @Override
    public Location getLocation() {
        return new Location(getWorld(), this.locX, this.locY, this.locZ);
    }

    @Override
    public World getWorld() {
        return Bukkit.getWorld(this.worldName);
    }

    @Override
    public String getWorldName() {
        return this.worldName;
    }

    @Override
    public int getX() {
        return this.locX;
    }

    @Override
    public int getY() {
        return this.locY;
    }

    @Override
    public int getZ() {
        return this.locZ;
    }

    @Override
    public int getStackAmount() {
        return this.stackAmount;
    }

    @Override
    public void setStackAmount(int stackAmount, boolean updateName) {
        this.stackAmount = stackAmount;
    }

    @Override
    public abstract void remove();

}
