package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class LockClaim extends BaseClaimCommand {
    public LockClaim(GriefPrevention plugin) {
        super(plugin, "lockclaim", Messages.ClaimMissing);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        if (args.size() != 0) return false;

        if ((player.hasPermission("griefprevention.lock") && claim.ownerName.equalsIgnoreCase(player.getName())) || player.hasPermission("griefprevention.adminlock")) {
            claim.neverdelete = true;
            plugin.dataStore.saveClaim(claim);
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.ClaimLocked);
        } else {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.OnlyOwnersModifyClaims);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
