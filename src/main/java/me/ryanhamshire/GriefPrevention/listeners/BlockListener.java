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

package me.ryanhamshire.GriefPrevention.listeners;

import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import me.ryanhamshire.GriefPrevention.tasks.TreeCleanupTask;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import me.ryanhamshire.GriefPrevention.visualization.VisualizationType;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

// event handlers related to blocks

/**
 * Listener class for Block-related Event handling.
 */
public class BlockListener implements Listener {
    // convenience reference to singleton datastore
    private GriefPrevention plugin;

    // how far away to search from a tree trunk for its branch blocks
    private static final int TREE_RADIUS = 5;

    // constructor
    public BlockListener(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    public String allowBreak(Player player, Location location, PlayerData playerData, Claim claim) {
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        // exception: administrators in ignore claims mode, and special player accounts created by server mods
        if (playerData.isIgnoreClaims() || wc.getModsIgnoreClaimsAccounts().contains(player.getName())) return null;

        // wilderness rules
        if (claim == null) {
            // no building in the wilderness in creative mode
            if (plugin.creativeRulesApply(location)) {
                String reason = plugin.getMessageManager().getMessage(Messages.NoBuildOutsideClaims) + "  " + plugin.getMessageManager().getMessage(Messages.CreativeBasicsDemoAdvertisement);
                if (player.hasPermission("griefprevention.ignoreclaims"))
                    reason += "  " + plugin.getMessageManager().getMessage(Messages.IgnoreClaimsAdvertisement);
                return reason;
            } else if (wc.getApplyTrashBlockRules() && wc.getClaimsEnabled()) {
                return plugin.getMessageManager().getMessage(Messages.NoBuildOutsideClaims) + "  " + plugin.getMessageManager().getMessage(Messages.SurvivalBasicsDemoAdvertisement);
            }

            // but it's fine in survival mode
            else {
                return null;
            }
        } else {
            // cache the claim for later reference
            playerData.setLastClaim(claim);

            // if not in the wilderness, then apply claim rules (permissions, etc)
            return claim.allowBreak(player, location.getBlock());
        }
    }


    public String allowBuild(Player player, Location location, PlayerData playerData, Claim claim) {
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        // exception: administrators in ignore claims mode and special player accounts created by server mods
        if (playerData.isIgnoreClaims() || wc.getModsIgnoreClaimsAccounts().contains(player.getName())) return null;
        if (claim == null) {
            claim = plugin.getDataStore().getClaimAt(location, true, playerData.getLastClaim());
        }
        // wilderness rules
        if (claim == null) {
            // no building in the wilderness in creative mode
            if (plugin.creativeRulesApply(location)) {
                String reason = plugin.getMessageManager().getMessage(Messages.NoBuildOutsideClaims) + "  " + plugin.getMessageManager().getMessage(Messages.CreativeBasicsDemoAdvertisement);
                if (player.hasPermission("griefprevention.ignoreclaims"))
                    reason += "  " + plugin.getMessageManager().getMessage(Messages.IgnoreClaimsAdvertisement);
                return reason;
            }

            // no building in survival wilderness when that is configured
            else if (wc.getApplyTrashBlockRules() && wc.getClaimsEnabled()) {
                if (wc.getTrashBlockPlacementBehaviour().allowed(location, player).denied())
                    return plugin.getMessageManager().getMessage(Messages.NoBuildOutsideClaims) + "  " + plugin.getMessageManager().getMessage(Messages.SurvivalBasicsDemoAdvertisement);
                else
                    return null;
            } else {
                // but it's fine in creative
                return null;
            }
        }

        // if not in the wilderness, then apply claim rules (permissions, etc)
        else {
            // cache the claim for later reference
            playerData.setLastClaim(claim);
            return claim.allowBuild(player);
        }
    }

    // when a block is damaged...
    @EventHandler(ignoreCancelled = true)
    public void onBlockDamaged(BlockDamageEvent event) {
        WorldConfig wc = plugin.getWorldCfg(event.getBlock().getLocation().getWorld());

        // if placing items in protected chests isn't enabled, none of this code needs to run
        if (!wc.getAddItemsToClaimedChests()) return;

        Block block = event.getBlock();
        Player player = event.getPlayer();

        // only care about player-damaged blocks
        if (player == null) return;

        // FEATURE: players may add items to a chest they don't have permission for by hitting it
        // if it's a chest
        if (block.getType() == Material.CHEST) {
            // only care about non-creative mode players, since those would outright break the box in one hit
            if (player.getGameMode() == GameMode.CREATIVE) return;

            // only care if the player has an itemstack in hand
            PlayerInventory playerInventory = player.getInventory();
            ItemStack stackInHand = playerInventory.getItemInHand();
            if (stackInHand == null || stackInHand.getType() == Material.AIR) return;

            // only care if the chest is in a claim, and the player does not have access to the chest
            Claim claim = plugin.getDataStore().getClaimAt(block.getLocation(), false, null);
            if (claim == null || claim.allowContainers(player) == null) return;

            PlayerData playerData = plugin.getDataStore().getPlayerData(event.getPlayer().getName());
            // if a player is in pvp combat, he can't give away items
            if (playerData.inPvpCombat()) return;

            // NOTE: to eliminate accidental give-aways, first hit on a chest displays a confirmation message
            // subsequent hits donate item to the chest

            // if first time damaging this chest, show confirmation message
            if (playerData.getLastChestDamageLocation() == null || !block.getLocation().equals(playerData.getLastChestDamageLocation())) {
                // remember this location
                playerData.setLastChestDamageLocation(block.getLocation());

                // give the player instructions
                plugin.sendMessage(player, TextMode.INSTR, Messages.DonateItemsInstruction);
            } else { // otherwise, try to donate the item stack in hand
                // look for empty slot in chest
                Chest chest = (Chest) block.getState();
                Inventory chestInventory = chest.getInventory();
                int availableSlot = chestInventory.firstEmpty();

                // if there isn't one
                if (availableSlot < 0) {
                    // tell the player and stop here
                    plugin.sendMessage(player, TextMode.ERROR, Messages.ChestFull);
                    return;
                }

                // otherwise, transfer item stack from player to chest
                // NOTE: Inventory.addItem() is smart enough to add items to existing stacks, making filling a chest with garbage as a grief very difficult
                chestInventory.addItem(stackInHand);
                playerInventory.setItemInHand(new ItemStack(Material.AIR));

                // and confirm for the player
                plugin.sendMessage(player, TextMode.SUCCESS, Messages.DonationSuccess);
            }
        }
    }

    // when a player breaks a block...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent breakEvent) {
        WorldConfig wc = plugin.getWorldCfg(breakEvent.getBlock().getWorld());
        Player player = breakEvent.getPlayer();
        Block block = breakEvent.getBlock();

        // if no survival building outside claims is enabled...
        // if the block is a trash block....
        if (wc.getTrashBlocks().contains(breakEvent.getBlock().getType())) {
            // and if this location is applicable for trash block placement...
            if (wc.getTrashBlockPlacementBehaviour().allowed(breakEvent.getBlock().getLocation(), player).allowed()) {
                return;
            }
        }

        // make sure the player is allowed to break at the location
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        Claim claim = plugin.getDataStore().getClaimAt(breakEvent.getBlock().getLocation(), false, playerData.getLastClaim());

        String noBuildReason = allowBreak(player, block.getLocation(), playerData, claim);
        if (noBuildReason != null) {
            plugin.sendMessage(player, TextMode.ERROR, noBuildReason);
            breakEvent.setCancelled(true);
            return;
        }
        claim = plugin.getDataStore().getClaimAt(block.getLocation(), true, claim);
        // if there's a claim here
        if (claim != null) {
            // if breaking UNDER the claim and the player has permission to build in the claim
            if (block.getY() < claim.getMin().getBlockY() && claim.allowBuild(player) == null) {
                // extend the claim downward beyond the breakage point
                plugin.getDataStore().extendClaim(claim, claim.getMin().getBlockY() - wc.getClaimsExtendIntoGroundDistance());

            }
        }

        // FEATURE: automatically clean up hanging treetops
        // if it's a log
        if (block.getType() == Material.LOG && wc.getRemoveFloatingTreetops()) {
            // run the specialized code for treetop removal (see below)
            handleLogBroken(block);
        }
    }

