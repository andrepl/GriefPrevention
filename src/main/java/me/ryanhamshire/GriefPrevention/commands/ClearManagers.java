package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class ClearManagers extends BaseClaimCommand {

    public ClearManagers(GriefPrevention plugin) {
        super(plugin, "clearmanagers", Messages.ClearManagersNotFound);
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {
        PlayerData pdata = plugin.getDataStore().getPlayerData(player.getName());
        if (claim != null) {
            if (claim.isAdminClaim()) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.ClearManagersNotAdmin);
                return true;
            }
            if (pdata.isIgnoreClaims() || claim.getOwnerName().equalsIgnoreCase(player.getName())) {
                for (String currmanager : claim.getManagerList()) {
                    claim.removeManager(currmanager);
                }
                plugin.sendMessage(player, TextMode.ERROR, Messages.ClearManagersSuccess);
            } else {
                plugin.sendMessage(player, TextMode.ERROR, Messages.ClearManagersNotOwned);
            }
        } else {
            plugin.sendMessage(player, TextMode.ERROR, Messages.ClearManagersNotFound);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
