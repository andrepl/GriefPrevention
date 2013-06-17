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
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.*;

// FEATURE: automatically remove claims owned by inactive players which:
// ...aren't protecting much OR
// ...are a free new player claim (and the player has no other claims) OR
// ...because the player has been gone a REALLY long time, and that expiration has been configured in config.yml

// runs every 1 minute in the main thread
public class CleanupUnusedClaimsTask implements Runnable {

    GriefPrevention plugin;
    LinkedList<UUID> claimIds = new LinkedList<UUID>();
    public CleanupUnusedClaimsTask() {}

    public CleanupUnusedClaimsTask(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getDataStore().claimCount() == 0) return;
        if (this.claimIds.isEmpty()) {
            // Get a list of top level claim id's and shuffle it.
            claimIds = new LinkedList<UUID>(Arrays.asList(plugin.getDataStore().getTopLevelClaimIDs()));
            Collections.shuffle(claimIds);
            return;
        }
        Claim claim = plugin.getDataStore().getClaim(claimIds.pop());
        if (claim == null) {
            return;
        }
        // skip administrative claims
        if (claim.isAdminClaim()) return;
        WorldConfig wc = plugin.getWorldCfg(claim.getMin().getWorld());
        // track whether we do any important work which would require cleanup afterward
        boolean cleanupChunks = false;

        // get data for the player, especially last login timestamp
        PlayerData playerData = plugin.getDataStore().getPlayerData(claim.getOwnerName());

        // determine area of the default chest claim
        int areaOfDefaultClaim = 0;
        if (wc.getAutomaticClaimsForNewPlayerRadius() >= 0) {
            areaOfDefaultClaim = (int) Math.pow(wc.getAutomaticClaimsForNewPlayerRadius() * 2 + 1, 2);
        }

        // if he's been gone at least a week, if he has ONLY the new player claim, it will be removed
        Calendar sevenDaysAgo = Calendar.getInstance();
        sevenDaysAgo.add(Calendar.DATE, -wc.getChestClaimExpirationDays());
        boolean newPlayerClaimsExpired = sevenDaysAgo.getTime().after(playerData.getLastLogin());

        // if only one claim, and the player hasn't played in a week
        if (newPlayerClaimsExpired && playerData.getClaims().size() == 1) {
            // if that's a chest claim and those are set to expire
            if (claim.getArea() <= areaOfDefaultClaim && wc.getChestClaimExpirationDays() > 0) {
                claim.removeSurfaceFluids(null);
                plugin.getDataStore().deleteClaim(claim, null, true);
                cleanupChunks = true;

                // if configured to do so, restore the land to natural
                if (wc.getClaimsAutoNatureRestoration()) {
                    plugin.restoreClaim(claim, 0);
                }

                plugin.getLogger().info(" " + claim.getOwnerName() + "'s new player claim expired.");
            }
        }

        // if configured to always remove claims after some inactivity period without exceptions...
        else if (wc.getClaimsExpirationDays() > 0) {
            Calendar earliestPermissibleLastLogin = Calendar.getInstance();
            earliestPermissibleLastLogin.add(Calendar.DATE, -wc.getClaimsExpirationDays());

            if (earliestPermissibleLastLogin.getTime().after(playerData.getLastLogin())) {
                // make a copy of this player's claim list
                Vector<Claim> claims = new Vector<Claim>();
                for (Claim c: playerData.getClaims()) {
                    claims.add(c);
                }

                // delete them
                plugin.getDataStore().deleteClaimsForPlayer(claim.getOwnerName(), true, false);
                plugin.getLogger().info(" All of " + claim.getOwnerName() + "'s claims have expired. Removing all but the locked claims.");

                for (Claim claim1 : claims) {
                    // if configured to do so, restore the land to natural
                    if (wc.getClaimsAutoNatureRestoration()) {
                        plugin.restoreClaim(claim1, 0);
                        cleanupChunks = true;
                    }
                }
            }
        } else if (wc.getUnusedClaimExpirationDays() > 0) {
            // if the player has been gone two weeks, scan claim content to assess player investment
            Calendar earliestAllowedLoginDate = Calendar.getInstance();
            earliestAllowedLoginDate.add(Calendar.DATE, -wc.getUnusedClaimExpirationDays());
            boolean needsInvestmentScan = earliestAllowedLoginDate.getTime().after(playerData.getLastLogin());
            boolean creativerules = plugin.creativeRulesApply(claim.getMin());
            boolean sizelimitreached = (creativerules && claim.getWidth() > wc.getClaimCleanupMaximumSize());


            // avoid scanning large claims, locked claims, and administrative claims
            if (claim.isAdminClaim() || claim.isNeverDelete() || sizelimitreached) return;

            // if creative mode or the claim owner has been away a long enough time, scan the claim content
            if (needsInvestmentScan || creativerules) {
                int minInvestment;
                minInvestment = wc.getClaimCleanupMaxInvestmentScore();
                // if minInvestment is 0, assume no limitation and force the following conditions to clear the claim.
                long investmentScore = minInvestment == 0 ? Long.MAX_VALUE : claim.getPlayerInvestmentScore();
                cleanupChunks = true;
                boolean removeClaim = false;

                // in creative mode, a build which is almost entirely lava above sea level will be automatically removed, even if the owner is an active player
                // lava above the surface deducts 10 points per block from the investment score
                // so 500 blocks of lava without anything built to offset all that potential mess would be cleaned up automatically
                if (plugin.creativeRulesApply(claim.getMin()) && investmentScore < -5000) {
                    removeClaim = true;
                }

                // otherwise, the only way to get a claim automatically removed based on build investment is to be away for two weeks AND not build much of anything
                else if (needsInvestmentScan && investmentScore < minInvestment) {
                    removeClaim = true;
                }

                if (removeClaim) {
                    plugin.getDataStore().deleteClaim(claim, null, true);
                    plugin.getLogger().info("Removed " + claim.getOwnerName() + "'s unused claim @ " + GriefPrevention.getfriendlyLocationString(claim.getMin()));

                    // if configured to do so, restore the claim area to natural state
                    if (wc.getClaimsAutoNatureRestoration()) {
                        plugin.restoreClaim(claim, 0);
                    }
                }
            }
        }

        // since we're potentially loading a lot of chunks to scan parts of the world where there are no players currently playing, be mindful of memory usage
        if (cleanupChunks) {
            World world = claim.getMin().getWorld();
            Chunk[] chunks = world.getLoadedChunks();
            for (Chunk chunk : chunks) {
                chunk.unload(true, true);
            }
        }
    }
}
