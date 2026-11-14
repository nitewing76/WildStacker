package com.bgsoftware.wildstacker.objects;

import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.api.objects.UnloadedStackedSpawner;

public final class WUnloadedStackedSpawner extends WUnloadedStackedObject implements UnloadedStackedSpawner {

    private final int upgradeId;

    public WUnloadedStackedSpawner(StackedSpawner stackedSpawner) {
        super(stackedSpawner);
        this.upgradeId = 0;
    }

    public WUnloadedStackedSpawner(String worldName, int locX, int locY, int locZ, int stackAmount) {
        this(worldName, locX, locY, locZ, stackAmount, 0);
    }

    public WUnloadedStackedSpawner(String worldName, int locX, int locY, int locZ, int stackAmount, int upgradeId) {
        super(worldName, locX, locY, locZ, stackAmount);
        this.upgradeId = upgradeId;
    }

    public int getUpgradeId() {
        return this.upgradeId;
    }

    @Override
    public void remove() {
        plugin.getDataHandler().stackedSpawnerStore.removeUnloaded(this);
        plugin.getDataHandler().deleteSpawner(this);
    }

}
