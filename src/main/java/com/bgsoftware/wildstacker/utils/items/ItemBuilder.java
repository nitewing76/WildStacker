package com.bgsoftware.wildstacker.utils.items;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public final class ItemBuilder {

    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    private ItemStack itemStack;
    private ItemMeta itemMeta;
    private String texture;

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    public ItemBuilder(Material type) {
        this(type, 0);
    }

    public ItemBuilder(Material type, int damage) {
        this.itemStack = new ItemStack(type == null ? Material.STONE : type, 1);
        this.itemMeta = this.itemStack.getItemMeta();
        if (damage > 0 && this.itemMeta instanceof Damageable damageable) {
            damageable.setDamage(damage);
        }
    }

    public ItemBuilder withAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder withName(String name) {
        if (itemMeta != null && name != null) {
            itemMeta.displayName(deserialize(name));
        }
        return this;
    }

    public ItemBuilder withLore(String firstLine, ConfigurationSection configurationSection) {
        if (itemMeta == null)
            return this;

        List<String> loreList = new ArrayList<>();
        loreList.add(firstLine);

        if (configurationSection != null) {
            for (String section : configurationSection.getKeys(false)) {
                loreList.add(section + ": " + configurationSection.get(section));
            }
        }

        if (loreList.size() > 16) {
            loreList = new ArrayList<>(loreList.subList(0, 16));
            loreList.add("...");
        }

        return withLore(loreList);
    }

    public ItemBuilder withLore(String firstLine, List<String> listLine) {
        if (itemMeta == null)
            return this;

        List<String> loreList = new ArrayList<>();
        loreList.add(firstLine);
        if (listLine != null) {
            loreList.addAll(listLine);
        }

        if (loreList.size() > 10) {
            loreList = new ArrayList<>(loreList.subList(0, 10));
            loreList.add("...");
        }

        return withLore(loreList);
    }

    public ItemBuilder withLore(List<String> lore) {
        if (itemMeta != null && lore != null) {
            List<Component> components = lore.stream()
                    .map(ItemBuilder::deserialize)
                    .collect(Collectors.toList());
            itemMeta.lore(components);
        }
        return this;
    }

    public ItemBuilder withLore(String... lore) {
        if (itemMeta != null && lore != null) {
            List<Component> components = new ArrayList<>(lore.length);
            for (String line : lore) {
                components.add(deserialize(line));
            }
            itemMeta.lore(components);
        }
        return this;
    }

    public ItemBuilder replaceName(String regex, String replace) {
        if (itemMeta != null && itemMeta.hasDisplayName() && itemMeta.displayName() != null) {
            String legacy = SECTION_SERIALIZER.serialize(itemMeta.displayName());
            itemMeta.displayName(SECTION_SERIALIZER.deserialize(legacy.replace(regex, replace)));
        }
        return this;
    }

    public ItemBuilder replaceLore(String regex, String replace) {
        if (itemMeta == null || !itemMeta.hasLore() || itemMeta.lore() == null)
            return this;

        List<Component> updated = new ArrayList<>();
        for (Component line : itemMeta.lore()) {
            String legacy = SECTION_SERIALIZER.serialize(line);
            updated.add(SECTION_SERIALIZER.deserialize(legacy.replace(regex, replace)));
        }
        itemMeta.lore(updated);
        return this;
    }

    public ItemBuilder replaceAll(String regex, String replace) {
        replaceName(regex, replace);
        replaceLore(regex, replace);
        return this;
    }

    public ItemBuilder withEnchant(Enchantment enchant, int level) {
        if (itemMeta != null) {
            if (enchant == null) {
                itemMeta.setEnchantmentGlintOverride(true);
            } else {
                itemMeta.addEnchant(enchant, level, true);
            }
        }
        return this;
    }

    public ItemBuilder withGlow() {
        if (itemMeta != null) {
            itemMeta.setEnchantmentGlintOverride(true);
        }
        return this;
    }

    public ItemBuilder withFlags(ItemFlag... itemFlags) {
        if (itemMeta != null && itemFlags != null) {
            itemMeta.addItemFlags(itemFlags);
        }
        return this;
    }

    public ItemBuilder asSkullOf(String texture) {
        if (itemStack.getType() == Material.PLAYER_HEAD) {
            this.texture = texture;
        }
        return this;
    }

    public ItemStack build(int amount) {
        itemStack.setAmount(amount);
        return build();
    }

    public ItemStack build() {
        if (texture != null && !texture.isEmpty() && itemMeta instanceof SkullMeta skullMeta) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "WildStacker");
            profile.setProperty(new ProfileProperty("textures", texture));
            skullMeta.setPlayerProfile(profile);
        }
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public ItemBuilder copy() {
        ItemBuilder itemBuilder = new ItemBuilder(Material.AIR);
        itemBuilder.itemStack = itemStack.clone();
        itemBuilder.itemMeta = itemMeta == null ? null : itemMeta.clone();
        itemBuilder.texture = this.texture;
        return itemBuilder;
    }

    private static Component deserialize(String input) {
        if (input == null || input.isEmpty())
            return Component.empty();
        if (input.indexOf('\u00A7') != -1) {
            return SECTION_SERIALIZER.deserialize(input);
        }
        return AMPERSAND_SERIALIZER.deserialize(input);
    }

}