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

public class UnlockClaim extends BaseClaimCommand {

    public UnlockClaim(GriefPrevention plugin) {
        super(plugin, "unlockclaim", Messages.ClaimMissing);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        if (args.size() != 0) {
            GriefPrevention.debug("too many args?");
            return false;
        }
        if ((player.hasPermission("griefprevention.lock") && claim.getOwnerName().equalsIgnoreCase(player.getName())) || player.hasPermission("griefprevention.adminlock")) {
            claim.setNeverDelete(false);
            plugin.dataStore.saveClaim(claim);
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.ClaimUnlocked);
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
