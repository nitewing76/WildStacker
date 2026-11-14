package com.bgsoftware.wildstacker.objects;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.enums.StackCheckResult;
import com.bgsoftware.wildstacker.api.enums.StackResult;
import com.bgsoftware.wildstacker.api.enums.UnstackResult;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedObject;
import com.bgsoftware.wildstacker.utils.GeneralUtils;
import com.bgsoftware.wildstacker.utils.events.EventsCaller;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import com.bgsoftware.wildstacker.utils.particles.ParticleWrapper;
import com.bgsoftware.wildstacker.utils.threads.Executor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class WStackedBarrel extends WStackedHologramObject<Block> implements StackedBarrel {

    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final List<Inventory> linkedInventories = new ArrayList<>();

    private final ItemStack barrelItem;
    private Display blockDisplay;

    public WStackedBarrel(Block block, ItemStack itemStack) {
        this(block, itemStack, 1);
    }

    public WStackedBarrel(Block block, ItemStack itemStack, int stackAmount) {
        super(block, stackAmount);
        this.barrelItem = itemStack == null ? new ItemStack(Material.AIR) : itemStack.clone();
        this.barrelItem.setAmount(1);
        setCachedDisplayName(ItemUtils.getFormattedType(this.barrelItem));
    }

    public static StackedBarrel of(Block block) {
        return plugin.getSystemManager().getStackedBarrel(block);
    }

    public static StackedBarrel of(Location location) {
        return plugin.getSystemManager().getStackedBarrel(location);
    }

    @Override
    public Block getBlock() {
        return object;
    }

    @Override
    public Material getType() {
        return barrelItem.getType();
    }

    @Override
    public short getData() {
        if (barrelItem.hasItemMeta() && barrelItem.getItemMeta() instanceof Damageable damageable) {
            return (short) damageable.getDamage();
        }
        return 0;
    }

    @Override
    public void createDisplayBlock() {
        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(this::createDisplayBlock);
            return;
        }

        Location location = getLocation();
        World world = location.getWorld();

        removeDisplayBlock();

        if (world == null || (location.getBlock().getType() != Material.CAULDRON && location.getBlock().getType() != Material.WATER_CAULDRON))
            return;

        if (barrelItem.getType().isAir())
            return;

        int viewRangeBlocks = plugin.getSettings().cmiHologramViewRange;
        float viewRangeMultiplier = Math.max(0.1F, viewRangeBlocks / 64.0F);
        Location displayLocation = location.getBlock().getLocation().add(0.5, 0.55, 0.5);

        this.blockDisplay = world.spawn(displayLocation, ItemDisplay.class, display -> {
            display.setPersistent(false);
            display.setItemStack(barrelItem.clone());
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setViewRange(viewRangeMultiplier);
            display.setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F),
                    new Vector3f(0.55F, 0.55F, 0.55F),
                    new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)
            ));
        });
    }

    @Override
    public void removeDisplayBlock() {
        Display display = this.blockDisplay;
        if (display == null)
            return;

        this.blockDisplay = null;

        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(display::remove);
        } else if (display.isValid()) {
            display.remove();
        }
    }

    public Display getDisplayBlock() {
        return blockDisplay;
    }

    @Override
    public ItemStack getBarrelItem(int amount) {
        ItemStack itemStack = barrelItem.clone();

        if (plugin.getSettings().dropStackedItem) {
            itemStack = ItemUtils.setSpawnerItemAmount(itemStack, amount);
            itemStack.setAmount(1);

            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                String formattedName = WildStackerPlugin.getPlugin().getSettings().giveItemName
                        .replace("{0}", String.valueOf(amount))
                        .replace("{1}", ItemUtils.getFormattedType(new ItemStack(getType())))
                        .replace("{2}", "Barrel");
                itemMeta.displayName(SECTION_SERIALIZER.deserialize(formattedName));
                itemStack.setItemMeta(itemMeta);
            }
        } else {
            itemStack.setAmount(amount);
        }

        return itemStack;
    }

    @Override
    public Location getLocation() {
        return object.getLocation();
    }

    @Override
    public Chunk getChunk() {
        return getLocation().getChunk();
    }

    @Override
    public int getStackLimit() {
        int limit = plugin.getSettings().barrelsLimits.getOrDefault(barrelItem.getType(), Integer.MAX_VALUE);
        return limit < 1 ? Integer.MAX_VALUE : limit;
    }

    @Override
    public int getMergeRadius() {
        int radius = plugin.getSettings().barrelsMergeRadius.getOrDefault(barrelItem.getType(), 0);
        return radius < 1 ? 0 : radius;
    }

    @Override
    public boolean isBlacklisted() {
        return plugin.getSettings().blacklistedBarrels.contains(barrelItem.getType());
    }

    @Override
    public boolean isWhitelisted() {
        return plugin.getSettings().whitelistedBarrels.size() == 0 ||
                plugin.getSettings().whitelistedBarrels.contains(barrelItem.getType());
    }

    @Override
    public boolean isWorldDisabled() {
        return plugin.getSettings().barrelsDisabledWorlds.contains(object.getWorld().getName());
    }

    @Override
    public boolean isCached() {
        return plugin.getSettings().barrelsStackingEnabled && super.isCached();
    }

    @Override
    public void remove() {
        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(this::remove);
            return;
        }

        plugin.getSystemManager().removeStackObject(this);
        plugin.getDataHandler().deleteBarrel(getLocation());

        removeHologram();
        removeDisplayBlock();

        List<HumanEntity> viewers = new ArrayList<>();
        linkedInventories.forEach(i -> viewers.addAll(i.getViewers()));
        viewers.forEach(HumanEntity::closeInventory);
        linkedInventories.clear();
    }

    @Override
    public void updateName() {
        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(this::updateName);
            return;
        }

        String customName = plugin.getSettings().barrelsCustomName;

        if (customName == null || customName.isEmpty())
            return;

        int amount = getStackAmount();

        if (amount < 1) {
            removeHologram();
            return;
        }

        customName = plugin.getSettings().barrelsNameBuilder.build(this);
        setHologramName(customName, false);
    }

    @Override
    public StackCheckResult runStackCheck(StackedObject<?> stackedObject) {
        if (!plugin.getSettings().barrelsStackingEnabled)
            return StackCheckResult.BLACKLISTED;

        return super.runStackCheck(stackedObject);
    }

    @Override
    public Optional<Block> runStack() {
        if (getStackLimit() <= 1)
            return Optional.empty();

        Chunk chunk = getChunk();

        boolean chunkMerge = plugin.getSettings().chunkMergeBarrels;
        Location blockLocation = getLocation();

        Stream<StackedBarrel> barrelStream;

        if (chunkMerge) {
            barrelStream = plugin.getSystemManager().getStackedBarrels(chunk).stream();
        } else {
            int range = getMergeRadius();

            if (range <= 0)
                return Optional.empty();

            barrelStream = plugin.getDataHandler().stackedBarrelStore.collectInRange(blockLocation, range).stream();
        }

        Optional<StackedBarrel> barrelOptional = GeneralUtils.getClosest(blockLocation, barrelStream
                .filter(stackedBarrel -> runStackCheck(stackedBarrel) == StackCheckResult.SUCCESS));

        if (barrelOptional.isPresent()) {
            StackedBarrel targetBarrel = barrelOptional.get();

            StackResult stackResult = runStack(targetBarrel);

            if (stackResult == StackResult.SUCCESS) {
                return barrelOptional.map(StackedBarrel::getBlock);
            }
        }

        return Optional.empty();
    }

    @Override
    public StackResult runStack(StackedObject<?> stackedObject) {
        if (runStackCheck(stackedObject) != StackCheckResult.SUCCESS)
            return StackResult.NOT_SIMILAR;

        StackedBarrel targetBarrel = (StackedBarrel) stackedObject;

        if (!EventsCaller.callBarrelStackEvent(targetBarrel, this))
            return StackResult.EVENT_CANCELLED;

        targetBarrel.increaseStackAmount(getStackAmount(), true);

        this.remove();

        spawnStackParticle(true);

        return StackResult.SUCCESS;
    }

    @Override
    public UnstackResult runUnstack(int amount, Entity entity) {
        if (!EventsCaller.callBarrelUnstackEvent(this, entity, amount))
            return UnstackResult.EVENT_CANCELLED;

        int newStackAmount = decreaseStackAmount(amount, true);

        if (newStackAmount < 1)
            remove();

        return UnstackResult.SUCCESS;
    }

    @Override
    public boolean isSimilar(StackedObject<?> stackedObject) {
        return stackedObject instanceof StackedBarrel other &&
                getType() == other.getType() && getData() == other.getData();
    }

    @Override
    public void spawnStackParticle(boolean checkEnabled) {
        if (!checkEnabled || plugin.getSettings().barrelsParticlesEnabled) {
            Location location = getLocation();
            for (ParticleWrapper particleWrapper : plugin.getSettings().barrelsParticles)
                particleWrapper.spawnParticle(location);
        }
    }

    @Override
    public World getWorld() {
        return object.getWorld();
    }

    @Override
    public int hashCode() {
        return getLocation().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StackedBarrel other ? getLocation().equals(other.getLocation()) : super.equals(obj);
    }

    @Override
    public String toString() {
        return String.format("StackedBarrel{location=%s,amount=%s,type=%s,data=%s}", getLocation(), getStackAmount(), getType().name(), getData());
    }

    public void linkInventory(Inventory inventory) {
        this.linkedInventories.add(inventory);
    }

    public void unlinkInventory(Inventory inventory) {
        this.linkedInventories.remove(inventory);
    }

}