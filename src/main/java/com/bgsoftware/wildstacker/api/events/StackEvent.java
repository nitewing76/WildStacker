package com.bgsoftware.wildstacker.api.events;

import com.bgsoftware.wildstacker.api.objects.StackedObject;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class StackEvent extends Event implements Cancellable {

    protected final StackedObject<?> object;
    protected final StackedObject<?> target;
    private boolean cancelled;

    protected StackEvent(StackedObject<?> object, StackedObject<?> target) {
        super(!Bukkit.isPrimaryThread());
        this.object = object;
        this.target = target;
        this.cancelled = false;
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