package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.*;
import me.ryanhamshire.GriefPrevention.configuration.Messages;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
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
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            if (claim.getChildren().size() > 0 && !playerData.isWarnedAboutMajorDeletion()) {
                GriefPrevention.sendMessage(player, TextMode.WARN, Messages.DeletionSubdivisionWarning);
                playerData.setWarnedAboutMajorDeletion(true);
            } else if (claim.isNeverdelete() && !playerData.isWarnedAboutMajorDeletion()) {
                GriefPrevention.sendMessage(player, TextMode.WARN, Messages.DeleteLockedClaimWarning);
                playerData.setWarnedAboutMajorDeletion(true);
            } else {
                claim.removeSurfaceFluids(null);
                plugin.dataStore.deleteClaim(claim);

                //if in a creative mode world, /restorenature the claim
                if (wc.getAutoRestoreUnclaimed() && plugin.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                    plugin.restoreClaim(claim, 0);
                }
                GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.DeleteSuccess);
                GriefPrevention.addLogEntry(player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));

                //revert any current visualization
                Visualization.Revert(player);
                playerData.setWarnedAboutMajorDeletion(false);
            }
        } else {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CantDeleteAdminClaim);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
