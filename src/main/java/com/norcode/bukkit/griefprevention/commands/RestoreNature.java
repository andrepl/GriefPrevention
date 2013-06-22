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

public class RestoreNature extends BaseCommand {

    public RestoreNature(GriefPreventionTNG plugin) {
        super(plugin, "restorenature");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        //change shovel mode
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }

        ShovelMode newMode = ShovelMode.RESTORE_NATURE;

        if (args.size() > 0) {
            if (args.peek().equalsIgnoreCase("aggressive")) {
                newMode = ShovelMode.RESTORE_NATURE_AGGRESSIVE;
            } else if (args.peek().equalsIgnoreCase("fill")) {
                newMode = ShovelMode.RESTORE_NATURE_FILL;
            } else {
                return false;
            }
            args.pop();
        }

        Player player = (Player) sender;
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());

        switch (newMode) {
            case RESTORE_NATURE:
                plugin.sendMessage(player, TextMode.INSTR, Messages.RestoreNatureActivate);
                break;
            case RESTORE_NATURE_AGGRESSIVE:
                plugin.sendMessage(player, TextMode.WARN, Messages.RestoreNatureAggressiveActivate);
                break;
            case RESTORE_NATURE_FILL:
                //set radius based on arguments
                playerData.setFillRadius(2);
                if (args.size() > 0) {
                    try {
                        playerData.setFillRadius(Integer.parseInt(args.peek()));
                    } catch (Exception exception) {
                        playerData.setFillRadius(2);
                    }
                }
                if (playerData.getFillRadius() < 0) playerData.setFillRadius(2);
                plugin.sendMessage(player, TextMode.SUCCESS, Messages.FillModeActive, String.valueOf(playerData.getFillRadius()));
                break;
        }
        playerData.setShovelMode(newMode);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        List<String> results = new LinkedList<String>();
        if (args.size() == 1) {
            if ("aggressive".startsWith(args.peek().toLowerCase())) results.add("aggressive");
            if ("fill".startsWith(args.peek().toLowerCase())) results.add("fill");
        }
        return results;
    }
}
