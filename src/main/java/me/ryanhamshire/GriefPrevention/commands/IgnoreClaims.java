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
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer, null);
            return true;
        }
        Player player = (Player) sender;

        PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
        playerData.setIgnoreClaims(!playerData.isIgnoreClaims());

        //toggle ignore claims mode on or off
        if (!playerData.isIgnoreClaims()) {
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.RespectingClaims);
        } else {
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.IgnoringClaims);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
