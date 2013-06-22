package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.data.Claim;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class LockClaim extends BaseClaimCommand {
    public LockClaim(GriefPreventionTNG plugin) {
        super(plugin, "lockclaim", Messages.ClaimMissing);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        if (args.size() != 0) return false;

        if ((player.hasPermission("griefprevention.lock") && claim.getFriendlyOwnerName().equalsIgnoreCase(player.getName())) || player.hasPermission("griefprevention.adminlock")) {
            claim.setNeverDelete(true);
            plugin.getDataStore().saveClaim(claim);
            plugin.sendMessage(player, TextMode.SUCCESS, Messages.ClaimLocked);
        } else {
            plugin.sendMessage(player, TextMode.ERROR, Messages.OnlyOwnersModifyClaims);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
