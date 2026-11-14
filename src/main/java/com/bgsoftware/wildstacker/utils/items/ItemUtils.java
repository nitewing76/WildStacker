package com.bgsoftware.wildstacker.utils.items;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Pattern;

public final class ItemUtils {

    private static final Pattern LEGACY_PATTERN = Pattern.compile("LEGACY_");
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    private static final WildStackerPlugin plugin = WildStackerPlugin.getPlugin();
    private static final int MAX_PICKUP_DELAY = 32767;

    private static final NamespacedKey KEY_SPAWNERS_AMOUNT = new NamespacedKey("wildstacker", "spawners-amount");
    private static final NamespacedKey KEY_ENTITY_TYPE = new NamespacedKey("wildstacker", "entity-type");
    private static final NamespacedKey KEY_ITEM_STACK_AMOUNT = new NamespacedKey("wildstacker", "item-stack-amount");

    private ItemUtils() {
    }

    public static void addItems(ItemStack[] itemStacks, Inventory inventory, Location location) {
        Arrays.stream(itemStacks)
                .filter(itemStack -> itemStack != null && itemStack.getType() != Material.AIR)
                .forEach(itemStack -> addItem(itemStack, inventory, location));
    }

    public static void addItem(ItemStack itemStack, Inventory inventory, Location location) {
        HashMap<Integer, ItemStack> additionalItems = inventory.addItem(itemStack);

        if (isBucket(itemStack.getType()))
            stackBucket(itemStack, inventory);
        if (isSoup(itemStack))
            stackStew(itemStack, inventory);

        if (location != null && !additionalItems.isEmpty()) {
            for (ItemStack additional : additionalItems.values())
                dropItem(additional, location);
        }
    }

    public static void dropItem(ItemStack itemStack, Location location) {
        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(() -> dropItem(itemStack, location));
            return;
        }

        if (itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0 || location == null || location.getWorld() == null)
            return;

        int amount = itemStack.getAmount();

