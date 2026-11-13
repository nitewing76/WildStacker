package com.bgsoftware.wildstacker.api.events;

import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BarrelStackEvent extends StackEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public BarrelStackEvent(StackedBarrel barrel, StackedBarrel target) {
        super(barrel, target);
    }

    public StackedBarrel getBarrel() {
        return (StackedBarrel) object;
    }

    public StackedBarrel getTarget() {
        return (StackedBarrel) target;
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