    // when a player places a sign...
    @EventHandler(ignoreCancelled = true)
    public void onSignChanged(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        WorldConfig wc = plugin.getWorldCfg(event.getPlayer().getWorld());
        StringBuilder lines = new StringBuilder();
        boolean notEmpty = false;
        for (String iterateLine : event.getLines()) {
            if (iterateLine.length() != 0) notEmpty = true;
            lines.append(iterateLine).append(";");
        }

        String signMessage = lines.toString();

        // if not empty and wasn't the same as the last sign, log it and remember it for later
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        if (notEmpty && playerData.getLastMessage() != null && !playerData.getLastMessage().equals(signMessage)) {
            plugin.getLogger().info("[Sign Placement] <" + player.getName() + "> " + lines.toString() + " @ " + GriefPrevention.getfriendlyLocationString(event.getBlock().getLocation()));
            playerData.setLastMessage(signMessage);

            if (!player.hasPermission("griefprevention.eavesdrop") && wc.getSignEavesdrop()) {
                Player[] players = plugin.getServer().getOnlinePlayers();
                for (Player otherPlayer : players) {
                    if (otherPlayer.hasPermission("griefprevention.eavesdrop")) {
                        otherPlayer.sendMessage(ChatColor.GRAY + player.getName() + "(sign): " + signMessage);
                    }
                }
            }
        }
    }

