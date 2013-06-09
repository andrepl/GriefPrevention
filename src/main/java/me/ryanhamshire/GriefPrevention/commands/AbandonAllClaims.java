package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.Messages;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class AbandonAllClaims extends BaseCommand {

    public AbandonAllClaims(GriefPrevention plugin) {
        super(plugin, "abandonallclaims");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (args.size() > 1) return false;
        if (!(sender instanceof Player)) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }
        Player player = (Player) sender;
        boolean deleteLocked = false;
        if (args.size() > 0) {
            try {
                deleteLocked = Boolean.parseBoolean(args.peek());
            } catch (IllegalArgumentException ex) {
                GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.BooleanParseError, args.peek());
            }
        }

        WorldConfig wc = plugin.getWorldCfg(player.getWorld());

        if (!wc.getAllowUnclaim()) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoCreativeUnClaim);
            return true;
        }

        //count claims
        PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
        int originalClaimCount = playerData.getClaims().size();

        //check count
        if (originalClaimCount == 0) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.YouHaveNoClaims);
            return true;
        }

        //delete them
        plugin.dataStore.deleteClaimsForPlayer(player.getName(), false, deleteLocked);

        //inform the player
        int remainingBlocks = playerData.getRemainingClaimBlocks();
        if (deleteLocked) {
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.SuccessfulAbandonIncludingLocked, String.valueOf(remainingBlocks));
        } else {
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.SuccessfulAbandonExcludingLocked, String.valueOf(remainingBlocks));
        }

        //revert any current visualization
        Visualization.Revert(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
