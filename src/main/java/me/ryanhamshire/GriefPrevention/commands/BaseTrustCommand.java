package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.*;
import me.ryanhamshire.GriefPrevention.configuration.Messages;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.ClaimPermission;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BaseTrustCommand extends BaseClaimCommand {

    ClaimPermission permissionLevel;

    public BaseTrustCommand(GriefPrevention plugin, String cmdName, ClaimPermission permissionLevel) {
        super(plugin, cmdName, Messages.ClaimMissing);
        this.permissionLevel = permissionLevel;
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        if (args.size() == 0) {
            return false;
        }

        String permission = null;
        OfflinePlayer otherPlayer = null;
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
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.InvalidPermissionID);
                return true;
            }
        } else if (recipientName.contains(".")) {
            permission = recipientName;
        } else {
            otherPlayer = plugin.resolvePlayer(recipientName);
            //addition: if it starts with G:, it indicates a group name, rather than a player name.

            if (otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all") &&
                    !recipientName.toUpperCase().startsWith("G:")) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.PlayerNotFound);
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
            PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
            for (int i = 0; i < playerData.getClaims().size(); i++) {
                targetClaims.add(playerData.getClaims().get(i));
            }
        } else {
            //check permission here
            if (claim.allowGrantPermission(player) != null) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoPermissionTrust, claim.getOwnerName());
                return true;
            }

            //see if the player has the level of permission he's trying to grant
            String errorMessage = null;

            //permission level null indicates granting permission trust
            if (permissionLevel == null) {
                errorMessage = claim.allowEdit(player);
                if (errorMessage != null) {
                    errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here.";
                }
            }

            //otherwise just use the ClaimPermission enum values
            else {
                switch (permissionLevel) {
                    case Access:
                        errorMessage = claim.allowAccess(player);
                        break;
                    case Inventory:
                        errorMessage = claim.allowContainers(player);
                        break;
                    default:
                        errorMessage = claim.allowBuild(player);
                }
            }

            //error message for trying to grant a permission the player doesn't have
            if (errorMessage != null) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CantGrantThatPermission);
                return true;
            }

            targetClaims.add(claim);
        }

        //if we didn't determine which claims to modify, tell the player to be specific
        if (targetClaims.size() == 0) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.GrantPermissionNoClaim);
            return true;
        }
        //if forcedenial is true, we will add the exclamation back to the name for addition.
        if (isForcedDenial) recipientName = "!" + recipientName;
        //apply changes
        for (int i = 0; i < targetClaims.size(); i++) {
            Claim currentClaim = targetClaims.get(i);
            if (permissionLevel == null) {
                if (!currentClaim.isManager(recipientName)) {
                    currentClaim.addManager(recipientName);
                }
            } else {
                currentClaim.setPermission(recipientName, permissionLevel);
            }
            plugin.dataStore.saveClaim(currentClaim);
        }

        //notify player
        if (recipientName.equals("public")) recipientName = plugin.dataStore.getMessage(Messages.CollectivePublic);
        String permissionDescription;
        if (permissionLevel == null) {
            permissionDescription = plugin.dataStore.getMessage(Messages.PermissionsPermission);
        } else if (permissionLevel == ClaimPermission.Build) {
            permissionDescription = plugin.dataStore.getMessage(Messages.BuildPermission);
        } else if (permissionLevel == ClaimPermission.Access) {
            permissionDescription = plugin.dataStore.getMessage(Messages.AccessPermission);
        } else //ClaimPermission.Inventory
        {
            permissionDescription = plugin.dataStore.getMessage(Messages.ContainersPermission);
        }

        String location;
        if (claim == null) {

            location = plugin.dataStore.getMessage(Messages.LocationAllClaims);
        } else {
            location = plugin.dataStore.getMessage(Messages.LocationCurrentClaim);
        }
        String userecipientName = recipientName;
        if (userecipientName.toUpperCase().startsWith("G:")) {
            userecipientName = "Group " + userecipientName.substring(2);
        }
        GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
