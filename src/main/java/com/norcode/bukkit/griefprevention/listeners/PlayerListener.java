/*
GriefPrevention Server Plugin for Minecraft
Copyright (C) 2011 Ryan Hamshire

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http:// www.gnu.org/licenses/>.
*/

package com.norcode.bukkit.griefprevention.listeners;

import com.norcode.bukkit.griefprevention.CreateClaimResult;
import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.ShovelMode;
import com.norcode.bukkit.griefprevention.configuration.ClaimBehaviourData;
import com.norcode.bukkit.griefprevention.configuration.MaterialInfo;
import com.norcode.bukkit.griefprevention.configuration.WorldConfig;
import com.norcode.bukkit.griefprevention.data.Claim;
import com.norcode.bukkit.griefprevention.data.PlayerData;
import com.norcode.bukkit.griefprevention.events.AllowPlayerActionEvent;
import com.norcode.bukkit.griefprevention.events.PlayerChangeClaimEvent;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import com.norcode.bukkit.griefprevention.tasks.EquipShovelProcessingTask;
import com.norcode.bukkit.griefprevention.visualization.Visualization;
import com.norcode.bukkit.griefprevention.visualization.VisualizationType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.regex.Pattern;

public class PlayerListener implements Listener {
    private final GriefPreventionTNG plugin;

    // timestamps of login and logout notifications in the last minute
    private ArrayList<Long> recentLoginLogoutNotifications = new ArrayList<Long>();

