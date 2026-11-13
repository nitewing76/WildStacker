package com.bgsoftware.wildstacker.api.events;

import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SpawnerUnstackEvent extends UnstackEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public SpawnerUnstackEvent(StackedSpawner spawner, Entity unstackSource, int unstackAmount) {
        super(spawner, unstackSource, unstackAmount);
    }

    public StackedSpawner getSpawner() {
        return (StackedSpawner) object;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
