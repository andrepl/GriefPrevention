package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;

public abstract class BaseClaimCommand extends BaseCommand {

    private Messages noClaimMessage = Messages.ClaimMissing;
    protected boolean ignoreHeight = true;

    public BaseClaimCommand(GriefPrevention plugin, String cmdName, Messages message) {
        super(plugin, cmdName);
        noClaimMessage = message;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }
        Player player = (Player) sender;
        Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), this.ignoreHeight, null);
        if (claim == null) {
            plugin.sendMessage(player, TextMode.ERROR, this.noClaimMessage);
            return true;
        }
        return onCommand(player, claim, cmd, label, args);
    }

    public abstract boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args);

}
