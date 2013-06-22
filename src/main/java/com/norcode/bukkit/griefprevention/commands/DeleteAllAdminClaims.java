package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import com.norcode.bukkit.griefprevention.visualization.Visualization;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class DeleteAllAdminClaims extends BaseCommand {

    public DeleteAllAdminClaims(GriefPreventionTNG plugin) {
        super(plugin, "deletealladminclaims");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (!sender.hasPermission("griefprevention.deleteclaims")) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.NoDeletePermission);
            return true;
        }
        //empty string for owner name indicates an administrative claim
        plugin.getDataStore().deleteClaimsForPlayer("", true, true);
        plugin.sendMessage(sender, TextMode.SUCCESS, Messages.AllAdminDeleted);
        if (sender != null) {
            plugin.getLogger().info(sender.getName() + " deleted all administrative claims.");
            if (sender instanceof Player) {
                //revert any current visualization
                Visualization.Revert(plugin, (Player) sender);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
