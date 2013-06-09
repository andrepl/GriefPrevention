package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class Deathblow extends BaseCommand {

    public Deathblow(GriefPrevention plugin) {
        super(plugin, "deathblow");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {

        if (args.size() < 1) return false;

        //try to find that player
        List<Player> matches = plugin.getServer().matchPlayer(args.pop());
        if (matches.size() != 1) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.PlayerNotFound);
            return true;
        }
        Player targetPlayer = matches.get(0);

        //try to find the recipient player, if specified
        Player recipientPlayer = null;
        if (args.size() > 1) {
            recipientPlayer = plugin.getServer().getPlayer(args.peek());
            if (recipientPlayer == null) {
                GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.PlayerNotFound);
                return true;
            }
        }

        //if giving inventory to another player, teleport the target player to that receiving player
        if (recipientPlayer != null) {
            targetPlayer.teleport(recipientPlayer);
        }

        //otherwise, plan to "pop" the player in place
        else {
            //if in a normal world, shoot him up to the sky first, so his items will fall on the surface.
            if (targetPlayer.getWorld().getEnvironment() == World.Environment.NORMAL) {
                Location location = targetPlayer.getLocation();
                location.setY(location.getWorld().getMaxHeight());
                targetPlayer.teleport(location);
            }
        }

        //kill target player
        targetPlayer.setHealth(0);

        //log entry
        if (sender instanceof Player) {
            GriefPrevention.addLogEntry(sender.getName() + " used /DeathBlow to kill " + targetPlayer.getName() + ".");
        } else {
            GriefPrevention.addLogEntry("Killed " + targetPlayer.getName() + ".");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
