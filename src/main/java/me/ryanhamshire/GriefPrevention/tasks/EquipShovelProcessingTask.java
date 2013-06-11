/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention.tasks;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.ShovelMode;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.entity.Player;

// tells a player about how many claim blocks he has, etc
// implemented as a task so that it can be delayed
// otherwise, it's spammy when players mouse-wheel past the shovel in their hot bars
public class EquipShovelProcessingTask implements Runnable {
    // player data
    private Player player;
    GriefPrevention plugin;

    public EquipShovelProcessingTask(GriefPrevention plugin, Player player) {
        this.player = player;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // if he logged out, don't do anything
        if (!player.isOnline()) return;
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        // if he's not holding the golden shovel anymore, do nothing
        if (player.getItemInHand().getType() != wc.getClaimsModificationTool()) return;

        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());

        int remainingBlocks = playerData.getRemainingClaimBlocks();

        // if in basic claims mode...
        if (playerData.getShovelMode() == ShovelMode.BASIC) {
            // tell him how many claim blocks he has available
            plugin.sendMessage(player, TextMode.INSTR, Messages.RemainingBlocks, String.valueOf(remainingBlocks));

            // link to a video demo of land claiming, based on world type
            if (plugin.creativeRulesApply(player.getLocation())) {
                plugin.sendMessage(player, TextMode.INSTR, Messages.CreativeBasicsDemoAdvertisement);
            } else {
                plugin.sendMessage(player, TextMode.INSTR, Messages.SurvivalBasicsDemoAdvertisement);
            }
        }
    }
}
