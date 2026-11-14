package com.bgsoftware.wildstacker.utils.files;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.handlers.SettingsHandler;
import com.bgsoftware.wildstacker.menu.WildMenu;
import com.bgsoftware.wildstacker.utils.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FileUtils {

    private static final WildStackerPlugin plugin = WildStackerPlugin.getPlugin();

    private FileUtils() {
    }

    public static void saveResource(String resourcePath) {
        saveResource(resourcePath, null);
    }

    public static void saveResource(String resourcePath, @Nullable String newName) {
        try {
            File outputFile = new File(plugin.getDataFolder(), newName == null ? resourcePath : newName);
            if (!outputFile.exists()) {
                plugin.saveResource(resourcePath, false);
                if (newName != null) {
                    File resourceFile = new File(plugin.getDataFolder(), resourcePath);
                    //noinspection ResultOfMethodCallIgnored
                    resourceFile.renameTo(outputFile);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static InputStream getResource(String resourcePath) {
        return plugin.getResource(resourcePath);
    }

    public static ItemBuilder getItemStack(String fileName, ConfigurationSection section) {
        if (section == null || !section.isString("type"))
            return null;

        String materialName = section.getString("type", "");
        Material type;
        short data = (short) section.getInt("data", 0);

        try {
            type = Material.valueOf(materialName.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            Material matched = Material.matchMaterial(materialName);
            if (matched == null) {
                WildStackerPlugin.log("&c[" + fileName + "] Couldn't convert '" + materialName +
                        "' into a material in '" + section.getCurrentPath() + ".type', skipping...");
                return null;
            }
            type = matched;
        }

        ItemBuilder itemBuilder = new ItemBuilder(type, data);

        if (section.isInt("amount"))
            itemBuilder.withAmount(section.getInt("amount"));

        if (section.isString("name"))
            itemBuilder.withName(section.getString("name"));

        if (section.isList("lore"))
            itemBuilder.withLore(section.getStringList("lore"));

        ConfigurationSection enchantsSection = section.getConfigurationSection("enchants");
        if (enchantsSection != null) {
            for (String enchantName : enchantsSection.getKeys(false)) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantName.toLowerCase(Locale.ENGLISH)));
                if (enchantment == null) {
                    //noinspection deprecation
                    enchantment = Enchantment.getByName(enchantName.toUpperCase(Locale.ENGLISH));
                }
                if (enchantment == null) {
                    itemBuilder.withGlow();
                } else {
                    itemBuilder.withEnchant(enchantment, enchantsSection.getInt(enchantName, 1));
                }
            }
        }

        if (section.isBoolean("glow") && section.getBoolean("glow")) {
            itemBuilder.withGlow();
        }

        if (section.isList("flags")) {
            for (String flagName : section.getStringList("flags")) {
                try {
                    itemBuilder.withFlags(ItemFlag.valueOf(flagName.toUpperCase(Locale.ENGLISH)));
                } catch (IllegalArgumentException e) {
                    WildStackerPlugin.log("&c[" + fileName + "] Couldn't convert '" + flagName +
                            "' into an item flag in '" + section.getCurrentPath() + ".flags', skipping...");
                }
            }
        }

        if (section.isString("skull"))
            itemBuilder.asSkullOf(section.getString("skull"));

        return itemBuilder;
    }

    public static Map<Character, List<Integer>> loadGUI(WildMenu menu, String fileName, ConfigurationSection section) {
        Map<Character, List<Integer>> charSlots = new HashMap<>();

        menu.resetData();

        menu.setTitle(SettingsHandler.translateAmpersand(section.getString("title", "")));

        List<String> pattern = section.getStringList("pattern");

        menu.setRowsSize(pattern.size());

        for (int row = 0; row < pattern.size(); row++) {
            String patternLine = pattern.get(row);
            int slot = row * 9;

            for (int i = 0; i < patternLine.length(); i++) {
                char ch = patternLine.charAt(i);
                if (ch != ' ') {
                    ItemBuilder itemBuilder = getItemStack(fileName, section.getConfigurationSection("items." + ch));

                    if (itemBuilder != null) {
                        List<String> commands = section.getStringList("commands." + ch);
                        SoundWrapper sound = getSound(section.getConfigurationSection("sounds." + ch));
                        String permission = section.getString("permissions." + ch + ".permission");
                        SoundWrapper noAccessSound = getSound(section.getConfigurationSection("permissions." + ch + ".no-access-sound"));

                        menu.addFillItem(slot, itemBuilder);
                        menu.addCommands(slot, commands);
                        menu.addPermission(slot, permission, noAccessSound);
                        menu.addSound(slot, sound);
                    }

                    charSlots.computeIfAbsent(ch, k -> new ArrayList<>()).add(slot);

                    slot++;
                }
            }
        }

        return charSlots;
    }

    public static SoundWrapper getSound(ConfigurationSection section) {
        if (section == null)
            return null;

        String soundType = section.getString("type");
        if (soundType == null || soundType.isEmpty())
            return null;

        Sound sound = null;
        try {
            NamespacedKey key = NamespacedKey.minecraft(soundType.toLowerCase(Locale.ENGLISH).replace('_', '.'));
            sound = Registry.SOUNDS.get(key);
        } catch (Exception ignored) {
        }

        if (sound == null) {
            try {
                sound = Sound.valueOf(soundType.toUpperCase(Locale.ENGLISH));
            } catch (Exception ignored) {
            }
        }

        if (sound == null)
            return null;

        return new SoundWrapper(sound, (float) section.getDouble("volume", 1.0), (float) section.getDouble("pitch", 1.0));
    }

}