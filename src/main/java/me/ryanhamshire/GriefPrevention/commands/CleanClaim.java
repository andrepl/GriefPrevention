package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.*;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: andre
 * Date: 6/8/13
 * Time: 3:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class CleanClaim extends BaseClaimCommand {
    public CleanClaim(GriefPrevention plugin) {
        super(plugin, "cleanclaim", Messages.ClaimMissing);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        //source is first arg; target is second arg.
        if (args.size() == 0) {
            return false;
        }

        MaterialInfo source = MaterialInfo.fromString(args.peek());
        if (source == null) {
            Material attemptparse = Material.valueOf(args.peek());
            if (attemptparse != null) {
                source = new MaterialInfo(attemptparse.getId(), (byte) 0, args.peek());
            } else {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.UnknownMaterial, args.peek());
                return true;
            }
        }
        args.pop();

        MaterialInfo target = new MaterialInfo(Material.AIR.getId(), (byte) 0, "Air");
        if (args.size() > 0) {
            target = MaterialInfo.fromString(args.peek());
            if (target == null) {
                Material attemptparse = Material.valueOf(args.peek());
                if (attemptparse != null) {
                    target = new MaterialInfo(attemptparse.getId(), (byte) 0, args.peek());
                } else {
                    GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.UnknownMaterial, args.peek());
                    return true;
                }
            }
        }
        args.pop();

        PlayerData pd = plugin.dataStore.getPlayerData(player.getName());
        if (pd.isIgnoreClaims() || claim.getOwnerName().equalsIgnoreCase(player.getName())) {
            plugin.handleClaimClean(claim, source, target, player);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