    // when a player places a block...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent placeEvent) {
        Player player = placeEvent.getPlayer();
        Block block = placeEvent.getBlock();
        WorldConfig wc = plugin.getWorldCfg(block.getWorld());
        if (wc.getApplyTrashBlockRules()) {
            // if set, then we only allow Trash Blocks to be placed, and only in the allowed places.
            Claim testclaim = plugin.getDataStore().getClaimAt(block.getLocation(), true, null);
            if (testclaim == null) {
                if (wc.getTrashBlockPlacementBehaviour().allowed(block.getLocation(), player).allowed()) {
                    if (wc.getTrashBlocks().contains(block.getType())) {
                        return;
                    }
                }
            }
        }

        // FEATURE: limit fire placement, to prevent PvP-by-fire
        // if placed block is fire and pvp is off, apply rules for proximity to other players
        if (block.getType() == Material.FIRE && !player.getWorld().getPVP() && !player.hasPermission("griefprevention.lava")) {
            List<Player> players = block.getWorld().getPlayers();
            for (Player otherPlayer : players) {
                Location location = otherPlayer.getLocation();
                if (!otherPlayer.equals(player) && location.distanceSquared(block.getLocation()) < 9) {
                    plugin.sendMessage(player, TextMode.ERROR, Messages.PlayerTooCloseForFire, otherPlayer.getName());
                    placeEvent.setCancelled(true);
                    return;
                }
            }
        }
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        Claim claim = plugin.getDataStore().getClaimAt(block.getLocation(), false, playerData.getLastClaim());
        // make sure the player is allowed to build at the location
        String noBuildReason = allowBuild(player, block.getLocation(), playerData, claim);
        if (noBuildReason != null) {
            plugin.sendMessage(player, TextMode.ERROR, noBuildReason);
            placeEvent.setCancelled(true);
            return;
        }

