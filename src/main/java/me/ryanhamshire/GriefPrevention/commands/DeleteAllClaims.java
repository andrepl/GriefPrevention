package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class DeleteAllClaims extends BaseCommand {

    public DeleteAllClaims(GriefPrevention plugin) {
        super(plugin, "deleteallclaims");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {

        //requires one or two parameters, the other player's name and whether to delete locked claims.
        if (args.size() < 1 && args.size() > 2) return false;

        //try to find that player
        OfflinePlayer otherPlayer = plugin.resolvePlayer(args.peekFirst());
        if (otherPlayer == null) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.PlayerNotFound);
            return true;
        }

        boolean deletelocked = false;
        if (args.size() == 2) {
            deletelocked = Boolean.parseBoolean(args.peekLast());
        }

        //delete all that player's claims
        plugin.dataStore.deleteClaimsForPlayer(otherPlayer.getName(), true, deletelocked);

        if (deletelocked) {
            plugin.sendMessage(sender, TextMode.SUCCESS, Messages.DeleteAllSuccessIncludingLocked, otherPlayer.getName());
        } else {
            plugin.sendMessage(sender, TextMode.SUCCESS, Messages.DeleteAllSuccessExcludingLocked, otherPlayer.getName());
        }
        if (sender != null) {
            GriefPrevention.addLogEntry(sender.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".");

             if (sender instanceof Player) {
                //revert any current visualization
                Visualization.Revert(plugin, (Player) sender);
             }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
