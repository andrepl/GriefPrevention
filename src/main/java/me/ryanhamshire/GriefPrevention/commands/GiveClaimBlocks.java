package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class GiveClaimBlocks extends BaseCommand {

    public GiveClaimBlocks(GriefPrevention plugin) {
        super(plugin, "giveclaimblocks");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (args.size() < 2) {
            return false;
        }
        if (!(sender instanceof Player)) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }

        String targetPlayerName = args.pop();
        List<Player> matches = plugin.getServer().matchPlayer(targetPlayerName);
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

        int amtXferred = plugin.transferClaimBlocks(sender.getName(), targetPlayerName, desiredxfer);
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