        // if the block is being placed within an existing claim
        claim = plugin.getDataStore().getClaimAt(block.getLocation(), true, claim);
        if (claim != null) {
            // warn about TNT not destroying claimed blocks
            if (block.getType() == Material.TNT && !claim.isExplosivesAllowed()) {
                plugin.sendMessage(player, TextMode.WARN, Messages.NoTNTDamageClaims);
                plugin.sendMessage(player, TextMode.INSTR, Messages.ClaimExplosivesAdvertisement);
            }

            // if the player has permission for the claim and he's placing UNDER the claim
            if (block.getY() < claim.getMin().getBlockY() && claim.allowBuild(player) == null) {
                // extend the claim downward
                plugin.getDataStore().extendClaim(claim, claim.getMin().getBlockY() - wc.getClaimsExtendIntoGroundDistance());
            }

            // reset the counter for warning the player when he places outside his claims
            playerData.setUnclaimedBlockPlacementsUntilWarning(1);
        } else if (block.getType() == Material.CHEST &&                     // otherwise if there's no claim, the player is placing a chest, and new player automatic claims are enabled
                wc.getAutomaticClaimsForNewPlayerRadius() > -1 &&
                plugin.claimsEnabledForWorld(block.getWorld())) {
            // FEATURE: automatically create a claim when a player who has no claims places a chest
            // if the chest is too deep underground, don't create the claim and explain why
            if (wc.getClaimsPreventTheft() && block.getY() < wc.getClaimsMaxDepth()) {
                plugin.sendMessage(player, TextMode.WARN, Messages.TooDeepToClaim);
                return;
            }
            int radius = wc.getAutomaticClaimsForNewPlayerRadius();
            // if the player doesn't have any claims yet, automatically create a claim centered at the chest
            if (playerData.getClaims().size() == 0) {
                // radius == 0 means protect ONLY the chest
                if (wc.getAutomaticClaimsForNewPlayerRadius() == 0) {
                    plugin.getDataStore().createClaim(block.getWorld(), block.getX(), block.getX(), block.getY(), block.getY(), block.getZ(), block.getZ(), player.getName(), null, null, false, null, player, true);
                    plugin.sendMessage(player, TextMode.SUCCESS, Messages.ChestClaimConfirmation);
                } else { // otherwise, create a claim in the area around the chest
                    // as long as the automatic claim overlaps another existing claim, shrink it
                    // note that since the player had permission to place the chest, at the very least, the automatic claim will include the chest
                    while (radius >= 0 && (plugin.getDataStore().createClaim(block.getWorld(),
                            block.getX() - radius, block.getX() + radius,
                            block.getY() - wc.getClaimsExtendIntoGroundDistance(), block.getY(),
                            block.getZ() - radius, block.getZ() + radius,
                            player.getName(),
                            null, null, false, null, player, true).succeeded != CreateClaimResult.Result.SUCCESS)) {
                        radius--;
                    }

                    // notify and explain to player
                    plugin.sendMessage(player, TextMode.SUCCESS, Messages.AutomaticClaimNotification);
                    // show the player the protected area
                    Claim newClaim = plugin.getDataStore().getClaimAt(block.getLocation(), false, null);
                    Visualization visualization = Visualization.FromClaim(newClaim, block.getY(), VisualizationType.CLAIM, player.getLocation());
                    Visualization.apply(plugin, player, visualization);
                }

                // instructions for using /trust
                plugin.sendMessage(player, TextMode.INSTR, Messages.TrustCommandAdvertisement);

                // unless special permission is required to create a claim with the shovel, educate the player about the shovel
                if (!wc.getCreateClaimRequiresPermission()) {
                    plugin.sendMessage(player, TextMode.INSTR, Messages.GoldenShovelAdvertisement);
                }
            }

            // check to see if this chest is in a claim, and warn when it isn't
            if (plugin.getWorldCfg(player.getWorld()).getClaimsPreventTheft() && plugin.getDataStore().getClaimAt(block.getLocation(), false, playerData.getLastClaim()) == null) {
                plugin.sendMessage(player, TextMode.WARN, Messages.UnprotectedChestWarning);
            }
        } else if (block.getType() == Material.SAPLING &&
                plugin.getWorldCfg(player.getWorld()).getBlockSkyTrees() &&
                plugin.claimsEnabledForWorld(player.getWorld())) {
            // FEATURE: limit wilderness tree planting to grass, or dirt with more blocks beneath it
            Block earthBlock = placeEvent.getBlockAgainst();
            if (earthBlock.getType() != Material.GRASS) {
                if (earthBlock.getRelative(BlockFace.DOWN).getType() == Material.AIR ||
                        earthBlock.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getType() == Material.AIR) {
                    placeEvent.setCancelled(true);
                }
            }
        } else if (wc.claims_warnOnBuildOutside() && !wc.getTrashBlocks().contains(block.getType()) && wc.getClaimsEnabled() && playerData.getClaims().size() > 0) {
            // FEATURE: warn players when they're placing non-trash blocks outside of their claimed areas
            playerData.setUnclaimedBlockPlacementsUntilWarning(playerData.getUnclaimedBlockPlacementsUntilWarning()-1);
            if (playerData.getUnclaimedBlockPlacementsUntilWarning() <= 0 && wc.getClaimsWildernessBlocksDelay() != 0) {
                plugin.sendMessage(player, TextMode.WARN, Messages.BuildingOutsideClaims);
                playerData.setUnclaimedBlockPlacementsUntilWarning(wc.getClaimsWildernessBlocksDelay());

                if (playerData.getLastClaim() != null && playerData.getLastClaim().allowBuild(player) == null) {
                    Visualization visualization = Visualization.FromClaim(playerData.getLastClaim(), block.getY(), VisualizationType.CLAIM, player.getLocation());
                    Visualization.apply(plugin, player, visualization);
                }
            }
        }

        // warn players when they place TNT above sea level, since it doesn't destroy blocks there

        // warn players if Explosions are not allowed at the position they place it.
        boolean TNTAllowed = wc.getTntExplosionBehaviour().allowed(block.getLocation(), null).allowed();

