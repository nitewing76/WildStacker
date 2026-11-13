package com.bgsoftware.wildstacker.api.objects;

import com.bgsoftware.wildstacker.api.enums.StackCheckResult;
import com.bgsoftware.wildstacker.api.enums.StackResult;
import com.bgsoftware.wildstacker.api.enums.UnstackResult;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

public interface StackedObject<T> {

    Location getLocation();

    int getStackAmount();

    void setStackAmount(int stackAmount, boolean updateName);

    int increaseStackAmount(int stackAmount, boolean updateName);

    World getWorld();

    Chunk getChunk();

    int getStackLimit();

    int getMergeRadius();

    boolean isBlacklisted();

    boolean isWhitelisted();

    boolean isWorldDisabled();

    boolean isCached();

    void remove();

    void updateName();

    StackCheckResult runStackCheck(StackedObject<?> stackedObject);

    T tryStack();

    StackResult runStack(StackedObject<?> stackedObject);

    void runStackAsync(StackedObject<?> stackedObject, Consumer<StackResult> stackResult);

    UnstackResult runUnstack(int amount);

    UnstackResult runUnstack(int amount, Entity entity);

    boolean isSimilar(StackedObject<?> stackedObject);

    void spawnStackParticle(boolean checkEnabled);

}
