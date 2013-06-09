package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.*;
import me.ryanhamshire.GriefPrevention.configuration.Messages;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: andre
 * Date: 6/8/13
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestoreNature extends BaseCommand {

    public RestoreNature(GriefPrevention plugin) {
        super(plugin, "restorenature");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        //change shovel mode
        if (!(sender instanceof Player)) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }

        ShovelMode newMode = ShovelMode.ADMIN.RESTORE_NATURE;

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
        PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());

        switch (newMode) {
            case RESTORE_NATURE:
                GriefPrevention.sendMessage(player, TextMode.INSTR, Messages.RestoreNatureActivate);
                break;
            case RESTORE_NATURE_AGGRESSIVE:
                GriefPrevention.sendMessage(player, TextMode.WARN, Messages.RestoreNatureAggressiveActivate);
                break;
            case RESTORE_NATURE_FILL:
                //set radius based on arguments
                playerData.setFillRadius(2);
                if (args.size() > 0) {
                    try {
                        playerData.setFillRadius(Integer.parseInt(args.peek()));
                    } catch (Exception exception) {
                    }
                }
                if (playerData.getFillRadius() < 0) playerData.setFillRadius(2);
                GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.FillModeActive, String.valueOf(playerData.getFillRadius()));
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