        if (!TNTAllowed && block.getType() == Material.TNT &&
                block.getWorld().getEnvironment() != Environment.NETHER &&
                block.getY() > plugin.getWorldCfg(block.getWorld()).getSeaLevelOverride() - 5) {
            plugin.sendMessage(player, TextMode.WARN, Messages.NoTNTDamageAboveSeaLevel);
        }
    }

    // blocks "pushing" other players' blocks around (pistons)
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();

        // if no blocks moving, then only check to make sure we're not pushing into a claim from outside
        // this avoids pistons breaking non-solids just inside a claim, like torches, doors, and touchplates
        if (blocks.size() == 0) {
            Block pistonBlock = event.getBlock();
            Block invadedBlock = pistonBlock.getRelative(event.getDirection());

            if (plugin.getDataStore().getClaimAt(pistonBlock.getLocation(), false, null) == null &&
                    plugin.getDataStore().getClaimAt(invadedBlock.getLocation(), false, null) != null) {
                event.setCancelled(true);
            }
            return;
        }

        // who owns the piston, if anyone?
        String pistonClaimOwnerName = "_";
        Claim claim = plugin.getDataStore().getClaimAt(event.getBlock().getLocation(), false, null);
        if (claim != null) pistonClaimOwnerName = claim.getOwnerName();

        // which blocks are being pushed?
        for (Block block : blocks) {
            // if ANY of the pushed blocks are owned by someone other than the piston owner, cancel the event
            claim = plugin.getDataStore().getClaimAt(block.getLocation(), false, null);
            if (claim != null && !claim.getOwnerName().equals(pistonClaimOwnerName)) {
                event.setCancelled(true);
                event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 0);
                event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(event.getBlock().getType()));
                event.getBlock().setType(Material.AIR);
                return;
            }
        }

        // which direction?  note we're ignoring vertical push
        int xchange = 0;
        int zchange = 0;

        Block piston = event.getBlock();
        Block firstBlock = blocks.get(0);

        if (firstBlock.getX() > piston.getX()) {
            xchange = 1;
        } else if (firstBlock.getX() < piston.getX()) {
            xchange = -1;
        } else if (firstBlock.getZ() > piston.getZ()) {
            zchange = 1;
        } else if (firstBlock.getZ() < piston.getZ()) {
            zchange = -1;
        }

        // if horizontal movement
        if (xchange != 0 || zchange != 0) {
            for (Block block : blocks) {
                Claim originalClaim = plugin.getDataStore().getClaimAt(block.getLocation(), false, null);
                String originalOwnerName = "";
                if (originalClaim != null) {
                    originalOwnerName = originalClaim.getOwnerName();
                }

                Claim newClaim = plugin.getDataStore().getClaimAt(block.getLocation().add(xchange, 0, zchange), false, null);
                String newOwnerName = "";
                if (newClaim != null) {
                    newOwnerName = newClaim.getOwnerName();
                }

                // if pushing this block will change ownership, cancel the event and take away the piston (for performance reasons)
                if (!newOwnerName.equals(originalOwnerName)) {
                    event.setCancelled(true);
                    event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 0);
                    event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(event.getBlock().getType()));
                    event.getBlock().setType(Material.AIR);
                    return;
                }

            }
        }
    }

    // blocks theft by pulling blocks out of a claim (again pistons)
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        // we only care about sticky pistons
        if (!event.isSticky()) return;

        // who owns the moving block, if anyone?
        String movingBlockOwnerName = "_";
        Claim movingBlockClaim = plugin.getDataStore().getClaimAt(event.getRetractLocation(), false, null);
        if (movingBlockClaim != null) movingBlockOwnerName = movingBlockClaim.getOwnerName();

        // who owns the piston, if anyone?
        String pistonOwnerName = "_";
        Location pistonLocation = event.getBlock().getLocation();
        Claim pistonClaim = plugin.getDataStore().getClaimAt(pistonLocation, false, null);
        if (pistonClaim != null) pistonOwnerName = pistonClaim.getOwnerName();

        // if there are owners for the blocks, they must be the same player
        // otherwise cancel the event
        if (!pistonOwnerName.equals(movingBlockOwnerName)) {
            event.setCancelled(true);
        }
    }

    // blocks are ignited ONLY by flint and steel (not by being near lava, open flames, etc), unless configured otherwise
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockIgnite(BlockIgniteEvent igniteEvent) {
        WorldConfig wc = plugin.getWorldCfg(igniteEvent.getBlock().getWorld());
        if (!wc.getFireSpreads() && igniteEvent.getCause() != IgniteCause.FLINT_AND_STEEL && igniteEvent.getCause() != IgniteCause.LIGHTNING) {
            igniteEvent.setCancelled(true);
        }
    }

    // fire doesn't spread unless configured to, but other blocks still do (mushrooms and vines, for example)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockSpread(BlockSpreadEvent spreadEvent) {
        WorldConfig wc = plugin.getWorldCfg(spreadEvent.getBlock().getWorld());
        if (spreadEvent.getSource().getType() != Material.FIRE) return;

        if (!wc.getFireSpreads()) {
            spreadEvent.setCancelled(true);

            Block underBlock = spreadEvent.getSource().getRelative(BlockFace.DOWN);
            if (underBlock.getType() != Material.NETHERRACK) {
                spreadEvent.getSource().setType(Material.AIR);
            }
            return;
        }

        // never spread into a claimed area, regardless of settings
        if (plugin.getDataStore().getClaimAt(spreadEvent.getBlock().getLocation(), false, null) != null) {
            spreadEvent.setCancelled(true);

            // if the source of the spread is not fire on netherrack, put out that source fire to save cpu cycles
            Block source = spreadEvent.getSource();
            if (source.getType() == Material.FIRE && source.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK) {
                source.setType(Material.AIR);
            }
        }
    }

    // blocks are not destroyed by fire, unless configured to do so
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBurn(BlockBurnEvent burnEvent) {
        WorldConfig wc = plugin.getWorldCfg(burnEvent.getBlock().getWorld().getName());
        if (!wc.getFireDestroys()) {
            burnEvent.setCancelled(true);
            Block block = burnEvent.getBlock();
            Block[] adjacentBlocks = new Block[] {
                block.getRelative(BlockFace.UP),
                block.getRelative(BlockFace.DOWN),
                block.getRelative(BlockFace.NORTH),
                block.getRelative(BlockFace.SOUTH),
                block.getRelative(BlockFace.EAST),
                block.getRelative(BlockFace.WEST)
            };

            // pro-actively put out any fires adjacent the burning block, to reduce future processing here
            for (Block adjacentBlock : adjacentBlocks) {
                if (adjacentBlock.getType() == Material.FIRE && adjacentBlock.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK) {
                    adjacentBlock.setType(Material.AIR);
                }
            }

            Block aboveBlock = block.getRelative(BlockFace.UP);
            if (aboveBlock.getType() == Material.FIRE) {
                aboveBlock.setType(Material.AIR);
            }
            return;
        }

        // never burn claimed blocks, regardless of settings
        if (plugin.getDataStore().getClaimAt(burnEvent.getBlock().getLocation(), false, null) != null) {
            burnEvent.setCancelled(true);
        }
    }

    // ensures fluids don't flow out of claims, unless into another claim where the owner is trusted to build
    private Claim lastSpreadClaim = null;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockFromTo(BlockFromToEvent spreadEvent) {
        WorldConfig wc = plugin.getWorldCfg(spreadEvent.getBlock().getWorld());
        // don't track fluid movement in worlds where claims are not enabled
        if (!wc.getClaimsEnabled()) return;

        // always allow fluids to flow straight down
        if (spreadEvent.getFace() == BlockFace.DOWN) return;

        // from where?
        Block fromBlock = spreadEvent.getBlock();
        Claim fromClaim = plugin.getDataStore().getClaimAt(fromBlock.getLocation(), false, this.lastSpreadClaim);
        if (fromClaim != null) {
            this.lastSpreadClaim = fromClaim;
        }

        // where to?
        Block toBlock = spreadEvent.getToBlock();
        Claim toClaim = plugin.getDataStore().getClaimAt(toBlock.getLocation(), false, fromClaim);

        // if it's within the same claim or wilderness to wilderness, allow it
        if (fromClaim == toClaim) return;

        // block any spread into the wilderness from a claim
        if (fromClaim != null && toClaim == null) {
            spreadEvent.setCancelled(true);
            return;
        }

        // who owns the spreading block, if anyone?
        OfflinePlayer fromOwner = null;
        if (fromClaim != null) {
            fromOwner = plugin.getServer().getOfflinePlayer(fromClaim.getOwnerName());
        }

        // cancel unless the owner of the spreading block is allowed to build in the receiving claim
        if (fromOwner == null || fromOwner.getPlayer() == null || toClaim.allowBuild(fromOwner.getPlayer()) != null) {
            spreadEvent.setCancelled(true);
        }

    }

    // ensures dispensers can't be used to dispense a block(like water or lava) or item across a claim boundary
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDispense(BlockDispenseEvent dispenseEvent) {
        // from where?
        Block fromBlock = dispenseEvent.getBlock();
        WorldConfig wc = plugin.getWorldCfg(fromBlock.getLocation().getWorld());
        if (fromBlock.getType().equals(Material.DROPPER)) return;
        // to where?
        Vector velocity = dispenseEvent.getVelocity();
        int xChange = 0;
        int zChange = 0;
        if (Math.abs(velocity.getX()) > Math.abs(velocity.getZ())) {
            if (velocity.getX() > 0) xChange = 1;
            else xChange = -1;
        } else {
            if (velocity.getZ() > 0) zChange = 1;
            else zChange = -1;
        }

        Block toBlock = fromBlock.getRelative(xChange, 0, zChange);

        Claim fromClaim = plugin.getDataStore().getClaimAt(fromBlock.getLocation(), false, null);
        Claim toClaim = plugin.getDataStore().getClaimAt(toBlock.getLocation(), false, fromClaim);

        // into wilderness is NOT OK when surface buckets are limited
        Material materialDispensed = dispenseEvent.getItem().getType();

        if (materialDispensed == Material.WATER_BUCKET && wc.getWaterBucketBehaviour().allowed(toBlock.getLocation(), null).allowed() ||
                (materialDispensed == Material.LAVA_BUCKET && wc.getLavaBucketBehaviour().allowed(toBlock.getLocation(), null).allowed())
                && plugin.claimsEnabledForWorld(fromBlock.getWorld())) {
            dispenseEvent.setCancelled(true);
            return;
        }

        // wilderness to wilderness is OK
        if (fromClaim == null && toClaim == null) return;

        // within claim is OK
        if (fromClaim == toClaim) return;

        // everything else is NOT OK
        dispenseEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent growEvent) {
        Location rootLocation = growEvent.getLocation();
        Claim rootClaim = plugin.getDataStore().getClaimAt(rootLocation, false, null);
        String rootOwnerName = null;

        // who owns the spreading block, if anyone?
        if (rootClaim != null) {
            // tree growth in subdivisions is dependent on who owns the top level claim
            if (rootClaim.getParent() != null) rootClaim = rootClaim.getParent();

            // if an administrative claim, just let the tree grow where it wants
            if (rootClaim.isAdminClaim()) return;

            // otherwise, note the owner of the claim
            rootOwnerName = rootClaim.getOwnerName();
        }

        // for each block growing
        for (int i = 0; i < growEvent.getBlocks().size(); i++) {
            BlockState block = growEvent.getBlocks().get(i);
            Claim blockClaim = plugin.getDataStore().getClaimAt(block.getLocation(), false, rootClaim);

            // if it's growing into a claim
            if (blockClaim != null) {
                // if there's no owner for the new tree, or the owner for the new tree is different from the owner of the claim
                if (rootOwnerName == null || !rootOwnerName.equals(blockClaim.getOwnerName())) {
                    growEvent.getBlocks().remove(i--);
                }
            }
        }
    }

    // processes broken log blocks to automatically remove floating treetops
    public void handleLogBroken(Block block) {
        // find the lowest log in the tree trunk including this log
        Block rootBlock = this.getRootBlock(block);

        // null indicates this block isn't part of a tree trunk
        if (rootBlock == null) return;

        // next step: scan for other log blocks and leaves in this tree

        // set boundaries for the scan
        int min_x = rootBlock.getX() - TREE_RADIUS;
        int max_x = rootBlock.getX() + TREE_RADIUS;
        int min_z = rootBlock.getZ() - TREE_RADIUS;
        int max_z = rootBlock.getZ() + TREE_RADIUS;
        int max_y = rootBlock.getWorld().getMaxHeight() - 1;

        // keep track of all the examined blocks, and all the log blocks found
        ArrayList<Block> examinedBlocks = new ArrayList<Block>();
        ArrayList<Block> treeBlocks = new ArrayList<Block>();

        // queue the first block, which is the block immediately above the player-chopped block
        ConcurrentLinkedQueue<Block> blocksToExamine = new ConcurrentLinkedQueue<Block>();
        blocksToExamine.add(rootBlock);
        examinedBlocks.add(rootBlock);

        boolean hasLeaves = false;

        while (!blocksToExamine.isEmpty()) {
            // pop a block from the queue
            Block currentBlock = blocksToExamine.remove();

            // if this is a log block, determine whether it should be chopped
            if (currentBlock.getType() == Material.LOG) {
                boolean partOfTree = false;

                // if it's stacked with the original chopped block, the answer is always yes
                if (currentBlock.getX() == block.getX() && currentBlock.getZ() == block.getZ()) {
                    partOfTree = true;
                }

                // otherwise find the block underneath this stack of logs
                else {
                    Block downBlock = currentBlock.getRelative(BlockFace.DOWN);
                    while (downBlock.getType() == Material.LOG) {
                        downBlock = downBlock.getRelative(BlockFace.DOWN);
                    }

                    // if it's air or leaves, it's okay to chop this block
                    // this avoids accidentally chopping neighboring trees which are close enough to touch their leaves to ours
                    if (downBlock.getType() == Material.AIR || downBlock.getType() == Material.LEAVES) {
                        partOfTree = true;
                    }

                    // otherwise this is a stack of logs which touches a solid surface
                    // if it's close to the original block's stack, don't clean up this tree (just stop here)
                    else {
                        if (Math.abs(downBlock.getX() - block.getX()) <= 1 && Math.abs(downBlock.getZ() - block.getZ()) <= 1)
                            return;
                    }
                }

                if (partOfTree) {
                    treeBlocks.add(currentBlock);
                }
            }

            // if this block is a log OR a leaf block, also check its neighbors
            if (currentBlock.getType() == Material.LOG || currentBlock.getType() == Material.LEAVES) {
                if (currentBlock.getType() == Material.LEAVES) {
                    hasLeaves = true;
                }

                Block[] neighboringBlocks = new Block[] {
                        currentBlock.getRelative(BlockFace.EAST),
                        currentBlock.getRelative(BlockFace.WEST),
                        currentBlock.getRelative(BlockFace.NORTH),
                        currentBlock.getRelative(BlockFace.SOUTH),
                        currentBlock.getRelative(BlockFace.UP),
                        currentBlock.getRelative(BlockFace.DOWN)
                };

                for (Block neighboringBlock : neighboringBlocks) {
                    // if the neighboringBlock is out of bounds, skip it
                    if (neighboringBlock.getX() < min_x || neighboringBlock.getX() > max_x || neighboringBlock.getZ() < min_z || neighboringBlock.getZ() > max_z || neighboringBlock.getY() > max_y)
                        continue;

                    // if we already saw this block, skip it
                    if (examinedBlocks.contains(neighboringBlock)) continue;

                    // mark the block as examined
                    examinedBlocks.add(neighboringBlock);

                    // if the neighboringBlock is a leaf or log, put it in the queue to be examined later
                    if (neighboringBlock.getType() == Material.LOG || neighboringBlock.getType() == Material.LEAVES) {
                        blocksToExamine.add(neighboringBlock);
                    }

                    // if we encounter any player-placed block type, bail out (don't automatically remove parts of this tree, it might support a treehouse!)
                    else if (this.isPlayerBlock(neighboringBlock)) {
                        return;
                    }
                }
            }
        }

        // if it doesn't have leaves, it's not a tree, so don't clean it up
        if (hasLeaves) {
            // schedule a cleanup task for later, in case the player leaves part of this tree hanging in the air
            TreeCleanupTask cleanupTask = new TreeCleanupTask(plugin, block, rootBlock, treeBlocks, rootBlock.getData());

            // 20L ~ 1 second, so 2 mins = 120 seconds ~ 2400L
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, cleanupTask, 2400L);
        }
    }

    // helper for above, finds the "root" of a stack of logs
    // will return null if the stack is determined to not be a natural tree
    private Block getRootBlock(Block logBlock) {
        if (logBlock.getType() != Material.LOG) return null;

        // run down through log blocks until finding a non-log block
        Block underBlock = logBlock.getRelative(BlockFace.DOWN);
        while (underBlock.getType() == Material.LOG) {
            underBlock = underBlock.getRelative(BlockFace.DOWN);
        }

        // if this is a standard tree, that block MUST be dirt
        if (underBlock.getType() != Material.DIRT) return null;

        // run up through log blocks until finding a non-log block
        Block aboveBlock = logBlock.getRelative(BlockFace.UP);
        while (aboveBlock.getType() == Material.LOG) {
            aboveBlock = aboveBlock.getRelative(BlockFace.UP);
        }

        // if this is a standard tree, that block MUST be air or leaves
        if (aboveBlock.getType() != Material.AIR && aboveBlock.getType() != Material.LEAVES) return null;

        return underBlock.getRelative(BlockFace.UP);
    }

    // for sake of identifying trees ONLY, a cheap but not 100% reliable method for identifying player-placed blocks
    private boolean isPlayerBlock(Block block) {
        Material material = block.getType();
        return !EnumSet.of(Material.AIR, Material.LEAVES, Material.LOG, Material.DIRT, Material.GRASS, Material.STATIONARY_WATER,
                Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.RED_ROSE, Material.LONG_GRASS, Material.SNOW,
                Material.STONE, Material.VINE, Material.WATER_LILY, Material.YELLOW_FLOWER, Material.CLAY).contains(material);
    }

}
