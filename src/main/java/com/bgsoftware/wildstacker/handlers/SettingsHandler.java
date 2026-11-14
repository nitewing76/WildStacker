package com.bgsoftware.wildstacker.handlers;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.menu.BarrelAmountsMenu;
import com.bgsoftware.wildstacker.menu.SpawnerAmountsMenu;
import com.bgsoftware.wildstacker.menu.SpawnersManageMenu;
import com.bgsoftware.wildstacker.objects.WStackedBarrel;
import com.bgsoftware.wildstacker.objects.WStackedItem;
import com.bgsoftware.wildstacker.objects.WStackedSpawner;
import com.bgsoftware.wildstacker.utils.data.structures.FastEnumArray;
import com.bgsoftware.wildstacker.utils.data.structures.FastEnumMap;
import com.bgsoftware.wildstacker.utils.items.ItemBuilder;
import com.bgsoftware.wildstacker.utils.names.NameBuilder;
import com.bgsoftware.wildstacker.utils.names.NamePlaceholder;
import com.bgsoftware.wildstacker.utils.pair.Pair;
import com.bgsoftware.wildstacker.utils.particles.ParticleWrapper;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public final class SettingsHandler {

    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    public final Pattern SPAWNERS_PATTERN;
    public final String[] CONFIG_IGNORED_SECTIONS = {"merge-radius", "limits", "minimum-required", "default-unstack",
            "break-slots", "manage-menu", "break-charge", "place-charge", "spawners-override.spawn-conditions",
            "spawner-upgrades.ladders"};

    // CMI Hologram View Range (fallback 16)
    public final int cmiHologramViewRange;

    // Database settings
    public final String databaseType;
    public final String databaseMySQLAddress;
    public final int databaseMySQLPort;
    public final String databaseMySQLDBName;
    public final String databaseMySQLUsername;
    public final String databaseMySQLPassword;
    public final String databaseMySQLPrefix;
    public final boolean databaseMySQLSSL;
    public final boolean databaseMySQLPublicKeyRetrieval;
    public final long databaseMySQLWaitTimeout;
    public final long databaseMySQLMaxLifetime;

    // Global settings
    public final String giveItemName, killTaskTimeCommand;
    public final ItemStack inspectTool, simulateTool;
    public final boolean deleteInvalidWorlds, killTaskStackedItems, killTaskUnstackedItems, killTaskSyncClearLagg;
    public final Map<String, String> customNames;
    public final long killTaskInterval;
    public final FastEnumArray<Material> killTaskItemsWhitelist, killTaskItemsBlacklist;
    public final List<String> killTaskItemsWorlds;

    // Items settings
    public final boolean itemsStackingEnabled, itemsParticlesEnabled, itemsFixStackEnabled, itemsDisplayEnabled,
            itemsUnstackedCustomName, itemsNamesToggleEnabled, itemsSoundEnabled, itemsMaxPickupDelay, storeItems;
    public final List<String> itemsDisabledWorlds;
    public final FastEnumArray<Material> blacklistedItems, whitelistedItems;
    public final int itemsChunkLimit;
    public final String itemsCustomName, itemsNamesToggleCommand;
    public final NameBuilder<StackedItem> itemsNameBuilder;
    public final FastEnumMap<Material, Integer> itemsMergeRadius, itemsLimits;
    public final List<ParticleWrapper> itemsParticles;
    public final long itemsStackInterval;

    // Spawners settings
    public final boolean spawnersStackingEnabled, perSpawnerLimit, spawnersParticlesEnabled, chunkMergeSpawners,
            silkTouchSpawners, explosionsDropSpawner, explosionsDropToInventory, dropToInventory, shiftGetWholeSpawnerStack,
            getStackedItem, dropSpawnerWithoutSilk, spawnersMineRequireSilk, floatingSpawnerNames, spawnersPlacementPermission,
            spawnersShiftPlaceStack, changeUsingEggs, eggsStackMultiply, nextSpawnerPlacement, onlyOneSpawner, inventoryTweaksEnabled,
            sneakingOpenMenu, amountsMenuEnabled, upgradeMenuEnabled, manageMenuEnabled, spawnersOverrideEnabled,
            spawnerUpgradesMultiplyStackAmount, listenPaperPreSpawnEvent, spawnersUnstackedCustomName;
    public final int explosionsBreakChance, explosionsBreakPercentage, explosionsBreakMinimum, explosionsAmountPercentage,
            explosionsAmountMinimum, silkTouchBreakChance, silkTouchMinimumLevel, spawnersChunkLimit;
    public final List<String> spawnersDisabledWorlds, spawnerItemLore, silkWorlds, explosionsWorlds;
    public final FastEnumArray<EntityType> blacklistedSpawners, whitelistedSpawners;
    public final String spawnersCustomName, spawnerItemName, inventoryTweaksPermission, inventoryTweaksCommand;
    public final NameBuilder<StackedSpawner> spawnersNameBuilder;
    public final FastEnumMap<EntityType, Integer> spawnersMergeRadius, spawnersLimits;
    public final List<ParticleWrapper> spawnersParticles;
    public final FastEnumMap<EntityType, Pair<Double, Boolean>> spawnersBreakCharge, spawnersPlaceCharge;

    // Barrels settings
    public final boolean barrelsStackingEnabled, barrelsParticlesEnabled, chunkMergeBarrels, explosionsBreakBarrelStack,
            barrelsToggleCommand, barrelsPlaceInventory, forceCauldron, barrelsAutoPickup, dropStackedItem, barrelsShiftPlaceStack;
    public final int barrelsChunkLimit;
    public final String barrelsCustomName, barrelsToggleCommandSyntax, barrelsPlaceInventoryTitle, barrelsRequiredPermission;
    public final NameBuilder<StackedBarrel> barrelsNameBuilder;
    public final List<String> barrelsDisabledWorlds;
    public final FastEnumArray<Material> blacklistedBarrels, whitelistedBarrels;
    public final FastEnumMap<Material, Integer> barrelsMergeRadius, barrelsLimits;
    public final List<ParticleWrapper> barrelsParticles;

    // Buckets settings
    public final boolean bucketsStackerEnabled;
    public final List<String> bucketsBlacklistedNames;
    public final int bucketsMaxStack;

    // Stews settings
    public final boolean stewsStackingEnabled;
    public final int stewsMaxStack;
    private YamlConfiguration particlesYaml = null;

    public SettingsHandler(WildStackerPlugin plugin) {
        WildStackerPlugin.log("Loading configuration started...");
        long startTime = System.currentTimeMillis();
        File file = new File(plugin.getDataFolder(), "config.yml");

        if (!file.exists())
            plugin.saveResource("config.yml", false);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        dataConvertor(cfg);

        this.cmiHologramViewRange = loadCmiViewRange(plugin);

        databaseType = cfg.getString("database.type").toUpperCase(Locale.ENGLISH);
        databaseMySQLAddress = cfg.getString("database.address");
        databaseMySQLPort = cfg.getInt("database.port");
        databaseMySQLDBName = cfg.getString("database.db-name");
        databaseMySQLUsername = cfg.getString("database.user-name");
        databaseMySQLPassword = cfg.getString("database.password");
        databaseMySQLPrefix = cfg.getString("database.prefix");
        databaseMySQLSSL = cfg.getBoolean("database.useSSL");
        databaseMySQLPublicKeyRetrieval = cfg.getBoolean("database.allowPublicKeyRetrieval");
        databaseMySQLWaitTimeout = cfg.getLong("database.waitTimeout");
        databaseMySQLMaxLifetime = cfg.getLong("database.maxLifetime");

        giveItemName = translateAmpersand(cfg.getString("give-item-name", "&6x{0} &f&o{1} {2}"));
        SPAWNERS_PATTERN = Pattern.compile(giveItemName
                .replace("{0}", "(.*)")
                .replace("{1}", "(.*)")
                .replace("{2}", "(.*)")
        );
        inspectTool = new ItemBuilder(Material.valueOf(cfg.getString("inspect-tool.type")), cfg.getInt("inspect-tool.data", 0))
                .withName(cfg.getString("inspect-tool.name"))
                .withLore(cfg.getStringList("inspect-tool.lore")).build();
        simulateTool = new ItemBuilder(Material.valueOf(cfg.getString("simulate-tool.type", cfg.getString("inspect-tool.type"))), cfg.getInt("inspect-tool.data", 0))
                .withName(cfg.getString("simulate-tool.name"))
                .withLore(cfg.getStringList("simulate-tool.lore")).build();
        deleteInvalidWorlds = cfg.getBoolean("database.delete-invalid-worlds", false);
        killTaskInterval = cfg.getLong("kill-task.interval", 300);
        killTaskStackedItems = cfg.getBoolean("kill-task.stacked-items", true);
        killTaskUnstackedItems = cfg.getBoolean("kill-task.unstacked-items", true);
        killTaskSyncClearLagg = cfg.getBoolean("kill-task.sync-clear-lagg", false);
        killTaskTimeCommand = cfg.getString("kill-task.time-command", "stacker timeleft");
        killTaskItemsWhitelist = FastEnumArray.fromList(cfg.getStringList("kill-task.kill-items.whitelist"), Material.class);
        killTaskItemsBlacklist = FastEnumArray.fromList(cfg.getStringList("kill-task.kill-items.blacklist"), Material.class);
        killTaskItemsWorlds = cfg.getStringList("kill-task.kill-items.worlds");
        customNames = new HashMap<>();
        loadCustomNames(plugin);

        itemsStackingEnabled = cfg.getBoolean("items.enabled", true);
        itemsMergeRadius = FastEnumMap.fromSection(cfg.getConfigurationSection("items.merge-radius"), Material.class);
        itemsParticlesEnabled = cfg.getBoolean("items.particles", true);
        itemsParticles = getParticles(plugin, "items");
        itemsDisabledWorlds = cfg.getStringList("items.disabled-worlds");
        itemsLimits = FastEnumMap.fromSection(cfg.getConfigurationSection("items.limits"), Material.class);
        itemsUnstackedCustomName = cfg.getBoolean("items.unstacked-custom-name", false);
        itemsFixStackEnabled = cfg.getBoolean("items.fix-stack", false);
        blacklistedItems = FastEnumArray.fromList(cfg.getStringList("items.blacklist"), Material.class);
        whitelistedItems = FastEnumArray.fromList(cfg.getStringList("items.whitelist"), Material.class);
        itemsChunkLimit = cfg.getInt("items.chunk-limit", 0);
        itemsCustomName = translateAmpersand(cfg.getString("items.custom-name", "&6&lx{0} {1}"));
        //noinspection unchecked
        itemsNameBuilder = new NameBuilder<>(itemsCustomName,
                new NamePlaceholder<>("{0}", stackedItem -> stackedItem.getStackAmount() + ""),
                new NamePlaceholder<>("{1}", stackedItem -> ((WStackedItem) stackedItem).getCachedDisplayName()),
                new NamePlaceholder<>("{2}", stackedItem -> ((WStackedItem) stackedItem).getCachedDisplayName().toUpperCase())
        );
        itemsDisplayEnabled = cfg.getBoolean("items.item-display", false);
        itemsNamesToggleEnabled = cfg.getBoolean("items.names-toggle.enabled", false);
        itemsNamesToggleCommand = cfg.getString("items.names-toggle.command", "stacker names item");
        itemsSoundEnabled = cfg.getBoolean("items.pickup-sound", true);
        itemsMaxPickupDelay = cfg.getBoolean("items.max-pickup-delay", false);
        itemsStackInterval = cfg.getLong("items.stack-interval", 0L);
        storeItems = cfg.getBoolean("items.store-items", true);

        spawnersStackingEnabled = cfg.getBoolean("spawners.enabled", true);
        spawnersMergeRadius = FastEnumMap.fromSection(cfg.getConfigurationSection("spawners.merge-radius"), EntityType.class);
        perSpawnerLimit = cfg.getBoolean("spawners.per-spawner-limit", false);
        spawnersParticlesEnabled = cfg.getBoolean("spawners.particles", true);
        spawnersParticles = getParticles(plugin, "spawners");
        spawnersDisabledWorlds = cfg.getStringList("spawners.disabled-worlds");
        spawnersLimits = FastEnumMap.fromSection(cfg.getConfigurationSection("spawners.limits"), EntityType.class);
        chunkMergeSpawners = cfg.getBoolean("spawners.chunk-merge", false);
        blacklistedSpawners = FastEnumArray.fromList(cfg.getStringList("spawners.blacklist"), EntityType.class);
        whitelistedSpawners = FastEnumArray.fromList(cfg.getStringList("spawners.whitelist"), EntityType.class);
        spawnersChunkLimit = cfg.getInt("spawners.chunk-limit", 0);
        spawnersCustomName = translateAmpersand(cfg.getString("spawners.custom-name", "&9&lx{0} {1}"));
        //noinspection unchecked
        spawnersNameBuilder = new NameBuilder<>(spawnersCustomName,
                new NamePlaceholder<>("{0}", stackedSpawner -> stackedSpawner.getStackAmount() + ""),
                new NamePlaceholder<>("{1}", stackedSpawner -> ((WStackedSpawner) stackedSpawner).getCachedDisplayName()),
                new NamePlaceholder<>("{2}", stackedSpawner -> ((WStackedSpawner) stackedSpawner).getCachedDisplayName().toUpperCase()),
                new NamePlaceholder<>("{3}", stackedSpawner -> "")
        );
        spawnerItemName = translateAmpersand(cfg.getString("spawners.spawner-item.name", "&e{0} &fSpawner"));
        spawnerItemLore = cfg.getStringList("spawners.spawner-item.lore").stream()
                .map(SettingsHandler::translateAmpersand)
                .collect(Collectors.toList());
        silkTouchSpawners = cfg.getBoolean("spawners.silk-touch.enabled", true);
        dropToInventory = cfg.getBoolean("spawners.silk-touch.drop-to-inventory", true);
        silkWorlds = cfg.getStringList("spawners.silk-touch.worlds");
        dropSpawnerWithoutSilk = cfg.getBoolean("spawners.silk-touch.drop-without-silk", false);
        silkTouchBreakChance = cfg.getInt("spawners.silk-touch.break-chance", 100);
        silkTouchMinimumLevel = cfg.getInt("spawners.silk-touch.minimum-level", 1);
        explosionsDropSpawner = cfg.getBoolean("spawners.explosions.enabled", true);
        explosionsDropToInventory = cfg.getBoolean("spawners.explosions.drop-to-inventory", false);
        explosionsWorlds = cfg.getStringList("spawners.explosions.worlds");
        explosionsBreakChance = cfg.getInt("spawners.explosions.break-chance", 100);
        explosionsBreakPercentage = cfg.getInt("spawners.explosions-break-percentage", 100);
        explosionsBreakMinimum = cfg.getInt("spawners.explosions-break-minimum", 1);
        explosionsAmountPercentage = cfg.getInt("spawners.explosions-amount-percentage", 100);
        explosionsAmountMinimum = cfg.getInt("spawners.explosions-amount-minimum", 1);
        spawnersMineRequireSilk = cfg.getBoolean("spawners.mine-require-silk", false);
        shiftGetWholeSpawnerStack = cfg.getBoolean("spawners.shift-get-whole-stack", true);
        getStackedItem = cfg.getBoolean("spawners.drop-stacked-item", true);
        floatingSpawnerNames = cfg.getBoolean("spawners.floating-names", false);
        spawnersPlacementPermission = cfg.getBoolean("spawners.placement-permission", false);
        spawnersShiftPlaceStack = cfg.getBoolean("spawners.shift-place-stack", true);

        spawnersBreakCharge = new FastEnumMap<>(EntityType.class);
        ConfigurationSection breakChargeSection = cfg.getConfigurationSection("spawners.break-charge");
        if (breakChargeSection != null) {
            for (String key : breakChargeSection.getKeys(false)) {
                ConfigurationSection mobSection = cfg.getConfigurationSection("spawners.break-charge." + key);
                if (mobSection == null)
                    continue;

                double amount = mobSection.getDouble("price", 0.0);
                if (amount <= 0)
                    continue;

                if (key.equalsIgnoreCase("ALL")) {
                    spawnersBreakCharge.putGlobal(new Pair<>(amount, mobSection.getBoolean("multiply-stack-amount", false)));
                } else {
                    try {
                        EntityType entityType = EntityType.valueOf(key.toUpperCase(Locale.ENGLISH));
                        spawnersBreakCharge.put(entityType, new Pair<>(amount, mobSection.getBoolean("multiply-stack-amount", false)));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        spawnersPlaceCharge = new FastEnumMap<>(EntityType.class);
        ConfigurationSection placeChargeSection = cfg.getConfigurationSection("spawners.place-charge");
        if (placeChargeSection != null) {
            for (String key : placeChargeSection.getKeys(false)) {
                ConfigurationSection mobSection = cfg.getConfigurationSection("spawners.place-charge." + key);
                if (mobSection == null)
                    continue;

                double amount = mobSection.getDouble("price", 0.0);
                if (amount <= 0)
                    continue;

                if (key.equalsIgnoreCase("ALL")) {
                    spawnersPlaceCharge.putGlobal(new Pair<>(amount, mobSection.getBoolean("multiply-stack-amount", false)));
                } else {
                    try {
                        EntityType entityType = EntityType.valueOf(key.toUpperCase(Locale.ENGLISH));
                        spawnersPlaceCharge.put(entityType, new Pair<>(amount, mobSection.getBoolean("multiply-stack-amount", false)));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        changeUsingEggs = cfg.getBoolean("spawners.change-using-eggs", true);
        eggsStackMultiply = cfg.getBoolean("spawners.eggs-stack-multiply", true);
        nextSpawnerPlacement = cfg.getBoolean("spawners.next-spawner-placement", true);
        onlyOneSpawner = cfg.getBoolean("spawners.only-one-spawner", true);
        inventoryTweaksEnabled = cfg.getBoolean("spawners.inventory-tweaks.enabled", true);
        inventoryTweaksPermission = cfg.getString("spawners.inventory-tweaks.permission", "");
        inventoryTweaksCommand = cfg.getString("spawners.inventory-tweaks.toggle-command", "stacker inventorytweaks,stacker it");
        spawnersOverrideEnabled = false;
        sneakingOpenMenu = cfg.getBoolean("spawners.manage-menu.sneaking-open-menu");
        amountsMenuEnabled = cfg.getBoolean("spawners.manage-menu.amounts-menu");
        upgradeMenuEnabled = false;
        manageMenuEnabled = amountsMenuEnabled;
        spawnerUpgradesMultiplyStackAmount = false;
        listenPaperPreSpawnEvent = cfg.getBoolean("spawners.listen-paper-pre-spawn-event");
        spawnersUnstackedCustomName = cfg.getBoolean("spawners.unstacked-custom-name");

        barrelsStackingEnabled = cfg.getBoolean("barrels.enabled", true);
        barrelsMergeRadius = FastEnumMap.fromSection(cfg.getConfigurationSection("barrels.merge-radius"), Material.class);
        barrelsParticlesEnabled = cfg.getBoolean("barrels.particles", true);
        barrelsParticles = getParticles(plugin, "barrels");
        barrelsDisabledWorlds = cfg.getStringList("barrels.disabled-worlds");
        barrelsLimits = FastEnumMap.fromSection(cfg.getConfigurationSection("barrels.limits"), Material.class);
        chunkMergeBarrels = cfg.getBoolean("barrels.chunk-merge", false);
        barrelsCustomName = translateAmpersand(cfg.getString("barrels.custom-name", "&9&lx{0} {1}"));
        //noinspection unchecked
        barrelsNameBuilder = new NameBuilder<>(barrelsCustomName,
                new NamePlaceholder<>("{0}", stackedBarrel -> stackedBarrel.getStackAmount() + ""),
                new NamePlaceholder<>("{1}", stackedBarrel -> ((WStackedBarrel) stackedBarrel).getCachedDisplayName()),
                new NamePlaceholder<>("{2}", stackedBarrel -> ((WStackedBarrel) stackedBarrel).getCachedDisplayName().toUpperCase())
        );
        blacklistedBarrels = FastEnumArray.fromList(cfg.getStringList("barrels.blacklist"), Material.class);
        whitelistedBarrels = FastEnumArray.fromList(cfg.getStringList("barrels.whitelist"), Material.class);
        barrelsChunkLimit = cfg.getInt("barrels.chunk-limit", 0);
        explosionsBreakBarrelStack = cfg.getBoolean("barrels.explosions-break-stack", true);
        barrelsToggleCommand = cfg.getBoolean("barrels.toggle-command.enabled", false);
        barrelsToggleCommandSyntax = cfg.getString("barrels.toggle-command.command", "stacker toggle");
        barrelsPlaceInventory = cfg.getBoolean("barrels.place-inventory.enabled", true);
        barrelsPlaceInventoryTitle = translateAmpersand(cfg.getString("barrels.place-inventory.title", "Add items here ({0})"));
        forceCauldron = cfg.getBoolean("barrels.force-cauldron", false);
        barrelsRequiredPermission = cfg.getString("barrels.required-permission", "");
        barrelsAutoPickup = cfg.getBoolean("barrels.auto-pickup", false);
        dropStackedItem = cfg.getBoolean("barrels.drop-stacked-item", false);
        barrelsShiftPlaceStack = cfg.getBoolean("barrels.shift-place-stack", false);

        bucketsStackerEnabled = cfg.getBoolean("buckets.enabled", true);
        bucketsBlacklistedNames = cfg.getStringList("buckets.name-blacklist").stream()
                .map(SettingsHandler::translateAmpersand)
                .collect(Collectors.toList());
        bucketsMaxStack = cfg.getInt("buckets.max-stack", 16);

        stewsStackingEnabled = cfg.getBoolean("stews.enabled", true);
        stewsMaxStack = cfg.getInt("stews.max-stack", 16);

        WildStackerPlugin.log(" - Stacking drops is " + getBoolean(itemsStackingEnabled));
        WildStackerPlugin.log(" - Stacking spawners is " + getBoolean(spawnersStackingEnabled));
        WildStackerPlugin.log(" - Stacking barrels is " + getBoolean(barrelsStackingEnabled));

        SpawnersManageMenu.loadMenu();
        SpawnerAmountsMenu.loadMenu();
        BarrelAmountsMenu.loadMenu();

        WildStackerPlugin.log("Loading configuration done (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public static void reload() {
        WildStackerPlugin plugin = WildStackerPlugin.getPlugin();
        plugin.setSettings(new SettingsHandler(plugin));
    }

    public static String translateAmpersand(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return SECTION_SERIALIZER.serialize(AMPERSAND_SERIALIZER.deserialize(text));
    }

    private int loadCmiViewRange(WildStackerPlugin plugin) {
        File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir != null) {
            File cmiConfigFile = new File(pluginsDir, "CMI" + File.separator + "config.yml");
            if (cmiConfigFile.exists()) {
                try {
                    YamlConfiguration cmiCfg = YamlConfiguration.loadConfiguration(cmiConfigFile);
                    int range = cmiCfg.getInt("Holograms.Defaults.viewRange", 16);
                    if (range > 0) {
                        return range;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return 16;
    }

    private String getBoolean(boolean bool) {
        return bool ? "enabled" : "disabled";
    }

    private void loadCustomNames(WildStackerPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "custom-names.yml");

        if (!file.exists())
            plugin.saveResource("custom-names.yml", false);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        if (cfg.getBoolean("enabled", true)) {
            ConfigurationSection root = cfg.getConfigurationSection("");
            if (root != null) {
                for (String key : root.getKeys(false)) {
                    if (!key.equals("enabled")) {
                        customNames.put(key, translateAmpersand(cfg.getString(key, "")));
                    }
                }
            }
        }
    }

    private List<ParticleWrapper> getParticles(WildStackerPlugin plugin, String sectionPath) {
        if (particlesYaml == null) {
            File file = new File(plugin.getDataFolder(), "particles.yml");

            if (!file.exists())
                plugin.saveResource("particles.yml", false);

            particlesYaml = YamlConfiguration.loadConfiguration(file);
        }

        List<ParticleWrapper> particleWrappers = new ArrayList<>();
        ConfigurationSection section = particlesYaml.getConfigurationSection(sectionPath);

        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection particleSection = section.getConfigurationSection(key);
                if (particleSection == null)
                    continue;
                try {
                    particleWrappers.add(new ParticleWrapper(
                            particleSection.getString("type"),
                            particleSection.getInt("count", 0),
                            particleSection.getInt("offsetX", 0),
                            particleSection.getInt("offsetY", 0),
                            particleSection.getInt("offsetZ", 0),
                            particleSection.getDouble("extra", 0.0)
                    ));
                } catch (IllegalArgumentException ex) {
                    WildStackerPlugin.log("Particle " + sectionPath + "." + key + " is missing 'type'.");
                }
            }
        }

        return particleWrappers;
    }

    private void dataConvertor(YamlConfiguration cfg) {
        if (cfg.contains("items.kill-all"))
            cfg.set("kill-task.stacked-items", cfg.getBoolean("items.kill-all"));
        if (cfg.contains("items.check-range"))
            cfg.set("items.merge-radius", cfg.getLong("items.check-range"));
        if (cfg.contains("items.save-interval"))
            cfg.set("save-interval", cfg.getLong("items.save-interval"));
        if (cfg.contains("items.blocked-materials"))
            cfg.set("items.blacklist", cfg.getStringList("items.blocked-materials"));
        if (cfg.contains("spawners.holograms.custom-name"))
            cfg.set("spawners.custom-name", cfg.getString("spawners.holograms.custom-name"));
        if (cfg.contains("spawners.holograms.enabled") && !cfg.getBoolean("spawners.holograms.enabled"))
            cfg.set("spawners.custom-name", "");
        if (cfg.contains("items.custom-display"))
            cfg.set("items.custom-display", null);
        if (cfg.getConfigurationSection("spawners.break-menu") == null)
            cfg.createSection("spawners.break-menu");
        if (cfg.isBoolean("spawners.place-inventory"))
            cfg.set("spawners.place-inventory.enabled", cfg.getBoolean("spawners.place-inventory"));
        if (cfg.isBoolean("buckets-stacker"))
            cfg.set("buckets-stacker.enabled", cfg.getBoolean("buckets-stacker"));
        if (cfg.isBoolean("barrels.place-inventory"))
            cfg.set("barrels.place-inventory.enabled", cfg.getBoolean("barrels.place-inventory"));
        if (cfg.contains("items.buckets-stacker.enabled"))
            cfg.set("buckets.enabled", cfg.getBoolean("items.buckets-stacker.enabled"));
        if (cfg.contains("items.buckets-stacker.name-blacklist"))
            cfg.set("buckets.name-blacklist", cfg.getStringList("items.buckets-stacker.name-blacklist"));
        if (cfg.contains("items.buckets-stacker.max-stack"))
            cfg.set("buckets.max-stack", cfg.getInt("items.buckets-stacker.max-stack"));
        if (cfg.contains("spawners.break-charge.amount")) {
            List<String> mobs = cfg.getStringList("spawners.break-charge.whitelist");
            if (mobs.isEmpty()) {
                mobs.add("EXAMPLE_MOB");
            }

            for (String mob : mobs) {
                cfg.set("spawners.break-charge." + mob + ".price", cfg.getInt("spawners.break-charge.amount"));
                cfg.set("spawners.break-charge." + mob + ".multiply-stack-amount", cfg.getBoolean("spawners.break-charge.multiply-stack-amount"));
            }

            cfg.set("spawners.break-charge.amount", null);
            cfg.set("spawners.break-charge.multiply-stack-amount", null);
            cfg.set("spawners.break-charge.whitelist", null);
        }
        if (cfg.contains("spawners.place-charge.amount")) {
            List<String> mobs = cfg.getStringList("spawners.place-charge.whitelist");
            if (mobs.isEmpty()) {
                mobs.add("EXAMPLE_MOB");
            }

            for (String mob : mobs) {
                cfg.set("spawners.place-charge." + mob + ".price", cfg.getInt("spawners.place-charge.amount"));
                cfg.set("spawners.place-charge." + mob + ".multiply-stack-amount", cfg.getBoolean("spawners.place-charge.multiply-stack-amount"));
            }

            cfg.set("spawners.place-charge.amount", null);
            cfg.set("spawners.place-charge.multiply-stack-amount", null);
            cfg.set("spawners.place-charge.whitelist", null);
        }
        if (cfg.contains("spawners.silk-spawners")) {
            cfg.set("spawners.silk-touch", cfg.getConfigurationSection("spawners.silk-spawners"));
            cfg.set("spawners.silk-spawners", null);
        }
        if (cfg.contains("spawners.silk-spawners.custom-name"))
            cfg.set("spawners.spawner-item.name", cfg.getString("spawners.silk-spawners.custom-name"));
        if (cfg.contains("spawners.silk-spawners.custom-lore"))
            cfg.set("spawners.spawner-item.lore", cfg.getStringList("spawners.silk-spawners.custom-lore"));
        if (cfg.contains("spawners.get-stacked-item"))
            cfg.set("drop-stacked-item", cfg.getBoolean("spawners.get-stacked-item"));
        if (cfg.contains("spawners.drop-without-silk"))
            cfg.set("spawners.silk-touch.drop-without-silk", cfg.getBoolean("spawners.drop-without-silk"));
        if (cfg.contains("spawners.silk-touch-break-chance"))
            cfg.set("spawners.silk-touch.break-chance", cfg.getInt("spawners.silk-touch-break-chance"));
        if (cfg.contains("spawners.silk-spawners.explosions-drop-spawner"))
            cfg.set("spawners.explosions.enabled", cfg.getBoolean("spawners.silk-spawners.explosions-drop-spawner"));
        if (cfg.contains("spawners.explosions-drop-to-inventory"))
            cfg.set("spawners.explosions.drop-to-inventory", cfg.getBoolean("spawners.explosions-drop-to-inventory"));
        if (cfg.contains("spawners.explosions-break-chance"))
            cfg.set("spawners.explosions.break-chance", cfg.getBoolean("spawners.explosions-break-chance"));
        if (cfg.contains("kill-task.items"))
            cfg.set("kill-task.kill-items", cfg.getConfigurationSection("kill-task.items"));
        if (cfg.isInt("items.merge-radius"))
            cfg.set("items.merge-radius.all", cfg.getInt("items.merge-radius"));
        if (cfg.isInt("spawners.merge-radius"))
            cfg.set("spawners.merge-radius.all", cfg.getInt("spawners.merge-radius"));
        if (cfg.isInt("barrels.merge-radius"))
            cfg.set("barrels.merge-radius.all", cfg.getInt("barrels.merge-radius"));
        if (cfg.isBoolean("spawners.explosions-break-stack"))
            cfg.set("spawners.explosions-break-percentage", cfg.getBoolean("spawners.explosions-break-stack") ? 100 : -1);
        if (cfg.contains("items.pickup-sound.enabled")) {
            cfg.set("items.pickup-sound", cfg.getBoolean("items.pickup-sound.enabled"));
        }
    }

}
