package com.bgsoftware.wildstacker.command.commands;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.command.ICommand;
import com.bgsoftware.wildstacker.handlers.SettingsHandler;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class CommandGive implements ICommand {

    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    @Override
    public String getLabel() {
        return "give";
    }

    @Override
    public String getUsage() {
        return "stacker give [-s] <player-name> <spawner/barrel> <entity-type/material-type> [stack-size]";
    }

    @Override
    public String getPermission() {
        return "wildstacker.give";
    }

    @Override
    public String getDescription() {
        return "Give a stacked egg, a spawner or a barrel to a player.";
    }

    @Override
    public int getMinArgs() {
        return 4;
    }

    @Override
    public int getMaxArgs() {
        return 6;
    }

    @Override
    public void perform(WildStackerPlugin plugin, CommandSender sender, String[] args) {
        boolean silence = false;

        if (args[1].equalsIgnoreCase("-s")) {
            silence = true;
            if (args.length < 5) {
                Locale.COMMAND_USAGE.send(sender, getUsage());
                return;
            }
            String[] shifted = new String[args.length - 1];
            shifted[0] = args[0];
            System.arraycopy(args, 2, shifted, 1, args.length - 2);
            args = shifted;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            Locale.INVALID_PLAYER.send(sender, args[1]);
            return;
        }

        ItemStack itemStack;
        String typeName;

        int stackSize = 1;
        if (args.length >= 5) {
            try {
                stackSize = Integer.parseInt(args[4]);
                if (stackSize <= 0) {
                    Locale.INVALID_NUMBER.send(sender);
                    return;
                }
            } catch (NumberFormatException ex) {
                Locale.INVALID_NUMBER.send(sender);
                return;
            }
        }

        String giveType = args[2].toLowerCase(java.util.Locale.ENGLISH);

        if (giveType.equals("spawner")) {
            EntityType entityType = SettingsHandler.parseEntityType(args[3]);
            if (entityType == null) {
                Locale.INVALID_ENTITY.send(sender, args[3]);
                return;
            }
            typeName = ItemUtils.getFormattedEntityType(entityType);
            itemStack = ItemUtils.getSpawnerItem(entityType, stackSize);
        } else if (giveType.equals("barrel")) {
            Material barrelMaterial = SettingsHandler.parseMaterial(args[3], null);
            if (barrelMaterial == null || !plugin.getSystemManager().isBarrelBlock(barrelMaterial, target.getWorld())) {
                Locale.INVALID_BARREL.send(sender, args[3]);
                return;
            }
            typeName = ItemUtils.getFormattedType(new ItemStack(barrelMaterial));
            itemStack = ItemUtils.setSpawnerItemAmount(new ItemStack(barrelMaterial), stackSize);
        } else {
            Locale.INVALID_TYPE.send(sender);
            return;
        }

        String formattedTypeArg = Character.toUpperCase(giveType.charAt(0)) + giveType.substring(1);

        if (!giveType.equals("spawner") || plugin.getSettings().spawnerItemName.isEmpty()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                String formattedGiveName = plugin.getSettings().giveItemName
                        .replace("{0}", String.valueOf(stackSize))
                        .replace("{1}", typeName)
                        .replace("{2}", formattedTypeArg);
                itemMeta.displayName(SECTION_SERIALIZER.deserialize(formattedGiveName));
                itemStack.setItemMeta(itemMeta);
            }
        }

        ItemUtils.addItem(itemStack, target.getInventory(), target.getLocation());

        String amountStr = String.valueOf(stackSize);

        if (!silence || !(sender instanceof Player))
            Locale.STACK_GIVE_PLAYER.send(sender, target.getName(), amountStr, typeName, formattedTypeArg);

        if (!silence && !target.equals(sender))
            Locale.STACK_RECEIVE.send(target, amountStr, typeName + " " + formattedTypeArg, sender.getName());
    }

    @Override
    public List<String> tabComplete(WildStackerPlugin plugin, CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("-s"))
            return tabCompleteSilence(plugin, args);

        List<String> list = new ArrayList<>();

        switch (args.length) {
            case 2:
                return null;
            case 3:
                Stream.of("spawner", "barrel")
                        .filter(stack -> stack.startsWith(args[2].toLowerCase(java.util.Locale.ENGLISH)))
                        .forEach(list::add);
                break;
            case 4:
                String prefix = args[3].toLowerCase(java.util.Locale.ENGLISH);
                if (args[2].equalsIgnoreCase("spawner")) {
                    Arrays.stream(EntityType.values())
                            .filter(entityType -> entityType.name().toLowerCase(java.util.Locale.ENGLISH).startsWith(prefix) &&
                                    entityType.getEntityClass() != null && LivingEntity.class.isAssignableFrom(entityType.getEntityClass()))
                            .forEach(entityType -> list.add(entityType.name().toLowerCase(java.util.Locale.ENGLISH)));
                } else if (args[2].equalsIgnoreCase("barrel")) {
                    for (Material mat : Material.values()) {
                        if (mat.isBlock() && !mat.isAir() && mat.name().toLowerCase(java.util.Locale.ENGLISH).startsWith(prefix)) {
                            list.add(mat.name().toLowerCase(java.util.Locale.ENGLISH));
                        }
                    }
                }
                break;
        }

        return list;
    }

    private List<String> tabCompleteSilence(WildStackerPlugin plugin, String[] args) {
        List<String> list = new ArrayList<>();

        switch (args.length) {
            case 3:
                return null;
            case 4:
                Stream.of("spawner", "barrel")
                        .filter(stack -> stack.startsWith(args[3].toLowerCase(java.util.Locale.ENGLISH)))
                        .forEach(list::add);
                break;
            case 5:
                String prefix = args[4].toLowerCase(java.util.Locale.ENGLISH);
                if (args[3].equalsIgnoreCase("spawner")) {
                    Arrays.stream(EntityType.values())
                            .filter(entityType -> entityType.name().toLowerCase(java.util.Locale.ENGLISH).startsWith(prefix) &&
                                    entityType.getEntityClass() != null && LivingEntity.class.isAssignableFrom(entityType.getEntityClass()))
                            .forEach(entityType -> list.add(entityType.name().toLowerCase(java.util.Locale.ENGLISH)));
                } else if (args[3].equalsIgnoreCase("barrel")) {
                    for (Material mat : Material.values()) {
                        if (mat.isBlock() && !mat.isAir() && mat.name().toLowerCase(java.util.Locale.ENGLISH).startsWith(prefix)) {
                            list.add(mat.name().toLowerCase(java.util.Locale.ENGLISH));
                        }
                    }
                }
                break;
        }

        return list;
    }

}