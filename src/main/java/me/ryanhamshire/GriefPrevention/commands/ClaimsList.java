package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.*;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class ClaimsList extends BaseCommand {

    public ClaimsList(GriefPrevention plugin) {
        super(plugin, "claimslist");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        //player whose claims will be listed
        OfflinePlayer otherPlayer = null;

        if (args.size() > 1) return false;

        if (args.size() == 0) {
            if (!(sender instanceof Player)) {
                return false;
            } else {
                otherPlayer = (Player) sender;
            }
        } else {
            otherPlayer = plugin.resolvePlayer(args.peek());
            if (otherPlayer == null) {
                plugin.sendMessage(sender, TextMode.ERROR, Messages.PlayerNotFound);
                return true;
            }
        }

        //otherwise if no permission to delve into another player's claims data
        if (!sender.getName().equals(otherPlayer.getName()) && !sender.hasPermission("griefprevention.deleteclaims")) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.ClaimsListNoPermission);
            return true;
        }

        //load the target player's data
        PlayerData playerData = plugin.getDataStore().getPlayerData(otherPlayer.getName());
        plugin.sendMessage(sender, TextMode.INSTR, " " + playerData.getAccruedClaimBlocks() + "(+" + (playerData.getBonusClaimBlocks() + plugin.getDataStore().getGroupBonusBlocks(otherPlayer.getName())) + ")=" + (playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks() + plugin.getDataStore().getGroupBonusBlocks(otherPlayer.getName())));
        for (int i = 0; i < playerData.getClaims().size(); i++) {
            Claim claim = playerData.getClaims().get(i);
            plugin.sendMessage(sender, TextMode.INSTR, "  (-" + claim.getArea() + ") " + plugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
        }

        if (playerData.getClaims().size() > 0)
            plugin.sendMessage(sender, TextMode.INSTR, "   =" + playerData.getRemainingClaimBlocks());

        return true;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
