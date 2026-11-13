package com.bgsoftware.wildstacker.api.objects;

import org.bukkit.Location;
import org.bukkit.World;

public interface UnloadedStackedObject {

    Location getLocation();

    World getWorld();

    int getStackAmount();

    void setStackAmount(int stackAmount, boolean updateName);

    void remove();

}
