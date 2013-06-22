package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.LinkedList;
import java.util.List;

public class Reload extends BaseCommand {

    public Reload(GriefPreventionTNG plugin) {
        super(plugin, "gpreload");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (sender.hasPermission("griefprevention.reload")) {
            plugin.onDisable();
            plugin.onEnable();
            plugin.sendMessage(sender, TextMode.ERROR, Messages.PluginReloaded);
        } else {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.NoPermissionForCommand);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
