package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.ShovelMode;
import com.norcode.bukkit.griefprevention.data.PlayerData;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class SubdivideClaims extends BaseCommand {

    public SubdivideClaims(GriefPreventionTNG plugin) {
        super(plugin, "subdivideclaims");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }
        Player player = (Player) sender;
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        playerData.setShovelMode(ShovelMode.SUBDIVIDE);
        playerData.setClaimSubdividing(null);
        plugin.sendMessage(player, TextMode.INSTR, Messages.SubdivisionMode);
        plugin.sendMessage(player, TextMode.INSTR, Messages.SubdivisionDemo);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
