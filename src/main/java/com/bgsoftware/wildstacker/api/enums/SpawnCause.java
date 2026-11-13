package com.bgsoftware.wildstacker.api.enums;

import org.bukkit.event.entity.CreatureSpawnEvent;

public enum SpawnCause {

    BEEHIVE,
    BREEDING,
    BUCKET,
    BUILD_COPPERGOLEM,
    BUILD_IRONGOLEM,
    BUILD_SNOWMAN,
    BUILD_WITHER,
    CHUNK_GEN,
    COMMAND,
    CURED,
    CURING,
    CUSTOM,
    DEFAULT,
    DISPENSE_EGG,
    DISMOUNT,
    DROWNED,
    DUPLICATION,
    EGG,
    ENCHANTMENT,
    ENDER_PEARL,
    EXPLOSION,
    FROZEN,
    INFECTION,
    JOCKEY,
    LIGHTNING,
    METAMORPHOSIS,
    MOUNT,
    NATURAL,
    NETHER_PORTAL,
    OCELOT_BABY,
    OMINOUS_ITEM_SPAWNER,
    PATROL,
    PIGLIN_ZOMBIFIED,
    POTION_EFFECT,
    RAID,
    REANIMATE,
    REINFORCEMENTS,
    SHEARED,
    SHOULDER_ENTITY,
    SILVERFISH_BLOCK,
    SLIME_SPLIT,
    SPAWNER,
    SPAWNER_EGG,
    SPELL,
    TRAP,
    TRIAL_SPAWNER,
    VILLAGE_DEFENSE,
    VILLAGE_INVASION;

    public static SpawnCause valueOf(CreatureSpawnEvent.SpawnReason spawnReason) {
        if (spawnReason == null) {
            return DEFAULT;
        }
        try {
            return valueOf(spawnReason.name());
        } catch (IllegalArgumentException ex) {
            return DEFAULT;
        }
    }

    public CreatureSpawnEvent.SpawnReason toSpawnReason() {
        try {
            return CreatureSpawnEvent.SpawnReason.valueOf(this.name());
        } catch (IllegalArgumentException ex) {
            return CreatureSpawnEvent.SpawnReason.CUSTOM;
        }
    }

}
