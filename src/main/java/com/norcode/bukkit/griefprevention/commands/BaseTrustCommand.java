package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.configuration.ClaimPermission;
import com.norcode.bukkit.griefprevention.data.Claim;
import com.norcode.bukkit.griefprevention.data.PlayerData;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BaseTrustCommand extends BaseClaimCommand {

    ClaimPermission permissionLevel;

    public BaseTrustCommand(GriefPreventionTNG plugin, String cmdName, ClaimPermission permissionLevel) {
        super(plugin, cmdName, Messages.ClaimMissing);
        this.permissionLevel = permissionLevel;
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        if (args.size() == 0) {
            return false;
        }

        String permission;
        OfflinePlayer otherPlayer;
        String recipientName = args.pop();

        boolean isForcedDenial = false;
        //if it starts with "!", remove it and set the forced denial value.
        //we use this flag to indicate to add in a "!" again when we set the perm.
        //This will have the effect of causing the logic to explicitly deny permissions for players that do not match.
        if (recipientName.startsWith("!")) {
            isForcedDenial = true;
            recipientName = recipientName.substring(1); //remove the exclamation for the rest of the parsing.
        }

        if (recipientName.startsWith("[") && recipientName.endsWith("]")) {
            permission = recipientName.substring(1, recipientName.length() - 1);
            if (permission == null || permission.isEmpty()) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.InvalidPermissionID);
                return true;
            }
        } else if (recipientName.contains(".")) {
            permission = recipientName;
        } else {
            otherPlayer = plugin.resolvePlayer(recipientName);
            //addition: if it starts with G:, it indicates a group name, rather than a player name.

            if (otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all") &&
                    !recipientName.toUpperCase().startsWith("G:")) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.PlayerNotFound);
                return true;
            } else if (recipientName.toUpperCase().startsWith("G:")) {
                //keep it as is.
                //we will give trust to that group, that is...
            } else if (otherPlayer != null) {
                recipientName = otherPlayer.getName();
            } else {
                recipientName = "public";
            }
        }

        //determine which claims should be modified
        ArrayList<Claim> targetClaims = new ArrayList<Claim>();
        if (claim == null) {
            PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
            for (Claim c: playerData.getClaims()) {
                targetClaims.add(c);
            }
        } else {
            //check permission here
            if (claim.allowGrantPermission(player) != null) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.NoPermissionTrust, claim.getFriendlyOwnerName());
                return true;
            }

            //see if the player has the level of permission he's trying to grant
            String errorMessage = null;

            //permission level null indicates granting permission trust
            if (permissionLevel == null) {
                errorMessage = claim.allowEdit(player);
                if (errorMessage != null) {
                    errorMessage = "Only " + claim.getFriendlyOwnerName() + " can grant /PermissionTrust here.";
                }
            }

            //otherwise just use the ClaimPermission enum values
            else {
                switch (permissionLevel) {
                    case ACCESS:
                        errorMessage = claim.allowAccess(player);
                        break;
                    case INVENTORY:
                        errorMessage = claim.allowContainers(player);
                        break;
                    default:
                        errorMessage = claim.allowBuild(player);
                }
            }

            //error message for trying to grant a permission the player doesn't have
            if (errorMessage != null) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.CantGrantThatPermission);
                return true;
            }

            targetClaims.add(claim);
        }

        //if we didn't determine which claims to modify, tell the player to be specific
        if (targetClaims.size() == 0) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.GrantPermissionNoClaim);
            return true;
        }
        //if forcedenial is true, we will add the exclamation back to the name for addition.
        if (isForcedDenial) recipientName = "!" + recipientName;
        //apply changes
        for (Claim currentClaim : targetClaims) {
            if (permissionLevel == null) {
                if (!currentClaim.isManager(recipientName)) {
                    currentClaim.addManager(recipientName);
                }
            } else {
                currentClaim.setPermission(recipientName, permissionLevel);
            }
            plugin.getDataStore().saveClaim(currentClaim);
        }

        //notify player
        if (recipientName.equals("public")) recipientName = plugin.getMessageManager().getMessage(Messages.CollectivePublic);
        String permissionDescription;
        if (permissionLevel == null) {
            permissionDescription = plugin.getMessageManager().getMessage(Messages.PermissionsPermission);
        } else if (permissionLevel == ClaimPermission.BUILD) {
            permissionDescription = plugin.getMessageManager().getMessage(Messages.BuildPermission);
        } else if (permissionLevel == ClaimPermission.ACCESS) {
            permissionDescription = plugin.getMessageManager().getMessage(Messages.AccessPermission);
        } else //ClaimPermission.Inventory
        {
            permissionDescription = plugin.getMessageManager().getMessage(Messages.ContainersPermission);
        }

        String location;
        if (claim == null) {
            location = plugin.getMessageManager().getMessage(Messages.LocationAllClaims);
        } else {
            location = plugin.getMessageManager().getMessage(Messages.LocationCurrentClaim);
        }
        String userecipientName = recipientName;
        if (userecipientName.toUpperCase().startsWith("G:")) {
            userecipientName = "Group " + userecipientName.substring(2);
        }
        plugin.sendMessage(player, TextMode.SUCCESS, Messages.GrantPermissionConfirmation, userecipientName, permissionDescription, location);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
