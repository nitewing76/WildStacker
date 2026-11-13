package com.bgsoftware.wildstacker.api.events;

import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BarrelUnstackEvent extends UnstackEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public BarrelUnstackEvent(StackedBarrel barrel, Entity unstackSource, int unstackAmount) {
        super(barrel, unstackSource, unstackAmount);
    }

    public StackedBarrel getBarrel() {
        return (StackedBarrel) object;
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
