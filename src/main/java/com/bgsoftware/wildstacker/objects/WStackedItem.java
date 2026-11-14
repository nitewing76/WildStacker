package com.bgsoftware.wildstacker.objects;

import com.bgsoftware.wildstacker.api.enums.StackCheckResult;
import com.bgsoftware.wildstacker.api.enums.StackResult;
import com.bgsoftware.wildstacker.api.enums.UnstackResult;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.bgsoftware.wildstacker.api.objects.StackedObject;
import com.bgsoftware.wildstacker.utils.events.EventsCaller;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import com.bgsoftware.wildstacker.utils.particles.ParticleWrapper;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public final class WStackedItem extends WStackedObject<Item> implements StackedItem {

    private static final Pattern DISPLAY_NAME_PLACEHOLDER = Pattern.compile(Pattern.quote("{0}"));
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public static final NamespacedKey KEY_REMOVED_ITEM = new NamespacedKey("wildstacker", "removed-item");
    public static final NamespacedKey KEY_DROPPED_BY_PLAYER = new NamespacedKey("wildstacker", "dropped-by-player");

    private final UUID cachedUUID;
    private final int cachedEntityId;

    public WStackedItem(Item item) {
        this(item, item.getItemStack().getAmount());
    }

    public WStackedItem(Item item, int stackAmount) {
        super(item, stackAmount);
        this.cachedUUID = item.getUniqueId();
        this.cachedEntityId = item.getEntityId();
    }

    public static StackedItem of(Entity entity) {
        if (entity instanceof Item item)
            return of(item);
        throw new IllegalArgumentException("Only items can be applied to StackedItem object");
    }

    public static StackedItem of(Item item) {
        if (!ItemUtils.isStackable(item))
            throw new IllegalArgumentException("The item " + item + " is not a stackable item.");

        return ofBypass(item);
    }

    public static StackedItem ofBypass(Item item) {
        return plugin.getSystemManager().getStackedItem(item);
    }

    @Override
    public World getWorld() {
        return object.getWorld();
    }

    @Override
    public void onStackAmountChange(int newStackAmount) {
        super.onStackAmountChange(newStackAmount);
        if (newStackAmount > 0) {
            ItemStack itemStack = object.getItemStack().clone();
            itemStack.setAmount(Math.min(itemStack.getMaxStackSize(), newStackAmount));
            if (itemStack.getType() != Material.AIR && itemStack.getAmount() > 0)
                object.setItemStack(itemStack);
            ItemUtils.saveItemStackAmount(object, newStackAmount);
        }
    }

    @Override
    public Location getLocation() {
        return object.getLocation();
    }

    @Override
    public Chunk getChunk() {
        return object.getLocation().getChunk();
    }

    @Override
    public int getStackLimit() {
        int limit = plugin.getSettings().itemsLimits.getOrDefault(object.getItemStack().getType(), Integer.MAX_VALUE);
        return limit < 1 ? Integer.MAX_VALUE : limit;
    }

    @Override
    public int getMergeRadius() {
        int radius = plugin.getSettings().itemsMergeRadius.getOrDefault(object.getItemStack().getType(), 0);
        return radius < 1 ? 0 : radius;
    }

    @Override
    public boolean isBlacklisted() {
        return plugin.getSettings().blacklistedItems.contains(object.getItemStack().getType());
    }

    @Override
    public boolean isWhitelisted() {
        return plugin.getSettings().whitelistedItems.size() == 0 ||
                plugin.getSettings().whitelistedItems.contains(object.getItemStack().getType());
    }

    @Override
    public boolean isWorldDisabled() {
        return plugin.getSettings().itemsDisabledWorlds.contains(object.getWorld().getName());
    }

    @Override
    public boolean isCached() {
        return plugin.getSettings().itemsStackingEnabled && super.isCached();
    }

    @Override
    public void remove() {
        plugin.getSystemManager().removeStackObject(this);

        object.getPersistentDataContainer().set(KEY_REMOVED_ITEM, PersistentDataType.BYTE, (byte) 1);
        Executor.sync(object::remove);
    }

    @Override
    public void updateName() {
        if (!plugin.getSettings().itemsStackingEnabled || !ItemUtils.canPickup(object))
            return;

        String customNameTemplate = plugin.getSettings().itemsCustomName;
        if (customNameTemplate == null || customNameTemplate.isEmpty())
            return;

        ItemStack itemStack = getItemStack();
        int amount = getStackAmount();
        boolean shouldShowName = plugin.getSettings().itemsUnstackedCustomName || amount > 1;

        String builtName = "";
        if (shouldShowName) {
            String cachedDisplayName = ItemUtils.getFormattedType(itemStack);
            ItemMeta meta = itemStack.getItemMeta();
            String displayName = (meta != null && meta.hasDisplayName() && meta.displayName() != null)
                    ? SECTION_SERIALIZER.serialize(meta.displayName())
                    : cachedDisplayName;

            if (plugin.getSettings().itemsDisplayEnabled)
                cachedDisplayName = displayName;

            setCachedDisplayName(DISPLAY_NAME_PLACEHOLDER.matcher(cachedDisplayName).replaceAll(displayName));
            builtName = plugin.getSettings().itemsNameBuilder.build(this);
        }

        final String finalName = builtName;
        Executor.sync(() -> {
            if (shouldShowName) {
                setCustomName(finalName);
            }
            setCustomNameVisible(shouldShowName);
        });

        if (saveData)
            plugin.getSystemManager().markToBeSaved(this);
    }

    @Override
    public StackCheckResult runStackCheck(StackedObject<?> stackedObject) {
        if (!plugin.getSettings().itemsStackingEnabled)
            return StackCheckResult.BLACKLISTED;

        StackCheckResult superResult = super.runStackCheck(stackedObject);
        if (superResult != StackCheckResult.SUCCESS)
            return superResult;

        if (!plugin.getSettings().itemsMaxPickupDelay && !ItemUtils.canPickup(object))
            return StackCheckResult.BLACKLISTED;

        if (isRemoved() || object.isDead())
            return StackCheckResult.ALREADY_DEAD;

        StackedItem targetItem = (StackedItem) stackedObject;

        if (!plugin.getSettings().itemsMaxPickupDelay && !ItemUtils.canPickup(targetItem.getItem()))
            return StackCheckResult.BLACKLISTED;

        if (((WStackedItem) targetItem).isRemoved() || targetItem.getItem().isDead())
            return StackCheckResult.ALREADY_DEAD;

        return StackCheckResult.SUCCESS;
    }

    @Override
    public Optional<Item> runStack() {
        int range = getMergeRadius();
        if (range <= 0 || getStackLimit() <= 1)
            return Optional.empty();

        Location itemLocation = getItem().getLocation();
        World world = itemLocation.getWorld();
        if (world == null)
            return Optional.empty();

        Collection<Item> nearbyItems = world.getNearbyEntitiesByType(Item.class, itemLocation, range, range, range, ItemUtils::isStackable);

        for (Item entity : nearbyItems) {
            if (entity.equals(this.object))
                continue;

            StackedItem targetItem = WStackedItem.ofBypass(entity);
            if (runStackCheck(targetItem) == StackCheckResult.SUCCESS) {
                StackResult stackResult = runStack(targetItem);
                if (stackResult == StackResult.SUCCESS) {
                    return Optional.of(targetItem.getItem());
                }
            }
        }

        updateName();
        return Optional.empty();
    }

    @Override
    public void runStackAsync(Consumer<Optional<Item>> result) {
        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(() -> runStackAsync(result));
            return;
        }
        Optional<Item> stacked = runStack();
        if (result != null) {
            result.accept(stacked);
        }
    }

    @Override
    public StackResult runStack(StackedObject<?> stackedObject) {
        if (runStackCheck(stackedObject) != StackCheckResult.SUCCESS)
            return StackResult.NOT_SIMILAR;

        StackedItem targetItem = (StackedItem) stackedObject;

        if (!EventsCaller.callItemStackEvent(targetItem, this))
            return StackResult.EVENT_CANCELLED;

        targetItem.increaseStackAmount(getStackAmount(), false);

        Executor.sync(() -> {
            if (targetItem.getItem().isValid())
                targetItem.updateName();
        }, 2L);

        this.remove();

        spawnStackParticle(true);

        return StackResult.SUCCESS;
    }

    @Override
    public UnstackResult runUnstack(int amount, Entity entity) {
        throw new UnsupportedOperationException("Cannot unstack stacked items. Use giveItemStack() method.");
    }

    @Override
    public boolean isSimilar(StackedObject<?> stackedObject) {
        return stackedObject instanceof StackedItem target &&
                object.getItemStack().isSimilar(target.getItem().getItemStack());
    }

    @Override
    public void spawnStackParticle(boolean checkEnabled) {
        if (!checkEnabled || plugin.getSettings().itemsParticlesEnabled) {
            Location location = getItem().getLocation();
            for (ParticleWrapper particleWrapper : plugin.getSettings().itemsParticles)
                particleWrapper.spawnParticle(location);
        }
    }

    @Override
    public Item getItem() {
        return object;
    }

    @Override
    public UUID getUniqueId() {
        return cachedUUID;
    }

    @Override
    public String getCustomName() {
        Component customName = object.customName();
        return customName == null ? "" : SECTION_SERIALIZER.serialize(customName);
    }

    @Override
    public void setCustomName(String customName) {
        if (customName == null || customName.isEmpty()) {
            object.customName(null);
        } else if (customName.indexOf('\u00A7') != -1) {
            object.customName(SECTION_SERIALIZER.deserialize(customName));
        } else {
            object.customName(AMPERSAND_SERIALIZER.deserialize(customName));
        }
    }

    public boolean isCustomNameVisible() {
        return object.isCustomNameVisible();
    }

    public void setCustomNameVisible(boolean visible) {
        object.setCustomNameVisible(visible);
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack is = object.getItemStack().clone();
        is.setAmount(Math.max(1, getStackAmount()));
        return is;
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR)
            remove();
        else
            object.setItemStack(itemStack);
    }

    @Override
    public void giveItemStack(Inventory inventory) {
        giveItemStack(inventory, null);
    }

    @Override
    public void giveItemStack(PlayerInventory playerInventory, Location dropLocation) {
        giveItemStack((Inventory) playerInventory, dropLocation);
    }

    private void giveItemStack(Inventory inventory, Location dropLocation) {
        if (isRemoved())
            return;

        ItemStack itemStack = getItemStack();

        int maxStackAmount = itemStack.getMaxStackSize();
        boolean inventoryFull = false;

        if (maxStackAmount != 64 && !plugin.getSettings().itemsFixStackEnabled && !ItemUtils.isShulkerBox(itemStack))
            maxStackAmount = 64;

        itemStack.setAmount(maxStackAmount);

        while (getStackAmount() >= maxStackAmount && !inventoryFull) {
            int amountLeft = giveItem(inventory, itemStack.clone());
            decreaseStackAmount(maxStackAmount - amountLeft, true);
            if (amountLeft > 0) {
                inventoryFull = true;
            }
        }

        if (!inventoryFull && getStackAmount() > 0) {
            int currentStackAmount = getStackAmount();
            itemStack.setAmount(currentStackAmount);
            int amountLeft = giveItem(inventory, itemStack.clone());
            decreaseStackAmount(currentStackAmount - amountLeft, true);
        }

        int finalStackAmount = getStackAmount();

        if (finalStackAmount <= 0) {
            remove();
        } else if (dropLocation != null) {
            ItemStack remaining = getItemStack();
            remaining.setAmount(finalStackAmount);
            remove();
            ItemUtils.dropItem(remaining, dropLocation);
        }
    }

    public int getId() {
        return cachedEntityId;
    }

    @Override
    public int hashCode() {
        return getUniqueId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StackedItem target ? getUniqueId().equals(target.getUniqueId()) : super.equals(obj);
    }

    @Override
    public String toString() {
        return String.format("StackedItem{uuid=%s,amount=%s,item=%s}", getUniqueId(), getStackAmount(), object.getItemStack());
    }

    public boolean isRemoved() {
        return object.getPersistentDataContainer().has(KEY_REMOVED_ITEM, PersistentDataType.BYTE);
    }

    private int giveItem(Inventory inventory, ItemStack itemStack) {
        Map<Integer, ItemStack> additionalItems = inventory.addItem(itemStack);

        if (ItemUtils.isBucket(itemStack.getType()))
            ItemUtils.stackBucket(itemStack, inventory);
        if (ItemUtils.isSoup(itemStack))
            ItemUtils.stackStew(itemStack, inventory);

        if (additionalItems.isEmpty())
            return 0;

        return additionalItems.values().iterator().next().getAmount();
    }

}