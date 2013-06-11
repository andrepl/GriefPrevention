package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import me.ryanhamshire.GriefPrevention.tasks.PlayerRescueTask;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class Trapped extends BaseClaimCommand {

    public Trapped(GriefPrevention plugin) {
        super(plugin, "trapped", Messages.NotTrappedHere);
        this.ignoreHeight = false;
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {

        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        //if another /trapped is pending, ignore this slash command
        if (playerData.isPendingTrapped()) {
            return true;
        }

        //if the player isn't in a claim or has permission to build, tell him to man up
        if (claim == null || claim.allowBuild(player) == null) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.NotTrappedHere);
            return true;
        }

        //if the player is in the nether or end, he's screwed (there's no way to programmatically find a safe place for him)
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.TrappedWontWorkHere);
            return true;
        }

        //if the player is in an administrative claim, he should contact an admin
        if (claim.isAdminClaim()) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.TrappedWontWorkHere);
            return true;
        }

        //check cooldown
        long lastTrappedUsage = playerData.getLastTrappedUsage().getTime();
        long nextTrappedUsage = lastTrappedUsage + 1000 * 60 * 60 * wc.getClaimsTrappedCooldownHours();
        long now = Calendar.getInstance().getTimeInMillis();
        if (now < nextTrappedUsage) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.TrappedOnCooldown, String.valueOf(wc.getClaimsTrappedCooldownHours()), String.valueOf((nextTrappedUsage - now) / (1000 * 60) + 1));
            return true;
        }
        //send instructions
        plugin.sendMessage(player, TextMode.INSTR, Messages.RescuePending);
        //create a task to rescue this player in a little while
        PlayerRescueTask task = new PlayerRescueTask(plugin, player, player.getLocation());
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, 200L);  //20L ~ 1 second

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
