package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.configuration.WorldConfig;
import com.norcode.bukkit.griefprevention.data.PlayerData;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import com.norcode.bukkit.griefprevention.visualization.Visualization;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class AbandonAllClaims extends BaseCommand {

    public AbandonAllClaims(GriefPreventionTNG plugin) {
        super(plugin, "abandonallclaims");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (args.size() > 1) return false;
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }
        Player player = (Player) sender;
        boolean deleteLocked = false;
        if (args.size() > 0) {
            try {
                deleteLocked = Boolean.parseBoolean(args.peek());
            } catch (IllegalArgumentException ex) {
                plugin.sendMessage(sender, TextMode.ERROR, Messages.BooleanParseError, args.peek());
            }
        }

        WorldConfig wc = plugin.getWorldCfg(player.getWorld());

        if (!wc.getAllowUnclaim()) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.NoCreativeUnClaim);
            return true;
        }

        //count claims
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        int originalClaimCount = playerData.getClaims().size();

        //check count
        if (originalClaimCount == 0) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.YouHaveNoClaims);
            return true;
        }

        //delete them
        plugin.getDataStore().deleteClaimsForPlayer(player.getName(), false, deleteLocked);

        //inform the player
        int remainingBlocks = playerData.getRemainingClaimBlocks();
        if (deleteLocked) {
            plugin.sendMessage(player, TextMode.SUCCESS, Messages.SuccessfulAbandonIncludingLocked, String.valueOf(remainingBlocks));
        } else {
            plugin.sendMessage(player, TextMode.SUCCESS, Messages.SuccessfulAbandonExcludingLocked, String.valueOf(remainingBlocks));
        }

        //revert any current visualization
        Visualization.Revert(plugin, player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
