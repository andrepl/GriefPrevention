package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class Siege extends BaseCommand {

    public Siege(GriefPrevention plugin) {
        super(plugin, "siege");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        if (!(sender instanceof Player)) {
            GriefPrevention.sendMessage(sender, TextMode.ERROR, Messages.CommandRequiresPlayer);
            return true;
        }
        Player player = (Player) sender;

        //error message for when siege mode is disabled
        if (!plugin.siegeEnabledForWorld(player.getWorld())) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NonSiegeWorld);
            return true;
        }

        //requires one argument
        if (args.size() > 1) {
            return false;
        }

        //can't start a siege when you're already involved in one
        Player attacker = player;
        PlayerData attackerData = plugin.dataStore.getPlayerData(attacker.getName());
        if (attackerData.getSiegeData() != null) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.AlreadySieging);
            return true;
        }

        //can't start a siege when you're protected from pvp combat
        if (attackerData.isPvpImmune()) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.CantFightWhileImmune);
            return true;
        }

        //if a player name was specified, use that
        Player defender = null;
        if (args.size() >= 1) {
            defender = plugin.getServer().getPlayer(args.peek());
            if (defender == null) {
                GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.PlayerNotFound);
                return true;
            }
        }

        //otherwise use the last player this player was in pvp combat with
        else if (attackerData.getLastPvpPlayer().length() > 0) {
            defender = plugin.getServer().getPlayer(attackerData.getLastPvpPlayer());
            if (defender == null) {
                return false;
            }
        } else {
            return false;
        }

        //victim must not be under siege already
        PlayerData defenderData = plugin.dataStore.getPlayerData(defender.getName());
        if (defenderData.getSiegeData() != null) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.AlreadyUnderSiegePlayer);
            return true;
        }

        //victim must not be pvp immune
        if (defenderData.isPvpImmune()) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoSiegeDefenseless);
            return true;
        }

        Claim defenderClaim = plugin.dataStore.getClaimAt(defender.getLocation(), false, null);

        //defender must have some level of permission there to be protected
        if (defenderClaim == null || defenderClaim.allowAccess(defender) != null) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NotSiegableThere);
            return true;
        }

        //attacker must be close to the claim he wants to siege
        if (!defenderClaim.isNear(attacker.getLocation(), 25)) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.SiegeTooFarAway);
            return true;
        }

        //claim can't be under siege already
        if (defenderClaim.siegeData != null) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.AlreadyUnderSiegeArea);
            return true;
        }

        //can't siege admin claims
        if (defenderClaim.isAdminClaim()) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.NoSiegeAdminClaim);
            return true;
        }

        //can't be on cooldown
        if (plugin.dataStore.onCooldown(attacker, defender, defenderClaim)) {
            GriefPrevention.sendMessage(player, TextMode.ERROR, Messages.SiegeOnCooldown);
            return true;
        }

        //start the siege
        if (plugin.dataStore.startSiege(attacker, defender, defenderClaim)) {

            //confirmation message for attacker, warning message for defender
            GriefPrevention.sendMessage(defender, TextMode.WARN, Messages.SiegeAlert, attacker.getName());
            GriefPrevention.sendMessage(player, TextMode.SUCCESS, Messages.SiegeConfirmed, defender.getName());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, LinkedList<String> args) {
        return null;
    }
}
