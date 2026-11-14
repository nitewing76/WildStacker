package com.bgsoftware.wildstacker.command.commands;

import com.bgsoftware.wildstacker.Locale;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.objects.StackedBarrel;
import com.bgsoftware.wildstacker.api.objects.StackedSpawner;
import com.bgsoftware.wildstacker.command.ICommand;
import com.bgsoftware.wildstacker.objects.WStackedBarrel;
import com.bgsoftware.wildstacker.objects.WStackedSpawner;
import com.bgsoftware.wildstacker.utils.items.ItemUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class CommandInfo implements ICommand {

    @Override
    public String getLabel() {
        return "info";
    }

    @Override
    public String getUsage() {
        return "stacker info";
    }

    @Override
    public String getPermission() {
        return "wildstacker.info";
    }

    @Override
    public String getDescription() {
        return "Shows information about a spawner.";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public void perform(WildStackerPlugin plugin, CommandSender sender, String[] args) {
        if (!(sender instanceof Player pl)) {
            Locale.sendMessage(sender, "&cOnly players can perform this command.");
            return;
        }

        Block targetBlock = pl.getTargetBlockExact(10, FluidCollisionMode.NEVER);

        if (targetBlock != null && targetBlock.getType() == Material.SPAWNER) {
            StackedSpawner stackedSpawner = WStackedSpawner.of(targetBlock);

            Locale.SPAWNER_INFO_HEADER.send(sender);
            Locale.SPAWNER_INFO_TYPE.send(sender, ItemUtils.getFormattedEntityType(stackedSpawner.getSpawnedType()));
            Locale.SPAWNER_INFO_AMOUNT.send(sender, stackedSpawner.getStackAmount());
            Locale.SPAWNER_INFO_FOOTER.send(sender);
            return;
        } else if (targetBlock != null && plugin.getSystemManager().isStackedBarrel(targetBlock)) {
            StackedBarrel stackedBarrel = WStackedBarrel.of(targetBlock);

            Locale.BARREL_INFO_HEADER.send(sender);
            Locale.BARREL_INFO_TYPE.send(sender, ItemUtils.getFormattedType(stackedBarrel.getBarrelItem(1)));
            Locale.BARREL_INFO_AMOUNT.send(sender, stackedBarrel.getStackAmount());
            Locale.BARREL_INFO_FOOTER.send(sender);
            return;
        }

        Locale.STACK_INFO_INVALID.send(sender);
    }

    @Override
    public List<String> tabComplete(WildStackerPlugin plugin, CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

}