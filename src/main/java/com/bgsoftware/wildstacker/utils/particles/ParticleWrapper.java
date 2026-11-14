package com.bgsoftware.wildstacker.utils.particles;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Locale;

public final class ParticleWrapper {

    private final Particle particle;
    private final int count;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double extra;

    public ParticleWrapper(String type, int count, int offsetX, int offsetY, int offsetZ, double extra) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Particle type cannot be null or empty.");
        }
        this.particle = parseParticle(type);
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.extra = extra;
    }

    public void spawnParticle(Location location) {
        if (location == null || particle == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    private static Particle parseParticle(String raw) {
        String upper = raw.trim().toUpperCase(Locale.ENGLISH);
        try {
            return Particle.valueOf(upper);
        } catch (IllegalArgumentException ex) {
            return Particle.CLOUD;
        }
    }

}
