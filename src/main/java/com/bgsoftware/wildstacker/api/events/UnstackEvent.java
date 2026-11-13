package com.bgsoftware.wildstacker.api.events;

import com.bgsoftware.wildstacker.api.objects.StackedObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class UnstackEvent extends Event implements Cancellable {

    protected final StackedObject<?> object;
    protected final Entity unstackSource;
    private int unstackAmount;
    private boolean cancelled;

    protected UnstackEvent(StackedObject<?> object, Entity unstackSource, int unstackAmount) {
        super(!Bukkit.isPrimaryThread());
        this.object = object;
        this.unstackSource = unstackSource;
        this.unstackAmount = unstackAmount;
        this.cancelled = false;
    }

    public Entity getUnstackSource() {
        return unstackSource;
    }

    public int getUnstackAmount() {
        return unstackAmount;
    }

    public void setUnstackAmount(int unstackAmount) {
        this.unstackAmount = unstackAmount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
