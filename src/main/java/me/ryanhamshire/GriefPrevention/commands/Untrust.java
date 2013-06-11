package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class Untrust extends BaseCommand {

    public Untrust(GriefPrevention plugin) {
        super(plugin, "untrust");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        //requires exactly one parameter, the other player's name
        if (args.size() != 1) return false;
        String target = args.pop();

        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }
        Player player = (Player) sender;
        //determine which claim the player is standing in
        Claim claim = plugin.getDataStore().getClaimAt(player.getLocation(), true /*ignore height*/, null);

        //bracket any permissions
        if (target.contains(".")) {
            target = "[" + target + "]";
        }

        //determine whether a single player or clearing permissions entirely
        boolean clearPermissions = false;
        OfflinePlayer otherPlayer = null;
        System.out.println("clearing perms for name:" + target);
        if (target.equals("all")) {
            if (claim == null || claim.allowEdit(player) == null) {
                clearPermissions = true;
            } else {
                plugin.sendMessage(player, TextMode.ERROR, Messages.ClearPermsOwnerOnly);
                return true;
            }
        } else if ((!target.startsWith("[") || !target.endsWith("]"))
                && !target.toUpperCase().startsWith("G:") && !target.startsWith("!")) {
            otherPlayer = plugin.resolvePlayer(target);
            if (otherPlayer == null && !target.equals("public")) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.PlayerNotFound);
                return true;
            }

            //correct to proper casing
            if (otherPlayer != null)
                target = otherPlayer.getName();
        } else if (target.startsWith("G:")) {
            //make sure the group exists, otherwise show the message.
            String groupname = target.substring(2);
            if (!plugin.configuration.getPlayerGroups().groupExists(groupname)) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.GroupNotFound);
                return true;
            }
        }

        //if no claim here, apply changes to all his claims
        if (claim == null) {
            PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
            for (int i = 0; i < playerData.getClaims().size(); i++) {
                claim = playerData.getClaims().get(i);
                //if untrusting "all" drop all permissions
                if (clearPermissions) {
                    claim.clearPermissions();
                } else { //otherwise drop individual permissions
                    claim.dropPermission(target);
                    claim.removeManager(target);
                    //claim.managers.remove(target);
                }
                //save changes
                plugin.getDataStore().saveClaim(claim);
            }
            //beautify for output
            if (target.equals("public")) {
                target = "the public";
            }
            //confirmation message
            if (!clearPermissions) {
                plugin.sendMessage(player, TextMode.SUCCESS, Messages.UntrustIndividualAllClaims, target);
            } else {
                plugin.sendMessage(player, TextMode.SUCCESS, Messages.UntrustEveryoneAllClaims);
            }
        }

        //otherwise, apply changes to only this claim
        else if (claim.allowGrantPermission(player) != null) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.NoPermissionTrust, claim.getOwnerName());
        } else {
            //if clearing all
            if (clearPermissions) {
                claim.clearPermissions();
                plugin.sendMessage(player, TextMode.SUCCESS, Messages.ClearPermissionsOneClaim);
            } else {
                //otherwise individual permission drop
                claim.dropPermission(target);
                if (claim.allowEdit(player) == null) {
                    claim.removeManager(target);
                    //beautify for output
                    if (target.equals("public")) {
                        target = "the public";
                    }
                    plugin.sendMessage(player, TextMode.SUCCESS, Messages.UntrustIndividualSingleClaim, target);
                } else {
                    plugin.sendMessage(player, TextMode.SUCCESS, Messages.UntrustOwnerOnly, claim.getOwnerName());
                }
            }
            //save changes
            plugin.getDataStore().saveClaim(claim);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        List<String> results = new LinkedList<String>();
        if (args.size() == 1) {
            if ("all".startsWith(args.peek().toLowerCase())) {
                results.add("all");
            }
            for (Player p: plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args.peek().toLowerCase())) {
                    results.add(p.getName());
                }
            }
        }
        return results;
    }
}
