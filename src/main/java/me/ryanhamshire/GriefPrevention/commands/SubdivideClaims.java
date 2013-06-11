package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.*;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class SubdivideClaims extends BaseCommand {

    public SubdivideClaims(GriefPrevention plugin) {
        super(plugin, "subdivideclaims");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }
        Player player = (Player) sender;
        PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
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
