package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class DeleteClaim extends BaseClaimCommand {

    public DeleteClaim(GriefPrevention plugin) {
        super(plugin, "deleteclaim", Messages.ClaimMissing);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        //deleting an admin claim additionally requires the adminclaims permission
        if (!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims")) {
            PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
            if (claim.getChildren().size() > 0 && !playerData.isWarnedAboutMajorDeletion()) {
                plugin.sendMessage(player, TextMode.WARN, Messages.DeletionSubdivisionWarning);
                playerData.setWarnedAboutMajorDeletion(true);
            } else if (claim.isNeverDelete() && !playerData.isWarnedAboutMajorDeletion()) {
                plugin.sendMessage(player, TextMode.WARN, Messages.DeleteLockedClaimWarning);
                playerData.setWarnedAboutMajorDeletion(true);
            } else {
                claim.removeSurfaceFluids(null);
                plugin.getDataStore().deleteClaim(claim, null, true);

                //if in a creative mode world, /restorenature the claim
                if (wc.getAutoRestoreUnclaimed() && plugin.creativeRulesApply(claim.getMin())) {
                    plugin.restoreClaim(claim, 0);
                }
                plugin.sendMessage(player, TextMode.SUCCESS, Messages.DeleteSuccess);
                plugin.getLogger().info(player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(claim.getMin()));

                //revert any current visualization
                Visualization.Revert(plugin, player);
                playerData.setWarnedAboutMajorDeletion(false);
            }
        } else {
            plugin.sendMessage(player, TextMode.ERROR, Messages.CantDeleteAdminClaim);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
