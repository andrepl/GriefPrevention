package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.data.PlayerData;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;


public class IgnoreClaims extends BaseCommand {
    public IgnoreClaims(GriefPreventionTNG plugin) {
        super(plugin, "ignoreclaims");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }
        Player player = (Player) sender;

        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        playerData.setIgnoreClaims(!playerData.isIgnoreClaims());

        //toggle ignore claims mode on or off
        if (!playerData.isIgnoreClaims()) {
            plugin.sendMessage(player, TextMode.SUCCESS, Messages.RespectingClaims);
        } else {
            plugin.sendMessage(player, TextMode.SUCCESS, Messages.IgnoringClaims);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
