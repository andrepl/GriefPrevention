package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TrustList extends BaseClaimCommand {

    public TrustList(GriefPrevention plugin) {
        super(plugin, "trustlist", Messages.TrustListNoClaim);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {

        //if no permission to manage permissions, error message
        String errorMessage = claim.allowGrantPermission(player);
        if (errorMessage != null) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, errorMessage);
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
        permissions.append(ChatColor.GOLD + "M: ");

        if (managers.size() > 0) {
            for (int i = 0; i < managers.size(); i++) {
                permissions.append(managers.get(i) + " ");
            }
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.YELLOW + "B: ");

        if (builders.size() > 0) {
            for (int i = 0; i < builders.size(); i++) {
                permissions.append(builders.get(i) + " ");
            }
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.GREEN + "C: ");

        if (containers.size() > 0) {
            for (int i = 0; i < containers.size(); i++) {
                permissions.append(containers.get(i) + " ");
            }
        }

        player.sendMessage(permissions.toString());
        permissions = new StringBuilder();
        permissions.append(ChatColor.BLUE + "A :");

        if (accessors.size() > 0) {
            for (int i = 0; i < accessors.size(); i++) {
                permissions.append(accessors.get(i) + " ");
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
