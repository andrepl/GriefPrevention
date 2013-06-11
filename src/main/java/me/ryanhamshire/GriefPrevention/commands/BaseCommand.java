package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class BaseCommand implements TabExecutor {

    protected GriefPrevention plugin;

    public Command pluginCommand = null;
    private String permissionNode = null;

    public BaseCommand(GriefPrevention plugin, String cmdName) {
        this.plugin = plugin;
        pluginCommand = this.plugin.getServer().getPluginCommand(cmdName);
        permissionNode = pluginCommand.getPermission();
        this.plugin.getCommandHandler().registerCommand(this, cmdName);
    }

    public Command getPluginCommand() {
        return pluginCommand;
    }

    public String getPermissionNode() {
        return permissionNode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        LinkedList<String> params = new LinkedList<String>();
        params.addAll(Arrays.asList(args));
        return onCommand(sender, cmd, label, params);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        LinkedList<String> params = new LinkedList<String>();
        params.addAll(Arrays.asList(args));
        return onTabComplete(sender, cmd, label, params);
    }

    public abstract boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args);
    public abstract List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args);

    public String[] getAliases() {
        return null;
    }
}
