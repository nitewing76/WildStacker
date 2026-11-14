package com.bgsoftware.wildstacker.objects;

import com.bgsoftware.wildstacker.utils.holograms.Hologram;

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
        if (hologram != null) {
            if (this.cachedHologramName.isEmpty()) {
                removeHologram();
            } else {
                hologram.setHologramName(this.cachedHologramName);
            }
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