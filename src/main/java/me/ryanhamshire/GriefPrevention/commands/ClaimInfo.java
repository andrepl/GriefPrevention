package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class ClaimInfo extends BaseCommand {

    public ClaimInfo(GriefPrevention plugin) {
        super(plugin, "claiminfo");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        Claim claim = null;
        if (args.size() == 1) {
            int claimid = Integer.parseInt(args.peek());
            claim = plugin.dataStore.getClaim(claimid);
            if (claim == null) {
                GriefPrevention.sendMessage(sender, TextMode.ERROR, "Invalid Claim ID:" + claimid);
                return true;
            }
        } else if (!(sender instanceof Player)) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        } else {
            claim = plugin.dataStore.getClaimAt(((Player) sender).getLocation(), true, null);
        }

        if (claim == null) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.ClaimMissing);
            return true;
        } else {
            //there is a claim, show all sorts of pointless info about it.
            //we do not show trust, since that can be shown with /trustlist.
            //first, get the upper and lower boundary.
            //see that it has Children.
            String lowerBoundary = GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner());
            String upperBoundary = GriefPrevention.getfriendlyLocationString(claim.getGreaterBoundaryCorner());
            String sizeString = "(" + String.valueOf(claim.getWidth()) + ", " + String.valueOf(claim.getHeight()) + ")";
            String ClaimOwner = claim.getOwnerName();
            GriefPrevention.sendMessage(sender, TextMode.INFO, "ID: " + claim.getID());
            GriefPrevention.sendMessage(sender, TextMode.INFO, "Position: " + lowerBoundary + "-" + upperBoundary);
            GriefPrevention.sendMessage(sender, TextMode.INFO, "Size: " + sizeString);
            GriefPrevention.sendMessage(sender, TextMode.INFO, "Owner: " + ClaimOwner);
            String parentId = claim.getParent() == null ? "(no parent)" : String.valueOf(claim.getParent().getID());
            GriefPrevention.sendMessage(sender, TextMode.INFO, "Parent ID: " + parentId);
            String childInfo = "";
            //if no subclaims, set childinfo to indicate as such.
            if (claim.getChildren().size() == 0) {
                childInfo = "No subclaims.";
            } else { //otherwise, we need to get the childclaim info by iterating through each child claim.
                childInfo = claim.getChildren().size() + " (";

                for (Claim childclaim : claim.getChildren()) {
                    childInfo += String.valueOf(childclaim.getSubClaimID()) + ",";
                }
                //remove the last character since it is a comma we do not want.
                childInfo = childInfo.substring(0, childInfo.length() - 1);
            }
            GriefPrevention.sendMessage(sender, TextMode.INFO, "Subclaims: " + childInfo);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
