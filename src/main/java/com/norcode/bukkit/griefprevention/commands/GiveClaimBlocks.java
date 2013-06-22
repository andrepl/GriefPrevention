package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.data.PlayerData;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class GiveClaimBlocks extends BaseCommand {

    public GiveClaimBlocks(GriefPreventionTNG plugin) {
        super(plugin, "giveclaimblocks");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (args.size() < 2) {
            return false;
        }
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }

        String targetPlayerName = args.pop();
        List<Player> matches = plugin.getServer().matchPlayer(targetPlayerName);
        if (matches.size() != 1) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.PlayerNotFound, targetPlayerName);
            return true;
        }

        int desiredxfer = 0;
        try {
            desiredxfer = Integer.parseInt(args.peek());
        } catch (NumberFormatException nfe) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.NotANumber, args.peek());
            return true;
        }

        if (desiredxfer == 0) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.TransferBlocksLessThanOne);
            return true;
        }

        int amtXferred = transferClaimBlocks(sender.getName(), targetPlayerName, desiredxfer);
        if (amtXferred == 0) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.TransferBlocksError);
        } else {
            plugin.sendMessage(sender, TextMode.SUCCESS, Messages.TransferBlocksSuccess, targetPlayerName, Integer.toString(amtXferred));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * transfers a number of claim blocks from a source player to a  target player.
     *
     * @param source player name.
     * @param target Player name.
     * @return number of claim blocks transferred.
     */
    public synchronized int transferClaimBlocks(String source, String target, int desiredAmount) {
        // transfer claim blocks from source to target, return number of claim blocks transferred.

        PlayerData playerData = plugin.getDataStore().getPlayerData(source);
        PlayerData receiverData = plugin.getDataStore().getPlayerData(target);
        if (playerData != null && receiverData != null) {
            int xferamount = Math.min(playerData.getAccruedClaimBlocks(), desiredAmount);
            playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - xferamount);
            receiverData.setAccruedClaimBlocks(receiverData.getAccruedClaimBlocks() + xferamount);
            return xferamount;
        }
        return 0;
    }

}
