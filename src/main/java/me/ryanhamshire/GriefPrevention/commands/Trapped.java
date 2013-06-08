package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.*;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.tasks.PlayerRescueTask;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: andre
 * Date: 6/8/13
 * Time: 6:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Trapped extends BaseClaimCommand {

    public Trapped(GriefPrevention plugin) {
        super(plugin, "trapped", Messages.NotTrappedHere);
        this.ignoreHeight = false;
    }

    @Override
    public boolean onCommand(Player player, Claim claim, Command cmd, String label, LinkedList<String> args) {

        PlayerData playerData = plugin.dataStore.getPlayerData(player.getName());
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        //if another /trapped is pending, ignore this slash command
        if (playerData.pendingTrapped) {
            return true;
        }

        //if the player isn't in a claim or has permission to build, tell him to man up
        if (claim == null || claim.allowBuild(player) == null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotTrappedHere);
            return true;
        }

        //if the player is in the nether or end, he's screwed (there's no way to programmatically find a safe place for him)
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
            return true;
        }

        //if the player is in an administrative claim, he should contact an admin
        if (claim.isAdminClaim()) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
            return true;
        }

        //check cooldown
        long lastTrappedUsage = playerData.lastTrappedUsage.getTime();
        long nextTrappedUsage = lastTrappedUsage + 1000 * 60 * 60 * wc.getClaimsTrappedCooldownHours();
        long now = Calendar.getInstance().getTimeInMillis();
        if (now < nextTrappedUsage) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedOnCooldown, String.valueOf(wc.getClaimsTrappedCooldownHours()), String.valueOf((nextTrappedUsage - now) / (1000 * 60) + 1));
            return true;
        }
        //send instructions
        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RescuePending);
        //create a task to rescue this player in a little while
        PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation());
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, 200L);  //20L ~ 1 second

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
