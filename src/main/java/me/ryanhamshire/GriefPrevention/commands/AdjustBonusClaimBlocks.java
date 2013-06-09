package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.Messages;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class AdjustBonusClaimBlocks extends BaseCommand {

    public AdjustBonusClaimBlocks(GriefPrevention plugin) {
        super(plugin, "adjustbonusclaimblocks");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        //requires exactly two parameters, the other player or group's name and the adjustment
        if (args.size() != 2) return false;

        //parse the adjustment amount
        int adjustment;
        try {
            adjustment = Integer.parseInt(args.peekLast());
        } catch (NumberFormatException numberFormatException) {
            return false;  //causes usage to be displayed
        }

        //if granting blocks to all players with a specific permission
        if (args.peek().startsWith("[") && args.peek().endsWith("]")) {
            String permissionIdentifier = args.peek().substring(1, args.peek().length() - 1);
            int newTotal = plugin.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);

            if (sender instanceof Player) {
                GriefPrevention.sendMessage(sender, TextMode.SUCCESS, Messages.AdjustGroupBlocksSuccess, permissionIdentifier, String.valueOf(adjustment), String.valueOf(newTotal));
                GriefPrevention.addLogEntry(sender.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");
            }
            return true;
        }

        //otherwise, find the specified player
        OfflinePlayer targetPlayer = plugin.resolvePlayer(args.peek());
        if (targetPlayer == null) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.PlayerNotFound);
            return true;
        }

        //give blocks to player
        PlayerData playerData = plugin.dataStore.getPlayerData(targetPlayer.getName());
        playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
        plugin.dataStore.savePlayerData(targetPlayer.getName(), playerData);
        GriefPrevention.sendMessage(sender, TextMode.SUCCESS, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.getBonusClaimBlocks()));
        if (sender instanceof Player) {
            GriefPrevention.addLogEntry(sender.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
