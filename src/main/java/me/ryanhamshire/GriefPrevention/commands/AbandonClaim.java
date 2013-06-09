package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.*;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class AbandonClaim extends BaseClaimCommand {

    public AbandonClaim(GriefPrevention plugin) {
        super(plugin, "abandonclaim", Messages.AbandonClaimMissing);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {

        boolean deleteTopLevel = args.size() == 1 && args.peek().equalsIgnoreCase("toplevel");

        PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());

        WorldConfig wc = plugin.getWorldCfg(player.getWorld());

        int claimarea = claim.getArea();
        // retrieve (1-abandonclaimration)*totalarea to get amount to subtract from the accrued claim blocks
        // after we delete the claim.
        int costoverhead = (int) Math.floor((double) claimarea * (1 - wc.getClaimsAbandonReturnRatio()));

        // verify ownership
        if (claim.allowEdit(player) != null) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NotYourClaim);
        }

        // don't allow abandon of claims if not configured to allow.
        else if (!wc.getAllowUnclaim()) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoCreativeUnClaim);
        }

        // warn if has children and we're not explicitly deleting a top level claim
        else if (claim.getChildren().size() > 0 && !deleteTopLevel) {
            GriefPrevention.sendMessage(player, TextMode.INSTR, Messages.DeleteTopLevelClaim);
            return true;
        }

        // if the claim is locked, let's warn the player and give them a chance to back out
        else if (!playerData.isWarnedAboutMajorDeletion() && claim.isNeverdelete()) {
            GriefPrevention.sendMessage(player, TextMode.WARN, Messages.ConfirmAbandonLockedClaim);
            playerData.setWarnedAboutMajorDeletion(true);
        }
        // if auto-restoration is enabled,
        else if (!playerData.isWarnedAboutMajorDeletion() && wc.getClaimsAbandonNatureRestoration()) {
            GriefPrevention.sendMessage(player, TextMode.WARN, Messages.AbandonClaimRestoreWarning);
            playerData.setWarnedAboutMajorDeletion(true);
        } else if (!playerData.isWarnedAboutMajorDeletion() && costoverhead != claimarea) {
            playerData.setWarnedAboutMajorDeletion(true);
            GriefPrevention.sendMessage(player, TextMode.WARN, Messages.AbandonCostWarning, String.valueOf(costoverhead));
        }
        // if the claim has lots of surface water or some surface lava, warn the player it will be cleaned up
        else if (!playerData.isWarnedAboutMajorDeletion() && claim.hasSurfaceFluids() && claim.getParent() == null) {
            GriefPrevention.sendMessage(player, TextMode.WARN, Messages.ConfirmFluidRemoval);
            playerData.setWarnedAboutMajorDeletion(true);
        } else {
            // delete it
            // Only do water/lava cleanup when it's a top level claim.
            if (claim.getParent() == null) {
                claim.removeSurfaceFluids(null);
            }
            // retrieve area of this claim...


            if (!plugin.dataStore.deleteClaim(claim, player)) {
                // cancelled!
                // assume the event called will show an appropriate message...
                return false;
            }

            // if in a creative mode world, restore the claim area
            // CHANGE: option is now determined by configuration options.
            // if we are in a creative world and the creative Abandon Nature restore option is enabled,
            // or if we are in a survival world and the creative Abandon Nature restore option is enabled,
            // then perform the restoration.
            if ((wc.getClaimsAbandonNatureRestoration())) {

                GriefPrevention.addLogEntry(player.getName() + " abandoned a claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                GriefPrevention.sendMessage(player, TextMode.WARN, Messages.UnclaimCleanupWarning);
                GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
            }
            // remove the interest cost, and message the player.
            if (costoverhead > 0) {
                playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - costoverhead);

                GriefPrevention.sendMessage(player, TextMode.WARN, Messages.AbandonCost, 0, String.valueOf(costoverhead));
            }
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            // tell the player how many claim blocks he has left
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.AbandonSuccess, 0, String.valueOf(remainingBlocks));

            // revert any current visualization
            Visualization.Revert(player);

            playerData.setWarnedAboutMajorDeletion(false);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}