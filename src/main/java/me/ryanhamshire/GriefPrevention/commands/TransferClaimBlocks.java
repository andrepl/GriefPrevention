package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class TransferClaimBlocks extends BaseCommand {

    public TransferClaimBlocks(GriefPrevention plugin) {
        super(plugin, "transferclaimblocks");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (args.size() < 3) {
            return false;
        }

        String sourcePlayerName = args.pop();
        List<Player> matches = plugin.getServer().matchPlayer(sourcePlayerName);
        if (matches.size() != 1) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.PlayerNotFound, sourcePlayerName);
            return true;
        }

        String targetPlayerName = args.pop();
        matches = plugin.getServer().matchPlayer(targetPlayerName);
        if (matches.size() != 1) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.PlayerNotFound, targetPlayerName);
            return true;
        }

        int desiredxfer = 0;
        try {
            desiredxfer = Integer.parseInt(args.peek());
        } catch (NumberFormatException nfe) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.NotANumber, args.peek());
            return true;
        }

        if (desiredxfer == 0) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.TransferBlocksLessThanOne);
            return true;
        }

        int amtXferred = plugin.transferClaimBlocks(sourcePlayerName, targetPlayerName, desiredxfer);
        if (amtXferred == 0) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.TransferBlocksError);
        } else {
            GriefPrevention.sendMessage(sender, TextMode.SUCCESS, Messages.TransferBlocksSuccess, targetPlayerName, Integer.toString(amtXferred));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
