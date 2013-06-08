package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;


public class IgnoreClaims extends BaseCommand {
    public IgnoreClaims(GriefPrevention plugin) {
        super(plugin, "ignoreclaims");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (!(sender instanceof Player)) {
            GriefPrevention.sendMessage(sender, TextMode.Err, Messages.CommandRequiresPlayer, null);
            return true;
        }
        Player player = (Player) sender;

        PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
        playerData.ignoreClaims = !playerData.ignoreClaims;

        //toggle ignore claims mode on or off
        if (!playerData.ignoreClaims) {
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.RespectingClaims);
        } else {
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.IgnoringClaims);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
