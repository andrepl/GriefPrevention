package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.data.Claim;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TrustList extends BaseClaimCommand {

    public TrustList(GriefPreventionTNG plugin) {
        super(plugin, "trustlist", Messages.TrustListNoClaim);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {

        //if no permission to manage permissions, error message
        String errorMessage = claim.allowGrantPermission(player);
        if (errorMessage != null) {
            plugin.sendMessage(player, TextMode.ERROR, errorMessage);
            return true;
        }

        //otherwise build a list of explicit permissions by permission level
        //and send that to the player
        ArrayList<String> builders = new ArrayList<String>();
        ArrayList<String> containers = new ArrayList<String>();
        ArrayList<String> accessors = new ArrayList<String>();
        ArrayList<String> managers = new ArrayList<String>();
        claim.getPermissions(builders, containers, accessors, managers);

        player.sendMessage("Explicit permissions here:");

        StringBuilder permissions = new StringBuilder();
        permissions.append(ChatColor.GOLD.toString()).append("M: ");

        if (managers.size() > 0) {
            for (String manager : managers) {
                permissions.append(manager).append(" ");
            }
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.YELLOW.toString()).append("B: ");

        if (builders.size() > 0) {
            for (String builder : builders) {
                permissions.append(builder).append(" ");
            }
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.GREEN.toString()).append("C: ");

        if (containers.size() > 0) {
            for (String container : containers) {
                permissions.append(container).append(" ");
            }
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.BLUE.toString()).append("A :");

        if (accessors.size() > 0) {
            for (String accessor : accessors) {
                permissions.append(accessor).append(" ");
            }
        }
        player.sendMessage(permissions.toString());
        player.sendMessage("(M-anager, B-builder, C-ontainers, A-ccess)");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
