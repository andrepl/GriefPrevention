package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class GiveClaim extends BaseClaimCommand {

    public GiveClaim(GriefPrevention plugin) {
        super(plugin, "giveclaim", Messages.ClaimMissing);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        //gives a claim to another player. get the source player first.
        if (args.size() == 0) return false;
        Player source = player;
        Player target = Bukkit.getPlayer(args.peek());
        if (target == null) {
            GriefPrevention.sendMessage(source, TextMode.Err, Messages.PlayerNotFound, args.peek());
            return true;
        }

        //if it's not null, make sure they have either have giveclaim permission or adminclaims permission.
        if (source.hasPermission("griefprevention.giveclaims") || source.hasPermission("griefprevention.adminclaims")) {
            //find the claim at the players location.
            Claim claimToGive = plugin.dataStore.getClaimAt(source.getLocation(), true, null);
            //if the owner is not the source, they have to have adminclaims permission too.
            if (!claimToGive.getOwnerName().equalsIgnoreCase(source.getName())) {
                //if they don't have adminclaims permission, deny it.
                if (!source.hasPermission("griefprevention.adminclaims")) {
                    GriefPrevention.sendMessage(source, TextMode.Err, Messages.NoAdminClaimsPermission);
                    return true;
                }
            }
            //transfer ownership.
            claimToGive.ownerName = target.getName();

            String originalOwner = claimToGive.getOwnerName();
            try {
                plugin.dataStore.changeClaimOwner(claimToGive, target.getName());
                //message both players.
                GriefPrevention.sendMessage(source, TextMode.Success, Messages.GiveSuccessSender, originalOwner, target.getName());
                if (target != null && target.isOnline()) {
                    GriefPrevention.sendMessage(target, TextMode.Success, Messages.GiveSuccessTarget, originalOwner);
                }
            } catch (Exception exx) {
                GriefPrevention.sendMessage(source, TextMode.Err, "Failed to transfer Claim.");
            }
        } else {
            GriefPrevention.sendMessage(source, TextMode.Err, Messages.NoGiveClaimsPermission);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
