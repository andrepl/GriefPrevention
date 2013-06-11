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

import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.data.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

//FEATURE: give players claim blocks for playing, as long as they're not away from their computer

//runs every 5 minutes in the main thread, grants blocks per hour / 12 to each online player who appears to be actively playing
public class DeliverClaimBlocksTask implements Runnable {
    
    private GriefPrevention plugin;

    public DeliverClaimBlocksTask(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Player[] players = plugin.getServer().getOnlinePlayers();

        //ensure players get at least 1 block (if accrual is totally disabled, this task won't even be scheduled)
        //BC: refactored, now it calculates the blocks that have been accrued on a per-Player basis.
        //for each online player
        for (int i = 0; i < players.length; i++) {
            Player player = players[i];
            WorldConfig wc = plugin.getWorldCfg(player.getWorld());

            int accruedBlocks = Math.max(1, (int)(wc.getClaimBlocksAccruedPerHour() / 12));

            DataStore dataStore = plugin.dataStore;
            PlayerData playerData = dataStore.getPlayerData(player.getName());

            Location lastLocation = playerData.getLastAfkCheckLocation();
            try  //distance squared will throw an exception if the player has changed worlds
            {
                //if he's not in a vehicle and has moved at least three blocks since the last check
                //and he's not being pushed around by fluids
                if (!player.isInsideVehicle() &&
                        (lastLocation == null || lastLocation.distanceSquared(player.getLocation()) >= 9) &&
                        !player.getLocation().getBlock().isLiquid()) {
                    //if player is over accrued limit, accrued limit was probably reduced in config file AFTER he accrued
                    //in that case, leave his blocks where they are
                    if (playerData.getAccruedClaimBlocks() > plugin.configuration.getMaxAccruedBlocks()) {
                        continue;
                    }

                    //add blocks
                    playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() + accruedBlocks);

                    //respect limits
                    if (playerData.getAccruedClaimBlocks() > plugin.configuration.getMaxAccruedBlocks()) {
                        playerData.setAccruedClaimBlocks(plugin.configuration.getMaxAccruedBlocks());
                    }

                    //intentionally NOT saving data here to reduce overall secondary storage access frequency
                    //many other operations will cause this players data to save, including his eventual logout
                    //dataStore.writePlayerToStorage(player.getName(), playerData);
                }
            } catch (Exception e) {
            }

            //remember current location for next time
            playerData.setLastAfkCheckLocation(player.getLocation());
        }
    }
}