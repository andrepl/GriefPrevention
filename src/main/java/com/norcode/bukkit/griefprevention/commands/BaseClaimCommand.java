package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.data.Claim;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;

public abstract class BaseClaimCommand extends BaseCommand {

    private Messages noClaimMessage = Messages.ClaimMissing;
    protected boolean ignoreHeight = true;

    public BaseClaimCommand(GriefPreventionTNG plugin, String cmdName, Messages message) {
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
        Claim claim = plugin.getDataStore().getClaimAt(player.getLocation(), this.ignoreHeight, null);
        if (claim == null) {
            plugin.sendMessage(player, TextMode.ERROR, this.noClaimMessage);
            return true;
        }
        return onCommand(player, claim, cmd, label, args);
    }

    public abstract boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args);

}
