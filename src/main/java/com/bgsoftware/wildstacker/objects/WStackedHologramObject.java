package com.bgsoftware.wildstacker.objects;

import com.bgsoftware.wildstacker.utils.holograms.Hologram;
import com.bgsoftware.wildstacker.utils.holograms.OnDemandHologramManager;

public abstract class WStackedHologramObject<T> extends WStackedObject<T> {

    protected Hologram hologram;
    private String cachedHologramName = "";

    protected WStackedHologramObject(T object, int stackAmount) {
        super(object, stackAmount);
    }

    public void removeHologram() {
        this.cachedHologramName = "";
        if (hologram != null) {
            hologram.removeHologram();
            hologram = null;
        }
    }

    public void setHologramName(String name, boolean createIfNull) {
        this.cachedHologramName = name == null ? "" : name;
        if (this.cachedHologramName.isEmpty()) {
            removeHologram();
            return;
        }
        if (hologram == null && createIfNull) {
            hologram = OnDemandHologramManager.createTemporaryHologram(getLocation(), this.cachedHologramName);
        } else if (hologram != null) {
            hologram.setHologramName(this.cachedHologramName);
        }
    }

    public String getCachedHologramName() {
        return this.cachedHologramName;
    }

    public Hologram getHologram() {
        return this.hologram;
    }

    public void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }

}