    // regex pattern for the "how do i claim land?" scanner
    private Pattern howToClaimPattern = null;
    private static final EnumSet<Material> ALLOWED_FILL_BLOCKS = EnumSet.of(Material.SAND, Material.ICE, Material.SANDSTONE, Material.STONE, Material.DIRT, Material.GRASS);
    private static final EnumSet<Material> BUTTON_MATERIALS = EnumSet.of(Material.STONE_BUTTON, Material.WOOD_BUTTON, Material.LEVER);
    private static final EnumSet<Material> TWEAKABLE_MATERIALS = EnumSet.of(Material.NOTE_BLOCK, Material.DIODE_BLOCK_ON,
            Material.DIODE_BLOCK_OFF, Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_COMPARATOR_ON);
    private static final EnumSet<Material> MINECART_MATERIALS = EnumSet.of(Material.MINECART, Material.POWERED_MINECART, Material.STORAGE_MINECART,
            Material.HOPPER_MINECART, Material.EXPLOSIVE_MINECART);
    private static final EnumSet<Material> CONTAINER_BLOCKS = EnumSet.of(Material.WORKBENCH, Material.ENDER_CHEST, Material.DISPENSER, Material.ANVIL,
            Material.BREWING_STAND, Material.JUKEBOX, Material.ENCHANTMENT_TABLE, Material.CAKE_BLOCK, Material.DROPPER, Material.HOPPER);
    // typical constructor, yawn
    public PlayerListener(GriefPreventionTNG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    synchronized void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!player.isOnline()) {
            event.setCancelled(true);
            return;
        }
        String message = event.getMessage();
        event.setCancelled(this.handlePlayerChat(player, message, event));
    }

    // returns true if the message should be sent, false if it should be muted 
    private boolean handlePlayerChat(Player player, String message, PlayerEvent event) {
        // FEATURE: automatically educate players about claiming land
        // watching for message format how*claim*, and will send a link to the basics video
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());

        if (this.howToClaimPattern == null) {
            this.howToClaimPattern = Pattern.compile(plugin.getMessageManager().getMessage(Messages.HowToClaimRegex), Pattern.CASE_INSENSITIVE);
        }
        Messages showclaimmessage = null;
        if (this.howToClaimPattern.matcher(message).matches()) {
            if (plugin.creativeRulesApply(player.getLocation())) {
                showclaimmessage = Messages.CreativeBasicsDemoAdvertisement;
            } else {
                showclaimmessage = Messages.SurvivalBasicsDemoAdvertisement;
            }
            // retrieve the data on this player...
            final PlayerData pdata = plugin.getDataStore().getPlayerData(player.getName());
            // if they are currently set to ignore, do not send anything.
            if (!pdata.isIgnoreClaimMessage()) {
                // otherwise, set IgnoreClaimMessage and use a anonymous runnable to reset it after the timeout.
                // of note is that if the value is zero this is pretty much run right away, which means the end result is there
                // is no actual timeout.
                pdata.setIgnoreClaimMessage(true);
                Bukkit.getScheduler()
                        .runTaskLater(
                                plugin,
                                new Runnable() {
                                    public void run() {
                                        pdata.setIgnoreClaimMessage(false);
                                    }
                                }, wc.getMessageCooldownClaims() * 20);

                // send off the message.
                plugin.sendMessage(player, TextMode.INFO, showclaimmessage, 10L);
            }
        }

        // FEATURE: automatically educate players about the /trapped command
        // check for "trapped" or "stuck" to educate players about the /trapped command
        if (!message.contains("/trapped") && (message.contains("trapped") || message.contains("stuck") || message.contains(plugin.getMessageManager().getMessage(Messages.TrappedChatKeyword)))) {
            final PlayerData pdata = plugin.getDataStore().getPlayerData(player.getName());
            // if not set to ignore the stuck message, show it, set the ignore flag, and set an anonymous runnable to reset it after the
            // configured delay.
            if (!pdata.isIgnoreStuckMessage()) {
                pdata.setIgnoreStuckMessage(true);
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    public void run() {
                        pdata.setIgnoreStuckMessage(true);
                    }
                }, wc.getMessageCooldownStuck() * 20);
                plugin.sendMessage(player, TextMode.INFO, Messages.TrappedInstructions, 10L);
            }
        }
        return false;
    }

    // when a player uses a slash command...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    synchronized void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ");
        WorldConfig wc = plugin.getWorldCfg(event.getPlayer().getWorld());
        String command = args[0].toLowerCase();
        // if in pvp, block any pvp-banned slash commands
        PlayerData playerData = plugin.getDataStore().getPlayerData(event.getPlayer().getName());
        if (playerData.inPvpCombat() && wc.getPvPBlockedCommands().contains(command)) {
            event.setCancelled(true);
            plugin.sendMessage(event.getPlayer(), TextMode.ERROR, Messages.CommandBannedInPvP);
            return;
        }
    }

    // when a player attempts to join the server...
    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        // remember the player's ip address
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        playerData.setIpAddress(event.getAddress());
    }

    // when a player spawns, conditionally apply temporary pvp protection 
    @EventHandler(ignoreCancelled = true)
    void onPlayerRespawn(PlayerRespawnEvent event) {
        PlayerData playerData = plugin.getDataStore().getPlayerData(event.getPlayer().getName());
        playerData.setLastSpawn(Calendar.getInstance().getTimeInMillis());
        plugin.checkPvpProtectionNeeded(event.getPlayer());
    }

    // when a player successfully joins the server...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        String playerName = player.getName();
        // note login time
        long now = Calendar.getInstance().getTimeInMillis();
        final PlayerData playerData = plugin.getDataStore().getPlayerData(playerName);
        playerData.setLastSpawn(now);
        playerData.setLastLogin(new Date());
        plugin.getDataStore().savePlayerData(playerName, playerData);

        // if player has never played on the server before, may need pvp protection
        if (!player.hasPlayedBefore()) {
            plugin.checkPvpProtectionNeeded(player);
        }

        // silence notifications when they're coming too fast
        if (event.getJoinMessage() != null && this.shouldSilenceNotification()) {
            event.setJoinMessage(null);
        }
    }

    public String allowAccess(Player player, Claim claim, Event event) {
        if (claim == null) {
            return null;
        }
        String denyMsg = claim.allowAccess(player);
        AllowPlayerActionEvent allowEvent = new AllowPlayerActionEvent(player, claim, event, denyMsg);
        plugin.getServer().getPluginManager().callEvent(allowEvent);
        return allowEvent.getDenyMessage();
    }

    // when a player dies...
    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerDeath(PlayerDeathEvent event) {
        PlayerData playerData = plugin.getDataStore().getPlayerData(event.getEntity().getName());
        long now = Calendar.getInstance().getTimeInMillis();
        playerData.setLastDeathTimeStamp(now);
    }

    // when a player quits...
    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        // WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());

        // silence notifications when they're coming too fast
        if (event.getQuitMessage() != null && this.shouldSilenceNotification()) {
            event.setQuitMessage(null);
        }

        // make sure his data is all saved - he might have accrued some claim blocks while playing that were not saved immediately
        plugin.getDataStore().savePlayerData(player.getName(), playerData);

        this.onPlayerDisconnect(event.getPlayer(), event.getQuitMessage());
    }

    // helper for above
    private void onPlayerDisconnect(Player player, String notificationMessage) {
        String playerName = player.getName();
        PlayerData playerData = plugin.getDataStore().getPlayerData(playerName);
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        // FEATURE: claims where players have allowed explosions will revert back to not allowing them when the owner logs out
        for (Claim claim : playerData.getClaims()) {
            claim.setExplosivesAllowed(false);
        }

        // FEATURE: players in pvp combat when they log out will die
        if (wc.getPvPPunishLogout() && playerData.inPvpCombat()) {
            player.setHealth(0);
        }
    }

    // determines whether or not a login or logout notification should be silenced, depending on how many there have been in the last minute
    private boolean shouldSilenceNotification() {
        final long ONE_MINUTE = 60000;
        final int MAX_ALLOWED = 20;
        Long now = Calendar.getInstance().getTimeInMillis();

        // eliminate any expired entries (longer than a minute ago)
        for (int i = 0; i < this.recentLoginLogoutNotifications.size(); i++) {
            Long notificationTimestamp = this.recentLoginLogoutNotifications.get(i);
            if (now - notificationTimestamp > ONE_MINUTE) {
                this.recentLoginLogoutNotifications.remove(i--);
            } else {
                break;
            }
        }

        // add the new entry
        this.recentLoginLogoutNotifications.add(now);
        return this.recentLoginLogoutNotifications.size() > MAX_ALLOWED;
    }

    // when a player drops an item
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        // in creative worlds, dropping items is blocked
        if (wc.getCreativeRules()) {
            event.setCancelled(true);
            return;
        }
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        // if in combat, don't let him drop it
        if (!wc.getAllowCombatItemDrop() && playerData.inPvpCombat()) {
            plugin.sendMessage(player, TextMode.ERROR, Messages.PvPNoDrop);
            event.setCancelled(true);
        }
    }

    // when a player teleports
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());

        // FEATURE: prevent players from using ender pearls to gain access to secured claims
        if (event.getCause() == TeleportCause.ENDER_PEARL && wc.getEnderPearlsRequireAccessTrust()) {
            Claim toClaim = plugin.getDataStore().getClaimAt(player.getLocation(), false, playerData.getLastClaim());
            if (toClaim != null) {
                playerData.setLastClaim(toClaim);
                String noAccessReason = allowAccess(player, toClaim, event);
                if (noAccessReason != null) {
                    plugin.sendMessage(player, TextMode.ERROR, noAccessReason);
                    event.setCancelled(true);
                }
            }
        }
        // these rules only apply to non-ender-pearl teleportation
        if (event.getCause() == TeleportCause.ENDER_PEARL) return;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        WorldConfig wc = plugin.getWorldCfg(event.getEntity().getWorld());
        Player player = event.getPlayer();
        Entity entity = event.getEntity();
        if (wc.getShearingRules().allowed(entity.getLocation(), player).denied()) {
            event.setCancelled(true);
        }

    }

    // when a player interacts with an entity...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        // don't allow interaction with item frames in claimed areas without build permission
        if (entity instanceof Hanging) {
            String noBuildReason = plugin.getBlockListener().allowBuild(player, entity.getLocation(), playerData, playerData.getLastClaim(), event);
            if (noBuildReason != null) {
                plugin.sendMessage(player, TextMode.ERROR, noBuildReason);
                event.setCancelled(true);
                return;
            }
        }

        if (entity instanceof Villager && wc.getVillagerTrades().allowed(entity.getLocation(), player).denied()) {
            event.setCancelled(true);
            return;
        }
        if ((entity instanceof Sheep && event.getPlayer().getItemInHand().getType() == Material.INK_SACK)) {
            // apply dyeing rules.
            if (wc.getSheepDyeingRules().allowed(entity.getLocation(), event.getPlayer()).denied()) {
                event.setCancelled(true);
                return;
            }
        }

        // don't allow container access during pvp combat

        if ((entity instanceof StorageMinecart || entity instanceof PoweredMinecart || entity instanceof HopperMinecart)) {
            if (playerData.inPvpCombat()) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.PvPNoContainers);
                event.setCancelled(true);
                return;
            }
        }

        // if the entity is a vehicle and we're preventing theft in claims		
        if (wc.getClaimsPreventTheft() && entity instanceof Vehicle) {
            // if the entity is in a claim
            Claim claim = plugin.getDataStore().getClaimAt(entity.getLocation(), false, null);
            if (claim != null) {
                // for storage, hopper, and powered minecarts, apply container rules (this is a potential theft)
                if (entity instanceof StorageMinecart || entity instanceof PoweredMinecart || entity instanceof HopperMinecart) {
                    String noContainersReason = claim.allowContainers(player);
                    if (noContainersReason != null) {
                        plugin.sendMessage(player, TextMode.ERROR, noContainersReason);
                        event.setCancelled(true);
                    }
                }

                // for boats, apply access rules
                else if (entity instanceof Boat) {
                    String noAccessReason = allowAccess(player, claim, event);
                    if (noAccessReason != null) {
                        player.sendMessage(noAccessReason);
                        event.setCancelled(true);
                    }
                }

                // if the entity is an animal, apply container rules
                else if (entity instanceof Animals) {
                    if (claim.allowContainers(player) != null) {
                        plugin.sendMessage(player, TextMode.ERROR, Messages.NoDamageClaimedEntity);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    // when a player picks up an item...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        // we allow plugins to give players items regardless.
        // eg: this only prevents "true" pickups.
        if (event.getItem().getTicksLived() <= 10) return;
        // presumption: items given by Plugins will be nearly instant.
        // so if a item is triggering this event and younger than half a second, we'll assume some plugin
        // has bestowed it.
        Player player = event.getPlayer();
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        if (!event.getPlayer().getWorld().getPVP()) return;

        // if we're preventing spawn camping and the player was previously empty handed...
        if (wc.getProtectFreshSpawns() && (player.getItemInHand().getType() == Material.AIR)) {
            // if that player is currently immune to pvp
            PlayerData playerData = plugin.getDataStore().getPlayerData(event.getPlayer().getName());
            if (playerData.isPvpImmune()) {
                // if it's been less than 10 seconds since the last time he spawned, don't pick up the item
                long now = Calendar.getInstance().getTimeInMillis();
                long elapsedSinceLastSpawn = now - playerData.getLastSpawn();
                if (elapsedSinceLastSpawn < 10000) {
                    event.setCancelled(true);
                    return;
                }

                // otherwise take away his immunity. he may be armed now.  at least, he's worth killing for some loot
                playerData.setPvpImmune(false);
                plugin.sendMessage(player, TextMode.WARN, Messages.PvPImmunityEnd);
            }
        }
    }

    // when a player switches in-hand items
    @EventHandler(ignoreCancelled = true)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        // if he's switching to the golden shovel
        ItemStack newItemStack = player.getInventory().getItem(event.getNewSlot());
        if (newItemStack != null && newItemStack.getType() == wc.getClaimsModificationTool()) {
            PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());

            // always reset to basic claims mode
            if (playerData.getShovelMode() != ShovelMode.BASIC) {
                playerData.setShovelMode(ShovelMode.BASIC);
                plugin.sendMessage(player, TextMode.INFO, Messages.ShovelBasicClaimMode);
            }

            // reset any work he might have been doing
            playerData.setLastShovelLocation(null);
            playerData.setClaimResizing(null);

            // give the player his available claim blocks count and claiming instructions, but only if he keeps the shovel equipped for a minimum time, to avoid mouse wheel spam
            if (plugin.claimsEnabledForWorld(player.getWorld())) {
                EquipShovelProcessingTask task = new EquipShovelProcessingTask(plugin, player);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, 15L);  // 15L is approx. 3/4 of a second
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Don't do anything if there are no plugins listening
        if (PlayerChangeClaimEvent.getHandlerList().getRegisteredListeners().length > 0) {
            // Don't do anything if they haven't crossed a block boundary.
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                    event.getFrom().getBlockZ() != event.getTo().getBlockZ() ||
                    event.getFrom().getWorld().getName().equals(event.getTo().getWorld().getName())) {

                PlayerData playerData = plugin.getDataStore().getPlayerData(event.getPlayer().getName());
                Claim oldClaim = plugin.getDataStore().getClaimAt(event.getFrom(), true, playerData.getLastClaim());
                Claim newClaim = plugin.getDataStore().getClaimAt(event.getTo(), true, oldClaim);
                if ((oldClaim == null) != (newClaim == null) ||
                        oldClaim != null && !oldClaim.equals(newClaim)) {
                    PlayerChangeClaimEvent claimChangeEvent = new PlayerChangeClaimEvent(event.getPlayer(), oldClaim, newClaim);
                    plugin.getServer().getPluginManager().callEvent(claimChangeEvent);
                    if (claimChangeEvent.isCancelled()) {
                        event.setCancelled(true);
                    } else {
                        playerData.setLastClaim(newClaim);
                    }
                }
            }
        }
    }

    // block players from entering beds they don't have permission for
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent bedEvent) {
        Player player = bedEvent.getPlayer();
        Block block = bedEvent.getBed();
        WorldConfig wc = plugin.getWorldCfg(block.getWorld());
        if (!wc.getClaimsPreventButtonsSwitches()) return;
        // if the bed is in a claim 
        Claim claim = plugin.getDataStore().getClaimAt(block.getLocation(), false, null);
        if (claim != null) {
            // if the player doesn't have access in that claim, tell him so and prevent him from sleeping in the bed
            if (allowAccess(player, claim, bedEvent) != null) {
                bedEvent.setCancelled(true);
                plugin.sendMessage(player, TextMode.ERROR, Messages.NoBedPermission, claim.getFriendlyOwnerName());
            }
        }
    }

    // block use of buckets within other players' claims
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent bucketEvent) {
        Player player = bucketEvent.getPlayer();
        Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
        WorldConfig wc = plugin.getWorldCfg(block.getWorld());
        int minLavaDistance = 10;

        ClaimBehaviourData.ClaimAllowanceConstants bucketBehaviour = null;

        if (bucketEvent.getBucket() == Material.LAVA_BUCKET) {
            bucketBehaviour = wc.getLavaBucketBehaviour().allowed(block.getLocation(), player);
        } else if (bucketEvent.getBucket() == Material.WATER_BUCKET) {
            bucketBehaviour = wc.getWaterBucketBehaviour().allowed(block.getLocation(), player);
        }
        switch (bucketBehaviour) {
            case ALLOW_FORCED:
                return;
            case DENY_FORCED:
                bucketEvent.setCancelled(true);
                return;
            default:
        }

        // if the bucket is being used in a claim, allow for dumping lava closer to other players
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        Claim claim = plugin.getDataStore().getClaimAt(block.getLocation(), false, playerData.getLastClaim());

        // make sure the player is allowed to build at the location
        String noBuildReason = plugin.getBlockListener().allowBuild(player, block.getLocation(), playerData, claim, bucketEvent);
        if (noBuildReason != null) {
            plugin.sendMessage(player, TextMode.ERROR, noBuildReason);
            bucketEvent.setCancelled(true);
            return;
        }

        // checks for Behaviour perms.
        if (bucketEvent.getBucket() == Material.LAVA_BUCKET) {
            if (wc.getLavaBucketBehaviour().allowed(block.getLocation(), player).denied()) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.ConfigDisabled, "Lava placement ");
                bucketEvent.setCancelled(true);
                return;
            }
        } else if (bucketEvent.getBucket() == Material.WATER_BUCKET) {
            if (wc.getWaterBucketBehaviour().allowed(block.getLocation(), player).denied()) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.ConfigDisabled, "Water placement ");
                bucketEvent.setCancelled(true);
                return;
            }
        }
        if (claim != null) {
            minLavaDistance = 3;
        }
        // otherwise no wilderness dumping (unless underground) in worlds where claims are enabled
        else if (wc.getClaimsEnabled()) // outside claims logic...
        {
            if (!player.hasPermission("griefprevention.lava")) {
                if (bucketEvent.getBucket() == Material.LAVA_BUCKET) {
                    plugin.sendMessage(player, TextMode.ERROR, Messages.NoWildernessBuckets);
                    bucketEvent.setCancelled(true);
                    return;
                }
                if (bucketEvent.getBucket() == Material.WATER_BUCKET) {

                    if (wc.getWaterBucketBehaviour().allowed(block.getLocation(), player).denied()) {
                        plugin.sendMessage(player, TextMode.ERROR, Messages.NoWildernessBuckets);
                        bucketEvent.setCancelled(true);
                        return;
                    }
                }

            }
        }
        // lava buckets can't be dumped near other players unless pvp is on
        if (!block.getWorld().getPVP() && !player.hasPermission("griefprevention.lava")) {
            if (bucketEvent.getBucket() == Material.LAVA_BUCKET) {

                if (wc.getLavaBucketBehaviour().allowed(block.getLocation(), player).denied()) {
                    plugin.sendMessage(player, TextMode.ERROR, Messages.ConfigDisabled, "Lava Placement");
                    bucketEvent.setCancelled(true);
                    return;
                }

                List<Player> players = block.getWorld().getPlayers();
                for (Player otherPlayer : players) {
                    Location location = otherPlayer.getLocation();
                    if (!otherPlayer.equals(player) && block.getY() >= location.getBlockY() - 1 && location.distanceSquared(block.getLocation()) < minLavaDistance * minLavaDistance) {
                        plugin.sendMessage(player, TextMode.ERROR, Messages.NoLavaNearOtherPlayer, otherPlayer.getName());
                        bucketEvent.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    // see above
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBucketFill(PlayerBucketFillEvent bucketEvent) {
        Player player = bucketEvent.getPlayer();
        Block block = bucketEvent.getBlockClicked();
        WorldConfig wc = plugin.getWorldCfg(block.getWorld());
        // make sure the player is allowed to build at the location
        // String noBuildReason = plugin.allowBuild(player, block.getLocation());
        if (wc.getWaterBucketBehaviour().allowed(block.getLocation(), player).denied()) {
            // plugin.sendMessage(player, TextMode.Err, noBuildReason);
            bucketEvent.setCancelled(true);
        }
    }
    // when a player interacts with the world
    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        // determine target block.  FEATURE: shovel and stick can be used from a distance away
        Block clickedBlock = null;

        try {
            clickedBlock = event.getClickedBlock();  // null returned here means interacting with air
            if (clickedBlock == null || clickedBlock.getType() == Material.SNOW) {
                // try to find a far away non-air block along line of sight
                HashSet<Byte> transparentMaterials = new HashSet<Byte>();
                transparentMaterials.add((byte) Material.AIR.getId());
                transparentMaterials.add((byte) Material.SNOW.getId());
                transparentMaterials.add((byte) Material.LONG_GRASS.getId());
                clickedBlock = player.getTargetBlock(transparentMaterials, 200);
            }
        } catch (Exception e) {
            return;
        }

        // if no block, stop here
        if (clickedBlock == null) {
            return;
        }

        Material clickedBlockType = clickedBlock.getType();

        // apply rules for putting out fires (requires build permission)
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        if (event.getClickedBlock() != null && event.getClickedBlock().getRelative(event.getBlockFace()).getType() == Material.FIRE) {
            Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), false, playerData.getLastClaim());
            if (claim != null) {
                playerData.setLastClaim(claim);
                String noBuildReason = claim.allowBuild(player);
                if (noBuildReason != null) {
                    event.setCancelled(true);
                    plugin.sendMessage(player, TextMode.ERROR, noBuildReason);
                    return;
                }
            }
        }

        // apply rules for containers and crafting blocks
        if (wc.getClaimsPreventTheft() && event.getClickedBlock() != null && (
                event.getAction() == Action.RIGHT_CLICK_BLOCK && (
                        clickedBlock.getState() instanceof InventoryHolder ||
                                CONTAINER_BLOCKS.contains(clickedBlockType) ||
                                wc.getModsContainerTrustIds().contains(new MaterialInfo(clickedBlock.getTypeId(), clickedBlock.getData(), null))))) {

            // block container use during pvp combat, same reason
            if (playerData.inPvpCombat() && wc.getPvPBlockContainers()) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.PvPNoContainers);
                event.setCancelled(true);
                return;
            }

            // otherwise check permissions for the claim the player is in
            Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), false, playerData.getLastClaim());
            if (claim != null) {
                playerData.setLastClaim(claim);

                String noContainersReason = claim.allowContainers(player);
                if (noContainersReason != null) {

                    event.setCancelled(true);
                    plugin.sendMessage(player, TextMode.ERROR, noContainersReason);
                    return;
                }
            }

            // if the event hasn't been cancelled, then the player is allowed to use the container
            // so drop any pvp protection
            if (playerData.isPvpImmune()) {
                playerData.setPvpImmune(false);
                plugin.sendMessage(player, TextMode.WARN, Messages.PvPImmunityEnd);
            }
        }

        // otherwise apply rules for doors, if configured that way
        else if (event.getClickedBlock() != null && ((wc.getClaimsLockWoodenDoors() && clickedBlockType == Material.WOODEN_DOOR) ||
                (wc.getClaimsLockTrapDoors() && clickedBlockType == Material.TRAP_DOOR) ||
                (wc.getClaimsLockFenceGates() && clickedBlockType == Material.FENCE_GATE))) {
            Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), false, playerData.getLastClaim());
            if (claim != null) {
                playerData.setLastClaim(claim);

                String noAccessReason = allowAccess(player, claim, event);
                if (noAccessReason != null) {

                    event.setCancelled(true);
                    plugin.sendMessage(player, TextMode.ERROR, noAccessReason);
                    return;
                }
            }
        }
        // otherwise apply rules for buttons and switches
        else if (event.getClickedBlock() != null && wc.getClaimsPreventButtonsSwitches() && (clickedBlockType == null || clickedBlockType == Material.STONE_BUTTON || clickedBlockType == Material.WOOD_BUTTON || clickedBlockType == Material.LEVER || wc.getModsAccessTrustIds().contains(new MaterialInfo(clickedBlock.getTypeId(), clickedBlock.getData(), null)))) {
            Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), false, playerData.getLastClaim());
            if (claim != null) {
                playerData.setLastClaim(claim);

                String noAccessReason = allowAccess(player, claim, event);
                if (noAccessReason != null) {
                    event.setCancelled(true);
                    plugin.sendMessage(player, TextMode.ERROR, noAccessReason);
                    return;
                }
            }
        }

        // apply rule for players trampling tilled soil back to dirt (never allow it)
        // NOTE: that this event applies only to players.  monsters and animals can still trample.
        else if (event.getAction() == Action.PHYSICAL && clickedBlockType == Material.SOIL) {
            if (wc.getPlayerTrampleRules().allowed(event.getPlayer().getLocation(), event.getPlayer()).denied()) {
                event.setCancelled(true);
                return;
            }
        }

        // apply rule for note blocks and repeaters
        else if (event.getClickedBlock() != null && TWEAKABLE_MATERIALS.contains(clickedBlockType)) {
            Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), false, playerData.getLastClaim());
            if (claim != null) {
                String noBuildReason = claim.allowBuild(player);
                if (noBuildReason != null) {
                    event.setCancelled(true);
                    plugin.sendMessage(player, TextMode.ERROR, noBuildReason);
                    return;
                }
            }
        } else { // otherwise handle right click (shovel, stick, bonemeal)
            // ignore all actions except right-click on a block or in the air
            Action action = event.getAction();
            if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

            // what's the player holding?
            Material materialInHand = player.getItemInHand().getType();

            // if it's bonemeal, check for build permission (ink sac == bone meal, must be a Bukkit bug?)
            if (materialInHand == Material.INK_SACK && event.getClickedBlock() != null) {
                if (wc.getBonemealGrassRules().allowed(event.getClickedBlock().getLocation(), event.getPlayer()).denied()) {
                    event.setCancelled(true);
                }
                return;
            } else if (materialInHand == Material.BOAT) {
                Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), false, playerData.getLastClaim());
                if (claim != null) {
                    String noAccessReason = allowAccess(player, claim, event);
                    if (noAccessReason != null) {
                        plugin.sendMessage(player, TextMode.ERROR, noAccessReason);
                        event.setCancelled(true);
                    }
                }
                return;
            } else if ((materialInHand == Material.MONSTER_EGG || materialInHand == Material.MINECART || materialInHand == Material.POWERED_MINECART || materialInHand == Material.STORAGE_MINECART
                    || materialInHand == Material.HOPPER_MINECART || materialInHand == Material.EXPLOSIVE_MINECART || materialInHand == Material.BOAT) && plugin.creativeRulesApply(clickedBlock.getLocation())) {
                // if it's a spawn egg, minecart, or boat, and this is a creative world, apply special rules
                // player needs build permission at this location
                String noBuildReason = plugin.getBlockListener().allowBuild(player, clickedBlock.getLocation(), playerData, null, event);
                if (noBuildReason != null) {
                    plugin.sendMessage(player, TextMode.ERROR, noBuildReason);
                    System.out.println("CANCELLING Container Access.");
                    event.setCancelled(true);
                    return;
                }

                // enforce limit on total number of entities in this claim
                Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), false, playerData.getLastClaim());
                if (claim == null) return;

                String noEntitiesReason = claim.allowMoreEntities();
                if (noEntitiesReason != null) {
                    plugin.sendMessage(player, TextMode.ERROR, noEntitiesReason);
                    event.setCancelled(true);
                    return;
                }
                return;
            }

            // if he's investigating a claim			
            else if (materialInHand == wc.getClaimsInvestigationTool()) {
                // air indicates too far away
                if (clickedBlockType == Material.AIR) {
                    plugin.sendMessage(player, TextMode.ERROR, Messages.TooFarAway);
                    return;
                }

                Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), false /*ignore height*/, playerData.getLastClaim());

                // no claim case
                if (claim == null) {
                    plugin.sendMessage(player, TextMode.INFO, Messages.BlockNotClaimed);
                    Visualization.Revert(plugin, player);
                }

                // claim case
                else {
                    playerData.setLastClaim(claim);
                    plugin.sendMessage(player, TextMode.INFO, Messages.BlockClaimed, claim.getFriendlyOwnerName());

                    // visualize boundary
                    Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.CLAIM, player.getLocation());
                    Visualization.apply(plugin, player, visualization);

                    // if can resize this claim, tell about the boundaries
                    if (claim.allowEdit(player) == null) {
                        plugin.sendMessage(player, TextMode.INFO, "  " + claim.getWidth() + "x" + claim.getHeight() + "=" + claim.getArea());
                    }

                    // if deleteclaims permission, tell about the player's offline time
                    if (!claim.isAdminClaim() && player.hasPermission("griefprevention.deleteclaims")) {
                        PlayerData otherPlayerData = plugin.getDataStore().getPlayerData(claim.getOwnerName());
                        if (otherPlayerData != null) {
                            Date lastLogin = otherPlayerData.getLastLogin();
                            Date now = new Date();
                            long daysElapsed = (now.getTime() - lastLogin.getTime()) / (1000 * 60 * 60 * 24);

                            plugin.sendMessage(player, TextMode.INFO, Messages.PlayerOfflineTime, String.valueOf(daysElapsed));
                        }
                    }
                }
                return;
            }

            // if it's a golden shovel
            else if (materialInHand != wc.getClaimsModificationTool()) return;

            // can't use the shovel from too far away
            if (clickedBlockType == Material.AIR) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.TooFarAway);
                return;
            }

            // TODO Gold Shovel Starts Here. Make this method manageable.
            // if the player is in restore nature mode, do only that
            String playerName = player.getName();
            playerData = plugin.getDataStore().getPlayerData(player.getName());
            if (playerData.getShovelMode() == ShovelMode.RESTORE_NATURE || playerData.getShovelMode() == ShovelMode.RESTORE_NATURE_AGGRESSIVE) {
                // if the clicked block is in a claim, visualize that claim and deliver an error message
                Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), false, playerData.getLastClaim());
                if (claim != null) {
                    plugin.sendMessage(player, TextMode.ERROR, Messages.BlockClaimed, claim.getFriendlyOwnerName());
                    Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ERROR_CLAIM, player.getLocation());
                    Visualization.apply(plugin, player, visualization);
                    return;
                }

                // figure out which chunk to repair
                Chunk chunk = player.getWorld().getChunkAt(clickedBlock.getLocation());

                // start the repair process

                // set boundaries for processing
                int miny = clickedBlock.getY();

                // if not in aggressive mode, extend the selection down to a little below sea level
                if (!(playerData.getShovelMode() == ShovelMode.RESTORE_NATURE_AGGRESSIVE)) {
                    if (miny > plugin.getWorldCfg(chunk.getWorld()).getSeaLevelOverride() - 10) {
                        miny = plugin.getWorldCfg(chunk.getWorld()).getSeaLevelOverride() - 10;
                    }
                }

                plugin.restoreChunk(chunk, miny, playerData.getShovelMode() == ShovelMode.RESTORE_NATURE_AGGRESSIVE, 0, player);
                return;
            }

            // if in restore nature fill mode
            if (playerData.getShovelMode() == ShovelMode.RESTORE_NATURE_FILL) {
                ArrayList<Material> allowedFillBlocks = new ArrayList<Material>();
                Environment environment = clickedBlock.getWorld().getEnvironment();
                if (environment == Environment.NETHER) {
                    allowedFillBlocks.add(Material.NETHERRACK);
                } else if (environment == Environment.THE_END) {
                    allowedFillBlocks.add(Material.ENDER_STONE);
                } else {
                    allowedFillBlocks.add(Material.GRASS);
                    allowedFillBlocks.add(Material.DIRT);
                    allowedFillBlocks.add(Material.STONE);
                    allowedFillBlocks.add(Material.SAND);
                    allowedFillBlocks.add(Material.SANDSTONE);
                    allowedFillBlocks.add(Material.ICE);
                }

                Block centerBlock = clickedBlock;

                int maxHeight = centerBlock.getY();
                int minx = centerBlock.getX() - playerData.getFillRadius();
                int maxx = centerBlock.getX() + playerData.getFillRadius();
                int minz = centerBlock.getZ() - playerData.getFillRadius();
                int maxz = centerBlock.getZ() + playerData.getFillRadius();
                int minHeight = maxHeight - 10;
                if (minHeight < 0) minHeight = 0;

                Claim cachedClaim = null;
                for (int x = minx; x <= maxx; x++) {
                    for (int z = minz; z <= maxz; z++) {
                        // circular brush
                        Location location = new Location(centerBlock.getWorld(), x, centerBlock.getY(), z);
                        if (location.distance(centerBlock.getLocation()) > playerData.getFillRadius()) continue;

                        // default fill block is initially the first from the allowed fill blocks list above
                        Material defaultFiller = allowedFillBlocks.get(0);

                        // prefer to use the block the player clicked on, if it's an acceptable fill block
                        if (allowedFillBlocks.contains(centerBlock.getType())) {
                            defaultFiller = centerBlock.getType();
                        }

                        // if the player clicks on water, try to sink through the water to find something underneath that's useful for a filler
                        else if (centerBlock.getType() == Material.WATER || centerBlock.getType() == Material.STATIONARY_WATER) {
                            Block block = centerBlock.getWorld().getBlockAt(centerBlock.getLocation());
                            while (!allowedFillBlocks.contains(block.getType()) && block.getY() > centerBlock.getY() - 10) {
                                block = block.getRelative(BlockFace.DOWN);
                            }
                            if (allowedFillBlocks.contains(block.getType())) {
                                defaultFiller = block.getType();
                            }
                        }

                        // fill bottom to top
                        for (int y = minHeight; y <= maxHeight; y++) {
                            Block block = centerBlock.getWorld().getBlockAt(x, y, z);

                            // respect claims
                            Claim claim = plugin.getDataStore().getClaimAt(block.getLocation(), false, cachedClaim);
                            if (claim != null) {
                                cachedClaim = claim;
                                break;
                            }

                            // only replace air, spilling water, snow, long grass
                            if (block.getType() == Material.AIR || block.getType() == Material.SNOW || (block.getType() == Material.STATIONARY_WATER && block.getData() != 0) || block.getType() == Material.LONG_GRASS) {
                                // if the top level, always use the default filler picked above
                                if (y == maxHeight) {
                                    block.setType(defaultFiller);
                                } else {
                                    // otherwise look to neighbors for an appropriate fill block
                                    Block eastBlock = block.getRelative(BlockFace.EAST);
                                    Block westBlock = block.getRelative(BlockFace.WEST);
                                    Block northBlock = block.getRelative(BlockFace.NORTH);
                                    Block southBlock = block.getRelative(BlockFace.SOUTH);

                                    // first, check lateral neighbors (ideally, want to keep natural layers)
                                    if (allowedFillBlocks.contains(eastBlock.getType())) {
                                        block.setType(eastBlock.getType());
                                    } else if (allowedFillBlocks.contains(westBlock.getType())) {
                                        block.setType(westBlock.getType());
                                    } else if (allowedFillBlocks.contains(northBlock.getType())) {
                                        block.setType(northBlock.getType());
                                    } else if (allowedFillBlocks.contains(southBlock.getType())) {
                                        block.setType(southBlock.getType());
                                    }

                                    // if all else fails, use the default filler selected above
                                    else {
                                        block.setType(defaultFiller);
                                    }
                                }
                            }
                        }
                    }
                }
                return;
            }

            // if the player doesn't have claims permission, don't do anything
            if (wc.getCreateClaimRequiresPermission() && !player.hasPermission("griefprevention.createclaims")) {
                plugin.sendMessage(player, TextMode.ERROR, Messages.NoCreateClaimPermission);
                return;
            }
            plugin.debug("We are creating/resizing " + playerData.getClaimResizing());
