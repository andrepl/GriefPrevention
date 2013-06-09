package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.LinkedList;
import java.util.List;

public class Reload extends BaseCommand {

    public Reload(GriefPrevention plugin) {
        super(plugin, "gpreload");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (sender.hasPermission("griefprevention.reload")) {
            plugin.onDisable();
            plugin.onEnable();
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.PluginReloaded);
        } else {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.NoPermissionForCommand);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