        try {
            if (canBeStacked(itemStack, location.getWorld())) {
                ItemStack cloned = itemStack.clone();
                cloned.setAmount(Math.min(itemStack.getMaxStackSize(), amount));
                plugin.getSystemManager().spawnItemWithAmount(location, cloned, amount);
            } else {
                for (int i = 0; i < amount / 64; i++) {
                    ItemStack cloned = itemStack.clone();
                    cloned.setAmount(64);
                    plugin.getSystemManager().spawnItemWithAmount(location, cloned, 64);
                }

                if (amount % 64 > 0) {
                    ItemStack cloned = itemStack.clone();
                    cloned.setAmount(amount % 64);
                    plugin.getSystemManager().spawnItemWithAmount(location, cloned, amount % 64);
                }
            }
        } catch (Exception ex) {
            WildStackerPlugin.log("Error while dropping " + itemStack + ":");
            ex.printStackTrace();
        }
    }

    public static ItemStack setSpawnerItemAmount(ItemStack itemStack, int amount) {
        if (itemStack == null || itemStack.getType() == Material.AIR)
            return itemStack;

        ItemStack cloned = itemStack.clone();
        ItemMeta meta = cloned.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (amount <= 1) {
                pdc.remove(KEY_SPAWNERS_AMOUNT);
            } else {
                pdc.set(KEY_SPAWNERS_AMOUNT, PersistentDataType.INTEGER, amount);
            }
            cloned.setItemMeta(meta);
        }
        return cloned;
    }

    public static int getSpawnerItemAmount(ItemStack itemStack) {
        return getSpawnerItemAmountOptional(itemStack).orElse(1);
    }

    public static OptionalInt getSpawnerItemAmountOptional(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta())
            return OptionalInt.empty();

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return OptionalInt.empty();

        Integer spawnersAmount = meta.getPersistentDataContainer().get(KEY_SPAWNERS_AMOUNT, PersistentDataType.INTEGER);
        if (spawnersAmount == null)
            return OptionalInt.empty();

        return OptionalInt.of(Math.max(1, spawnersAmount));
    }

    public static void saveItemStackAmount(Item item, int stackAmount) {
        if (item == null)
            return;
        PersistentDataContainer pdc = item.getPersistentDataContainer();
        if (stackAmount <= 1) {
            pdc.remove(KEY_ITEM_STACK_AMOUNT);
        } else {
            pdc.set(KEY_ITEM_STACK_AMOUNT, PersistentDataType.INTEGER, stackAmount);
        }
    }

    public static int loadItemStackAmount(Item item) {
        if (item == null)
            return 1;
        Integer stored = item.getPersistentDataContainer().get(KEY_ITEM_STACK_AMOUNT, PersistentDataType.INTEGER);
        return stored == null ? item.getItemStack().getAmount() : Math.max(1, stored);
    }

    public static ItemStack getSpawnerItem(EntityType entityType, int amount) {
        ItemStack itemStack = new ItemStack(Material.SPAWNER, 1);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta) {
            BlockState blockState = blockStateMeta.getBlockState();
            if (blockState instanceof CreatureSpawner spawner) {
                spawner.setSpawnedType(entityType);
                blockStateMeta.setBlockState(spawner);
            }
        }
        if (meta != null) {
            if (entityType != null) {
                meta.getPersistentDataContainer().set(KEY_ENTITY_TYPE, PersistentDataType.STRING, entityType.name());
            }
            if (amount > 1) {
                meta.getPersistentDataContainer().set(KEY_SPAWNERS_AMOUNT, PersistentDataType.INTEGER, amount);
            }

            String entityName = getFormattedEntityType(entityType);
            String rawName = plugin.getSettings().spawnerItemName;
            if (rawName != null && !rawName.isEmpty()) {
                String formattedName = rawName
                        .replace("{0}", String.valueOf(amount))
                        .replace("{1}", entityName)
                        .replace("{2}", "");
                meta.displayName(SECTION_SERIALIZER.deserialize(formattedName));
            }

            List<String> rawLore = plugin.getSettings().spawnerItemLore;
            if (rawLore != null && !rawLore.isEmpty()) {
                List<Component> loreComponents = new ArrayList<>(rawLore.size());
                for (String line : rawLore) {
                    String formattedLine = line
                            .replace("{0}", String.valueOf(amount))
                            .replace("{1}", entityName)
                            .replace("{2}", "");
                    loreComponents.add(SECTION_SERIALIZER.deserialize(formattedLine));
                }
                meta.lore(loreComponents);
            }

            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    @Nullable
    public static EntityType getSpawnerItemEntityType(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.SPAWNER)
            return null;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            String storedType = meta.getPersistentDataContainer().get(KEY_ENTITY_TYPE, PersistentDataType.STRING);
            if (storedType != null && !storedType.isEmpty()) {
                try {
                    return EntityType.valueOf(storedType);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (meta instanceof BlockStateMeta blockStateMeta && blockStateMeta.hasBlockState()) {
                BlockState state = blockStateMeta.getBlockState();
                if (state instanceof CreatureSpawner spawner && spawner.getSpawnedType() != null) {
                    return spawner.getSpawnedType();
                }
            }
        }
        return EntityType.PIG;
    }

    @Nullable
    public static EntityType getEntityType(ItemStack itemStack) {
        if (!isValidAndSpawnEgg(itemStack))
            throw new IllegalArgumentException("Only spawn-eggs can be used in ItemUtils#getEntityType");

        EntityType nmsType = getNMSEntityType(itemStack);
        if (nmsType != null)
            return nmsType;

        Material eggMaterial = Bukkit.getItemFactory().getSpawnEgg(EntityType.PIG);
        String matName = itemStack.getType().name();
        if (matName.endsWith("_SPAWN_EGG")) {
            String rawEntity = matName.substring(0, matName.length() - "_SPAWN_EGG".length());
            try {
                return EntityType.valueOf(rawEntity);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    public static void setEntityType(ItemStack itemStack, EntityType entityType) {
        if (!isValidAndSpawnEgg(itemStack))
            throw new IllegalArgumentException("Only spawn-eggs can be used in ItemUtils#setEntityType");

        Material spawnEgg = getSpawnEgg(entityType);
        if (spawnEgg != null) {
            itemStack.setType(spawnEgg);
        }
    }

    public static ItemStack getItemNMSEntityType(EntityType entityType) {
        Material eggMat = getSpawnEgg(entityType);
        if (eggMat == null)
            eggMat = Material.GHAST_SPAWN_EGG;

        ItemStack itemStack = new ItemStack(eggMat);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && entityType != null) {
            meta.getPersistentDataContainer().set(KEY_ENTITY_TYPE, PersistentDataType.STRING, entityType.name());
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    @Nullable
    public static EntityType getNMSEntityType(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta())
            return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return null;
        String entityType = meta.getPersistentDataContainer().get(KEY_ENTITY_TYPE, PersistentDataType.STRING);
        if (entityType == null || entityType.isEmpty())
            return null;
        try {
            return EntityType.valueOf(entityType);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Nullable
    public static Material getSpawnEgg(EntityType entityType) {
        if (entityType == null)
            return null;
        Material egg = Bukkit.getItemFactory().getSpawnEgg(entityType);
        if (egg != null)
            return egg;
        try {
            return Material.valueOf(entityType.name() + "_SPAWN_EGG");
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static boolean isValidAndSpawnEgg(@Nullable ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_SPAWN_EGG");
    }

    public static boolean isPickaxe(@Nullable ItemStack itemStack) {
        return itemStack != null && (Tag.ITEMS_PICKAXES.isTagged(itemStack.getType()) || itemStack.getType().name().endsWith("_PICKAXE"));
    }

    public static boolean isShulkerBox(@Nullable ItemStack itemStack) {
        return itemStack != null && Tag.SHULKER_BOXES.isTagged(itemStack.getType());
    }

    public static boolean isBucket(@Nullable Material material) {
        if (material == null || material == Material.BUCKET)
            return false;
        return material.name().endsWith("_BUCKET");
    }

    public static boolean isSoup(@Nullable ItemStack itemStack) {
        if (itemStack == null)
            return false;
        Material type = itemStack.getType();
        return type == Material.MUSHROOM_STEW || type == Material.RABBIT_STEW ||
                type == Material.BEETROOT_SOUP || type == Material.SUSPICIOUS_STEW;
    }

    public static String getFormattedType(ItemStack itemStack) {
        String typeName = LEGACY_PATTERN.matcher(itemStack.getType().name()).replaceAll("");

        String customName = plugin.getSettings().customNames.get(typeName);
        if (customName != null)
            return customName;

        return formatEnumType(typeName);
    }

    public static String getFormattedEntityType(EntityType entityType) {
        if (entityType == null)
            return "";
        String customName = plugin.getSettings().customNames.get(entityType.name());
        if (customName != null)
            return customName;
        return formatEnumType(entityType.name());
    }

    public static String formatEnumType(String rawName) {
        if (rawName == null || rawName.isEmpty())
            return "";
        StringBuilder name = new StringBuilder();
        for (String section : rawName.split("_")) {
            if (section.isEmpty())
                continue;
            if (!name.isEmpty())
                name.append(" ");
            name.append(Character.toUpperCase(section.charAt(0)))
                    .append(section.substring(1).toLowerCase(java.util.Locale.ENGLISH));
        }
        return name.toString();
    }

    public static void stackBucket(ItemStack bucket, Inventory inventory) {
        if (!plugin.getSettings().bucketsStackerEnabled || bucket == null)
            return;
        if (bucket.hasItemMeta() && bucket.getItemMeta().hasDisplayName()) {
            String legacyName = SECTION_SERIALIZER.serialize(bucket.getItemMeta().displayName());
            if (plugin.getSettings().bucketsBlacklistedNames.contains(legacyName))
                return;
        }
        stackItems(bucket, inventory, plugin.getSettings().bucketsMaxStack);
    }

    public static void stackStew(ItemStack stew, Inventory inventory) {
        if (plugin.getSettings().stewsStackingEnabled && stew != null)
            stackItems(stew, inventory, plugin.getSettings().stewsMaxStack);
    }

    private static void stackItems(ItemStack item, Inventory inventory, int maxStack) {
        int amountOfItems = 0;
        int slotToSetFirstItem = -1;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack itemStack = inventory.getItem(slot);
            if (itemStack != null && itemStack.isSimilar(item)) {
                if (slotToSetFirstItem == -1)
                    slotToSetFirstItem = slot;
                amountOfItems += itemStack.getAmount();
                inventory.setItem(slot, new ItemStack(Material.AIR));
            }
        }

        updateInventory(inventory);

        ItemStack cloned = item.clone();
        cloned.setAmount(maxStack);

        for (int i = 0; i < amountOfItems / maxStack; i++) {
            if (slotToSetFirstItem != -1) {
                inventory.setItem(slotToSetFirstItem, cloned.clone());
                slotToSetFirstItem = -1;
            } else {
                inventory.addItem(cloned.clone());
            }
        }

        if (amountOfItems % maxStack > 0) {
            cloned.setAmount(amountOfItems % maxStack);
            if (slotToSetFirstItem != -1) {
                inventory.setItem(slotToSetFirstItem, cloned);
            } else {
                inventory.addItem(cloned);
            }
        }

        updateInventory(inventory);
    }

    public static ItemStack getFromBlock(Block block) {
        return block == null ? new ItemStack(Material.AIR) : new ItemStack(block.getType());
    }

    private static void updateInventory(Inventory inventory) {
        inventory.getViewers().stream().filter(humanEntity -> humanEntity instanceof Player)
                .forEach(player -> ((Player) player).updateInventory());
    }

    public static EquipmentSlot getHand(Event event) {
        if (event instanceof PlayerInteractEvent interactEvent) {
            return interactEvent.getHand() == null ? EquipmentSlot.HAND : interactEvent.getHand();
        } else if (event instanceof PlayerInteractEntityEvent interactEntityEvent) {
            return interactEntityEvent.getHand();
        } else if (event instanceof BlockPlaceEvent blockPlaceEvent) {
            return blockPlaceEvent.getHand();
        } else if (event instanceof PlayerBucketEvent bucketEvent) {
            return bucketEvent.getHand();
        } else {
            throw new IllegalArgumentException("Cannot get used hand for event: " + event.getClass());
        }
    }

    public static void removeItemFromHand(Player player, int amount, EquipmentSlot equipmentSlot) {
        PlayerInventory playerInventory = player.getInventory();

        if (equipmentSlot == EquipmentSlot.HAND) {
            ItemStack mainHand = playerInventory.getItemInMainHand();
            if (mainHand.getType() != Material.AIR) {
                mainHand.setAmount(mainHand.getAmount() - amount);
                playerInventory.setItemInMainHand(mainHand);
            }
        } else if (equipmentSlot == EquipmentSlot.OFF_HAND) {
            ItemStack offHand = playerInventory.getItemInOffHand();
            if (offHand.getType() != Material.AIR) {
                offHand.setAmount(offHand.getAmount() - amount);
                playerInventory.setItemInOffHand(offHand);
            }
        }
    }

    @Nullable
    public static ItemStack getItemInOffhand(PlayerInventory inventory) {
        return getItemFromHand(inventory, EquipmentSlot.OFF_HAND);
    }

    public static int countItem(Inventory inventory, ItemStack itemStack) {
        int counter = 0;

        for (ItemStack _itemStack : inventory.getContents()) {
            if (_itemStack != null && _itemStack.isSimilar(itemStack))
                counter += _itemStack.getAmount();
        }

        return counter;
    }

    public static void removeItem(Inventory inventory, ItemStack itemStack, int amount) {
        int amountRemoved = 0;

        for (int i = 0; i < inventory.getSize() && amountRemoved < amount; i++) {
            ItemStack _itemStack = inventory.getItem(i);
            if (_itemStack != null && _itemStack.isSimilar(itemStack)) {
                if (amountRemoved + _itemStack.getAmount() <= amount) {
                    amountRemoved += _itemStack.getAmount();
                    inventory.setItem(i, new ItemStack(Material.AIR));
                } else {
                    _itemStack.setAmount(_itemStack.getAmount() - amount + amountRemoved);
                    amountRemoved = amount;
                }
            }
        }
    }

    @Nullable
    public static ItemStack getItemFromHand(PlayerInventory inventory, EquipmentSlot equipmentSlot) {
        if (equipmentSlot == EquipmentSlot.HAND) {
            return inventory.getItemInMainHand();
        } else if (equipmentSlot == EquipmentSlot.OFF_HAND) {
            return inventory.getItemInOffHand();
        }
        return null;
    }

    public static void setItemInHand(PlayerInventory inventory, EquipmentSlot equipmentSlot, ItemStack itemStack) {
        if (equipmentSlot == EquipmentSlot.HAND) {
            inventory.setItemInMainHand(itemStack);
        } else if (equipmentSlot == EquipmentSlot.OFF_HAND) {
            inventory.setItemInOffHand(itemStack);
        }
    }

    public static boolean canPickup(Item item) {
        return item != null && item.getPickupDelay() < MAX_PICKUP_DELAY &&
                !item.hasMetadata("ChestShop_Display") && !item.hasMetadata("no_pickup");
    }

    public static boolean isOffHand(Event event) {
        return EquipmentSlot.OFF_HAND == getHand(event);
    }

    public static boolean isPickaxeAndHasSilkTouch(@Nullable ItemStack itemStack) {
        if (!isPickaxe(itemStack) || itemStack == null)
            return false;

        int requiredLevel = plugin.getSettings().silkTouchMinimumLevel;
        return itemStack.getEnchantmentLevel(Enchantment.SILK_TOUCH) >= requiredLevel;
    }

    public static Location getSafeDropLocation(Location origin) {
        Location location;

        if ((location = origin.clone().add(0, 0.5, 0)).getBlock().getType().isAir())
            return location;
        else if ((location = origin.clone().subtract(0, 0.5, 0)).getBlock().getType().isAir())
            return location;

        return origin;
    }

    public static boolean isStackable(Entity entity) {
        return entity != null && entity.isValid() && !entity.isDead() && entity instanceof Item;
    }

    public static boolean canBeStacked(ItemStack itemStack, World world) {
        if (!plugin.getSettings().itemsStackingEnabled || itemStack == null || world == null)
            return false;

        Material itemType = itemStack.getType();
        return !plugin.getSettings().blacklistedItems.contains(itemType) &&
                (plugin.getSettings().whitelistedItems.size() == 0 || plugin.getSettings().whitelistedItems.contains(itemType)) &&
                !plugin.getSettings().itemsDisabledWorlds.contains(world.getName());
    }

}