//            // TODO Claim Create / Resize Starts Here.
//            if (playerData.getClaimResizing() == null) {
//                // see if the player has clicked inside one of their claims.
//                Claim checkclaim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), true, null);
//                // is there even a claim here?
//                if (checkclaim != null) {
//                    // there is a claim; make sure it belongs to this player.
//                    String cannotedit = checkclaim.allowEdit(player);
//                    if (cannotedit == null) {
//                        // it DOES belong to them.
//                        // automatically switch to advanced claims mode, and show a message.
//                        playerData.setClaimSubdividing(checkclaim);
//                        playerData.setShovelMode(ShovelMode.SUBDIVIDE);
//                        // TODO: Raise StartClaimSubdivideEvent
//                        plugin.sendMessage(player, TextMode.INFO, "Entering Claim subdivide mode.");
//                    }
//                }
//            }

            // if he's resizing a claim and that claim hasn't been deleted since he started resizing it
            if (playerData.getClaimResizing() != null && playerData.getClaimResizing().isInDataStore()) {
                if (clickedBlock.getLocation().equals(playerData.getLastShovelLocation())) return;
                plugin.debug("We are resizing " + playerData.getClaimResizing());
                // figure out what the coords of his new claim would be
                int newx1, newx2, newz1, newz2, newy1, newy2;
                if (playerData.getLastShovelLocation().getBlockX() == playerData.getClaimResizing().getMin().getBlockX()) {
                    newx1 = clickedBlock.getX();
                } else {
                    newx1 = playerData.getClaimResizing().getMin().getBlockX();
                }

                if (playerData.getLastShovelLocation().getBlockX() == playerData.getClaimResizing().getMax().getBlockX()) {
                    newx2 = clickedBlock.getX();
                } else {
                    newx2 = playerData.getClaimResizing().getMax().getBlockX();
                }

                if (playerData.getLastShovelLocation().getBlockZ() == playerData.getClaimResizing().getMin().getBlockZ()) {
                    newz1 = clickedBlock.getZ();
                } else {
                    newz1 = playerData.getClaimResizing().getMin().getBlockZ();
                }

                if (playerData.getLastShovelLocation().getBlockZ() == playerData.getClaimResizing().getMax().getBlockZ()) {
                    newz2 = clickedBlock.getZ();
                } else {
                    newz2 = playerData.getClaimResizing().getMax().getBlockZ();
                }

                newy1 = Math.min(playerData.getClaimResizing().getMin().getBlockY(), clickedBlock.getY() - wc.getClaimsExtendIntoGroundDistance());
                newy2 = Math.max(playerData.getClaimResizing().getMin().getBlockY(),playerData.getClaimResizing().getMax().getBlockY());

                // for top level claims, apply size rules and claim blocks requirement
                if (playerData.getClaimResizing().getParent() == null) {
                    // measure new claim, apply size rules
                    int newWidth = (Math.abs(newx1 - newx2) + 1);
                    int newLength = (Math.abs(newz1 - newz2) + 1);

                    if (!playerData.getClaimResizing().isAdminClaim() && (newWidth < wc.getMinClaimSize() || newLength < wc.getMinClaimSize())) {
                        plugin.sendMessage(player, TextMode.ERROR, Messages.ResizeClaimTooSmall, String.valueOf(wc.getMinClaimSize()));
                        return;
                    }

                    // make sure player has enough blocks to make up the difference
                    if (!playerData.getClaimResizing().isAdminClaim() && player.getName().equals(playerData.getClaimResizing().getOwnerName())) {
                        int newArea = newWidth * newLength;
                        int blocksRemainingAfter = playerData.getRemainingClaimBlocks() + playerData.getClaimResizing().getArea() - newArea;

                        if (blocksRemainingAfter < 0) {
                            plugin.sendMessage(player, TextMode.ERROR, Messages.ResizeNeedMoreBlocks, String.valueOf(Math.abs(blocksRemainingAfter)));
                            return;
                        }
                    }
                }

                // special rules for making a top-level claim smaller.  to check this, verifying the old claim's corners are inside the new claim's boundaries.
                // rule1: in creative mode, top-level claims can't be moved or resized smaller.
                // rule2: in any mode, shrinking a claim removes any surface fluids
                Claim oldClaim = playerData.getClaimResizing();
                boolean smaller = false;
                if (oldClaim.getParent() == null) {
                    // temporary claim instance, just for checking contains()
                    Claim newClaim = new Claim(plugin,
                            new Location(oldClaim.getMin().getWorld(), newx1, newy1, newz1),
                            new Location(oldClaim.getMin().getWorld(), newx2, newy2, newz2),
                            "", new String[]{}, new String[]{}, new String[]{}, new String[]{}, null, false);

                    // if the new claim is smaller
                    if (!newClaim.contains(oldClaim.getMin(), true, false) || !newClaim.contains(oldClaim.getMax(), true, false)) {
                        smaller = true;

                        // enforce creative mode rule
                        if (!wc.getAllowUnclaim()) {
                            plugin.sendMessage(player, TextMode.ERROR, Messages.NoCreativeUnClaim);
                            return;
                        }

                        // remove surface fluids about to be unclaimed
                        oldClaim.removeSurfaceFluids(newClaim);
                    }
                }

                // ask the datastore to try and resize the claim, this checks for conflicts with other claims
                CreateClaimResult result = plugin.getDataStore().resizeClaim(playerData.getClaimResizing(), newx1, newx2, newy1, newy2, newz1, newz2, player);

                if (result.succeeded == CreateClaimResult.Result.SUCCESS) {
                    // TODO: Raise a ClaimResizeEvent here.

                    // inform and show the player
                    plugin.sendMessage(player, TextMode.SUCCESS, Messages.ClaimResizeSuccess, String.valueOf(playerData.getRemainingClaimBlocks()));
                    Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.CLAIM, player.getLocation());
                    Visualization.apply(plugin, player, visualization);

                    // if resizing someone else's claim, make a log entry
                    if (!playerName.equals(playerData.getClaimResizing().getOwnerName())) {
                        plugin.getLogger().info(playerName + " resized " + playerData.getClaimResizing().getFriendlyOwnerName() + "'s claim at " + GriefPreventionTNG.getfriendlyLocationString(playerData.getClaimResizing().getMin()) + ".");
                    }

                    // if in a creative mode world and shrinking an existing claim, restore any unclaimed area
                    if (smaller && wc.getAutoRestoreUnclaimed() && plugin.creativeRulesApply(oldClaim.getMin())) {
                        plugin.sendMessage(player, TextMode.WARN, Messages.UnclaimCleanupWarning);
                        plugin.restoreClaim(oldClaim, 20L * 60 * 2);  // 2 minutes
                        plugin.getLogger().info(player.getName() + " shrank a claim @ " + GriefPreventionTNG.getfriendlyLocationString(playerData.getClaimResizing().getMin()));
                    }

                    // clean up
                    playerData.setClaimResizing(null);
                    playerData.setLastShovelLocation(null);
                } else if (result.succeeded == CreateClaimResult.Result.CLAIM_OVERLAP) {
                    // inform player
                    plugin.sendMessage(player, TextMode.ERROR, Messages.ResizeFailOverlap);

                    // show the player the conflicting claim
                    Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ERROR_CLAIM, player.getLocation());
                    Visualization.apply(plugin, player, visualization);
                }
                return;
            }

            // otherwise, since not currently resizing a claim, must be starting a resize, creating a new claim, or creating a subdivision
            Claim claim = plugin.getDataStore().getClaimAt(clickedBlock.getLocation(), true /*ignore height*/, playerData.getLastClaim());

            // if within an existing claim, he's not creating a new one
            if (claim != null) {
                // if the player has permission to edit the claim or subdivision
                String noEditReason = claim.allowEdit(player);
                if (noEditReason == null) {
                    // if he clicked on a corner, start resizing it
                    if ((clickedBlock.getX() == claim.getMin().getBlockX() || clickedBlock.getX() == claim.getMax().getBlockX()) && (clickedBlock.getZ() == claim.getMin().getBlockZ() || clickedBlock.getZ() == claim.getMax().getBlockZ())) {
                        playerData.setClaimResizing(claim);
                        playerData.setLastShovelLocation(clickedBlock.getLocation());
                        // TODO: Raise ClaimResizeBegin Event here
                        plugin.sendMessage(player, TextMode.INSTR, Messages.ResizeStart);
                    } else if (playerData.getShovelMode() == ShovelMode.SUBDIVIDE) { // if he didn't click on a corner and is in subdivision mode, he's creating a new subdivision
                        // if it's the first click, he's trying to start a new subdivision
                        if (playerData.getLastShovelLocation() == null) {
                            // if the clicked claim was a subdivision, tell him he can't start a new subdivision here
                            if (claim.getParent() != null) {
                                plugin.sendMessage(player, TextMode.ERROR, Messages.ResizeFailOverlapSubdivision);
                            }

                            // otherwise start a new subdivision
                            else {
                                // RaiseCreateSubdivisionStart
                                plugin.sendMessage(player, TextMode.INSTR, Messages.SubdivisionStart);
                                playerData.setLastShovelLocation(clickedBlock.getLocation());
                                playerData.setClaimSubdividing(claim);
                            }
                        } else { // otherwise, he's trying to finish creating a subdivision by setting the other boundary corner
                            // if last shovel location was in a different world, assume the player is starting the create-claim workflow over
                            if (!playerData.getLastShovelLocation().getWorld().equals(clickedBlock.getWorld())) {
                                playerData.setLastShovelLocation(null);
                                this.onPlayerInteract(event);
                                return;
                            }

                            // try to create a new claim (will return null if this subdivision overlaps another)
                            CreateClaimResult result = plugin.getDataStore().createClaim(
                                    player.getWorld(),
                                    playerData.getLastShovelLocation().getBlockX(), clickedBlock.getX(),
                                    playerData.getLastShovelLocation().getBlockY() - wc.getClaimsExtendIntoGroundDistance(), clickedBlock.getY() - wc.getClaimsExtendIntoGroundDistance(),
                                    playerData.getLastShovelLocation().getBlockZ(), clickedBlock.getZ(),
                                    "--subdivision--",  // owner name is not used for subdivisions
                                    playerData.getClaimSubdividing(), null, false, null, player, true);

                            // if it didn't succeed, tell the player why
                            if (result.succeeded == CreateClaimResult.Result.CLAIM_OVERLAP) {
                                plugin.sendMessage(player, TextMode.ERROR, Messages.CreateSubdivisionOverlap);

                                Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ERROR_CLAIM, player.getLocation());
                                Visualization.apply(plugin, player, visualization);

                                return;
                            } else if (result.succeeded == CreateClaimResult.Result.CANCELED) {
                                // It was canceled by a plugin, just return, as the plugin should put out a 
                                // custom error message.
                                return;
                            } else { // otherwise, advise him on the /trust command and show him his new subdivision
                                plugin.sendMessage(player, TextMode.SUCCESS, Messages.SubdivisionSuccess);
                                Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.CLAIM, player.getLocation());
                                Visualization.apply(plugin, player, visualization);
                                playerData.setLastShovelLocation(null);
                                playerData.setClaimSubdividing(null);
                            }
                        }
                    } else {
                        // otherwise tell him he can't create a claim here, and show him the existing claim
                        // also advise him to consider /abandonclaim or resizing the existing claim
                        plugin.sendMessage(player, TextMode.ERROR, Messages.CreateClaimFailOverlap);
                        Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.CLAIM, player.getLocation());
                        Visualization.apply(plugin, player, visualization);
                    }
                } else { // otherwise tell the player he can't claim here because it's someone else's claim, and show him the claim

                    plugin.sendMessage(player, TextMode.ERROR, Messages.CreateClaimFailOverlapOtherPlayer, claim.getFriendlyOwnerName());
                    Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ERROR_CLAIM, player.getLocation());
                    Visualization.apply(plugin, player, visualization);
                }
                return;
            }

            // otherwise, the player isn't in an existing claim!
            // if he hasn't already start a claim with a previous shovel action
            Location lastShovelLocation = playerData.getLastShovelLocation();
            if (lastShovelLocation == null) {
                // if claims are not enabled in this world and it's not an administrative claim, display an error message and stop
                if (!plugin.claimsEnabledForWorld(player.getWorld()) && playerData.getShovelMode() != ShovelMode.ADMIN) {
                    plugin.sendMessage(player, TextMode.ERROR, Messages.ClaimsDisabledWorld);
                    return;
                } else if (wc.getClaimsPerPlayerLimit() > 0 && !(player.hasPermission("griefprevention.ignoreclaimslimit"))) {
                    // get the number of claims the player has in this world.
                    if (wc.getClaimsPerPlayerLimit() >= playerData.getWorldClaims(clickedBlock.getWorld()).size()) {
                        plugin.sendMessage(player, TextMode.ERROR, Messages.PlayerClaimLimit, String.valueOf(wc.getClaimsPerPlayerLimit()));
                        return;
                    }
                }
                // remember it, and start him on the new claim
                playerData.setLastShovelLocation(clickedBlock.getLocation());
                plugin.sendMessage(player, TextMode.INSTR, Messages.ClaimStart);
                // TODO: raise ClaimCreateStartEvent
                // show him where he's working
                Visualization visualization = Visualization.FromClaim(new Claim(plugin, clickedBlock.getLocation(), clickedBlock.getLocation(), "", new String[]{}, new String[]{}, new String[]{}, new String[]{}, null, false), clickedBlock.getY(), VisualizationType.RESTORE_NATURE, player.getLocation());
                Visualization.apply(plugin, player, visualization);
            } else { // otherwise, he's trying to finish creating a claim by setting the other boundary corner
                // if last shovel location was in a different world, assume the player is starting the create-claim workflow over
                if (!lastShovelLocation.getWorld().equals(clickedBlock.getWorld())) {
                    playerData.setLastShovelLocation(null);
                    this.onPlayerInteract(event);
                    return;
                }

                // apply minimum claim dimensions rule
                int newClaimWidth = Math.abs(playerData.getLastShovelLocation().getBlockX() - clickedBlock.getX()) + 1;
                int newClaimHeight = Math.abs(playerData.getLastShovelLocation().getBlockZ() - clickedBlock.getZ()) + 1;

                if (playerData.getShovelMode() != ShovelMode.ADMIN && (newClaimWidth < wc.getMinClaimSize() || newClaimHeight < wc.getMinClaimSize())) {
                    plugin.sendMessage(player, TextMode.ERROR, Messages.NewClaimTooSmall, String.valueOf(wc.getMinClaimSize()));
                    return;
                }

                // if not an administrative claim, verify the player has enough claim blocks for this new claim
                if (playerData.getShovelMode() != ShovelMode.ADMIN) {
                    int newClaimArea = newClaimWidth * newClaimHeight;
                    int remainingBlocks = playerData.getRemainingClaimBlocks();
                    if (newClaimArea > remainingBlocks) {
                        plugin.sendMessage(player, TextMode.ERROR, Messages.CreateClaimInsufficientBlocks, String.valueOf(newClaimArea - remainingBlocks));
                        plugin.sendMessage(player, TextMode.INSTR, Messages.AbandonClaimAdvertisement);
                        return;
                    }
                } else {
                    playerName = "";
                }

                // try to create a new claim (will return null if this claim overlaps another)
                CreateClaimResult result = plugin.getDataStore().createClaim(
                        player.getWorld(),
                        lastShovelLocation.getBlockX(), clickedBlock.getX(),
                        lastShovelLocation.getBlockY() - wc.getClaimsExtendIntoGroundDistance(), clickedBlock.getY() - wc.getClaimsExtendIntoGroundDistance(),
                        lastShovelLocation.getBlockZ(), clickedBlock.getZ(),
                        playerName, null, null, false, null, player, true);

                // if it didn't succeed, tell the player why
                if (result.succeeded == CreateClaimResult.Result.CLAIM_OVERLAP) {
                    // if the claim it overlaps is owned by the player...
                    System.out.println("Claim owned by:" + result.claim.getFriendlyOwnerName());
                    if (playerName.equals(result.claim.getOwnerName())) {
                        // owned by the player. make sure our larger 
                        // claim entirely contains the smaller one.

                        if ((Claim.contains(lastShovelLocation, clickedBlock.getLocation(), result.claim.getMin(), true) &&
                                (Claim.contains(lastShovelLocation, clickedBlock.getLocation(), result.claim.getMax(), true)))) {
                            // it contains it
                            // resize the other claim
                            result.claim.setLocation(lastShovelLocation, clickedBlock.getLocation());

                            // msg, and show visualization.
                            plugin.sendMessage(player, TextMode.SUCCESS, Messages.ClaimResizeSuccess, String.valueOf(playerData.getRemainingClaimBlocks()));
                            Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.CLAIM, player.getLocation());
                            Visualization.apply(plugin, player, visualization);
                            return;
                        }
                    }
                    plugin.sendMessage(player, TextMode.ERROR, Messages.CreateClaimFailOverlapShort);

                    Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ERROR_CLAIM, player.getLocation());
                    Visualization.apply(plugin, player, visualization);

                } else if (result.succeeded == CreateClaimResult.Result.CANCELED) {
                    // A plugin canceled the event.

                } else { // otherwise, advise him on the /trust command and show him his new claim
                    plugin.sendMessage(player, TextMode.SUCCESS, Messages.CreateClaimSuccess);
                    Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.CLAIM, player.getLocation());
                    Visualization.apply(plugin, player, visualization);
                    playerData.setLastShovelLocation(null);
                }
            }
        }
    }
}
