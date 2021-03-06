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

package com.norcode.bukkit.griefprevention.listeners;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.configuration.ClaimBehaviourData;
import com.norcode.bukkit.griefprevention.configuration.MaterialInfo;
import com.norcode.bukkit.griefprevention.configuration.WorldConfig;
import com.norcode.bukkit.griefprevention.data.Claim;
import com.norcode.bukkit.griefprevention.data.PlayerData;
import com.norcode.bukkit.griefprevention.messages.Messages;
import com.norcode.bukkit.griefprevention.messages.TextMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;

// handles events related to entities
public class EntityListener implements Listener {
    // convenience reference for the singleton datastore
    GriefPreventionTNG plugin;
    private EnumSet<EntityType> FARM_ANIMALS = EnumSet.of(EntityType.PIG, EntityType.CHICKEN, EntityType.COW,
            EntityType.SHEEP, EntityType.VILLAGER, EntityType.MUSHROOM_COW, EntityType.WOLF, EntityType.OCELOT);

    public EntityListener(GriefPreventionTNG griefPreventionTNG) {
        this.plugin = griefPreventionTNG;
    }

    private Claim ChangeBlockClaimCache = null;

    // don't allow endermen to change blocks
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        WorldConfig wc = plugin.getWorldCfg(event.getEntity().getWorld());

        if (!wc.endermenMoveBlocks() && event.getEntityType() == EntityType.ENDERMAN) {
            event.setCancelled(true);
        } else if (!wc.getSilverfishBreakBlocks() && event.getEntityType() == EntityType.SILVERFISH) {
            event.setCancelled(true);
        }

        // don't allow the wither to break blocks, when the wither is determined, too expensive to constantly check for claimed blocks
        else if (event.getEntityType() == EntityType.WITHER && wc.getClaimsEnabled()) {
            event.setCancelled(wc.getWitherEatBehaviour().allowed(event.getEntity().getLocation(), null).denied());
        }
    }

    // don't allow zombies to break down doors
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onZombieBreakDoor(EntityBreakDoorEvent event) {
        WorldConfig wc = plugin.getWorldCfg(event.getEntity().getWorld());
        if (!wc.getZombieDoorBreaking().allowed(event.getEntity().getLocation(), null).allowed()) {
            event.setCancelled(true);
        }
    }

    // don't allow entities to trample crops
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityInteract(EntityInteractEvent event) {
        WorldConfig wc = plugin.getWorldCfg(event.getEntity().getWorld());
        if (!wc.creaturesTrampleCrops() && event.getBlock().getType() == Material.SOIL) {
            event.setCancelled(true);
        }
        if (wc.getClaimsPreventButtonsSwitches() && event.getBlock().getType() == Material.WOOD_BUTTON && event.getEntity().getType() == EntityType.ARROW) {
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();
                Claim claim = plugin.getDataStore().getClaimAt(event.getBlock().getLocation(), false, null);
                if (claim != null) {
                    String noAccessReason = plugin.getPlayerListener().allowAccess(shooter, claim, event);
                    if (noAccessReason != null) {
                        plugin.sendMessage(shooter, TextMode.ERROR, noAccessReason);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent explodeEvent) {
        List<Block> blocks = explodeEvent.blockList();
        Location location = explodeEvent.getLocation();
        WorldConfig wc = plugin.getWorldCfg(location.getWorld());

        // logic: we have Creeper and TNT Explosions currently. each one has special configuration options.
        // make sure that we are allowed to explode, first.
        Entity explodingEntity = explodeEvent.getEntity();
        boolean isCreeper = explodingEntity != null && explodingEntity instanceof Creeper;
        boolean isTNT = explodingEntity != null && (explodingEntity instanceof TNTPrimed || explodingEntity instanceof ExplosiveMinecart);
        boolean isWither = explodingEntity != null && (explodingEntity instanceof WitherSkull || explodingEntity instanceof Wither);

        ClaimBehaviourData useBehaviour = null;
        if (isCreeper) {
            useBehaviour = wc.getCreeperExplosionBehaviour();
        } else if (isWither) {
            useBehaviour = wc.getWitherExplosionBehaviour();
        } else if (isTNT) {
            useBehaviour = wc.getTntExplosionBehaviour();
        } else {
            useBehaviour = wc.getOtherExplosionBehaviour();
        }
        Claim claimPos = null;
        // //go through each block that was affected...
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            if (wc.getModsExplodableIds().contains(new MaterialInfo(block.getTypeId(), block.getData(), null)))
                continue;
            // creative rules stop all explosions, regardless of the other settings.
            if (wc.getCreativeRules() || (useBehaviour != null && useBehaviour.allowed(block.getLocation(), null).denied())) {
                // if not allowed. remove it...
                blocks.remove(i--);
            } else {
                // it is allowed, however, if it is on a claim only allow if explosions are enabled for that claim.
                claimPos = plugin.getDataStore().getClaimAt(block.getLocation(), false, claimPos);
                if (claimPos != null && !claimPos.isExplosivesAllowed()) {
                    blocks.remove(i--);
                } else if (block.getType() == Material.LOG) {
                    plugin.getBlockListener().handleLogBroken(block);
                }
            }
        }
    }

    // when an item spawns...
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        // precheck: always allow Droppers to drop items when triggered.
        // We do this by seeing of there is a Dropper within a few blocks of the spawned item.

        Block centerblock = event.getEntity().getLocation().getBlock();
        for (int testx = -1; testx <= 1; testx++) {
            for (int testy = -1; testy <= 1; testy++) {
                for (int testz = -1; testz <= 1; testz++) {
                    Block grabblock = event.getEntity().getWorld().
                            getBlockAt(centerblock.getX() + testx,
                                    centerblock.getY() + testy,
                                    centerblock.getZ() + testz);
                    if (grabblock.getType().equals(Material.DROPPER)) {
                        return;
                    }
                }
            }
        }
        // if in a creative world, cancel the event (don't drop items on the ground)
        if (plugin.creativeRulesApply(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    // when an experience bottle explodes...
    @EventHandler(priority = EventPriority.LOWEST)
    public void onExpBottle(ExpBottleEvent event) {
        // if in a creative world, cancel the event (don't drop exp on the ground)
        if (plugin.creativeRulesApply(event.getEntity().getLocation())) {
            event.setExperience(0);
        }
    }

    // when a creature spawns...
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        WorldConfig wc = plugin.getWorldCfg(entity.getWorld());
        // these rules apply only to creative worlds
        // chicken eggs and breeding could potentially make a mess in the wilderness, once griefers get involved
        SpawnReason reason = event.getSpawnReason();

        if (reason == SpawnReason.BUILD_WITHER) {
            // can we build a wither?
            if (wc.getWitherSpawnBehaviour().allowed(entity.getLocation(), null).denied()) {
                event.setCancelled(true);
                return;
            }
        } else if (reason == SpawnReason.BUILD_SNOWMAN) {
            // can we build a snowman?
            if (wc.getSnowGolemSpawnBehaviour().allowed(entity.getLocation(), null).denied()) {
                event.setCancelled(true);
                return;
            }
        } else if (reason == SpawnReason.BUILD_IRONGOLEM) {
            if (wc.getIronGolemSpawnBehaviour().allowed(entity.getLocation(), null).denied()) {
                event.setCancelled(true);
                return;
            }
        } else if (reason == SpawnReason.BREEDING || reason == SpawnReason.EGG || reason == SpawnReason.SPAWNER_EGG) {
            if (FARM_ANIMALS.contains(entity.getType())) {
                if (wc.getAnimalBreedingRules().allowed(entity.getLocation(), null).denied()) {
                    event.setCancelled(true);
                    return;
                }
                int count = 0;
                for (Entity e: entity.getNearbyEntities(8, 8, 8)) {
                    if (e.getType() == entity.getType()) {
                        count++;
                    }
                }
                if (count > wc.getAnimalCrowdingLimit()) {
                    event.setCancelled(true);
                    return;
                }

            }
        }

        if (!plugin.creativeRulesApply(entity.getLocation())) return;

        // otherwise, just apply the limit on total entities per claim
        Claim claim = plugin.getDataStore().getClaimAt(event.getLocation(), false, null);
        if (claim != null && claim.allowMoreEntities() != null) {
            event.setCancelled(true);
            return;
        }
        if (reason != SpawnReason.SPAWNER_EGG && reason != SpawnReason.BUILD_IRONGOLEM && reason != SpawnReason.BUILD_SNOWMAN) {
            event.setCancelled(true);
            return;
        }
    }

    // when an entity dies...
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // special rule for creative worlds: killed entities don't drop items or experience orbs
        if (plugin.creativeRulesApply(entity.getLocation())) {
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
        if (!(entity instanceof Player)) return;  // only tracking players

        Player player = (Player) entity;
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
    }

    // when an entity picks up an item
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPickup(EntityChangeBlockEvent event) {
        // FEATURE: endermen don't steal claimed blocks

        // if its an enderman
        if (event.getEntity() instanceof Enderman) {
            // and the block is claimed
            if (plugin.getDataStore().getClaimAt(event.getBlock().getLocation(), false, null) != null) {
                // he doesn't get to steal it
                event.setCancelled(true);
            }
        }
    }

    // when a painting is broken
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onHangingBreak(HangingBreakEvent event) {
        // FEATURE: claimed paintings are protected from breakage

        // only allow players to break paintings, not anything else (like water and explosions)
        if (!(event instanceof HangingBreakByEntityEvent)) {
            event.setCancelled(true);
            return;
        }

        HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent) event;

        // who is removing it?
        Entity remover = entityEvent.getRemover();

        // again, making sure the breaker is a player
        if (!(remover instanceof Player)) {
            event.setCancelled(true);
            return;
        }

        // if the player doesn't have build permission, don't allow the breakage
        Player playerRemover = (Player) entityEvent.getRemover();
        String noBuildReason = plugin.getBlockListener().allowBuild(playerRemover, event.getEntity().getLocation(), plugin.getDataStore().getPlayerData(playerRemover.getName()), null, event);
        if (noBuildReason != null) {
            event.setCancelled(true);
            plugin.sendMessage(playerRemover, TextMode.ERROR, noBuildReason);
        }
    }

    // when a painting is placed...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPaintingPlace(HangingPlaceEvent event) {
        // FEATURE: similar to above, placing a painting requires build permission in the claim

        // if the player doesn't have permission, don't allow the placement
        String noBuildReason = plugin.getBlockListener().allowBuild(event.getPlayer(), event.getEntity().getLocation(), plugin.getDataStore().getPlayerData(event.getPlayer().getName()), null, event);
        if (noBuildReason != null) {
            event.setCancelled(true);
            plugin.sendMessage(event.getPlayer(), TextMode.ERROR, noBuildReason);
        } else if (plugin.creativeRulesApply(event.getEntity().getLocation())) {
            // otherwise, apply entity-count limitations for creative worlds
            PlayerData playerData = plugin.getDataStore().getPlayerData(event.getPlayer().getName());
            Claim claim = plugin.getDataStore().getClaimAt(event.getBlock().getLocation(), false, playerData.getLastClaim());
            if (claim == null) return;

            String noEntitiesReason = claim.allowMoreEntities();
            if (noEntitiesReason != null) {
                plugin.sendMessage(event.getPlayer(), TextMode.ERROR, noEntitiesReason);
                event.setCancelled(true);
                return;
            }
        }
    }

    // when an entity is damaged
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        // environmental damage
        if (event.getEntity() instanceof Hanging) { // hanging objects are not destroyed by explosions inside claims.
            Claim claimatpos = plugin.getDataStore().getClaimAt(event.getEntity().getLocation(), false, null);
            if (claimatpos != null) {
                if (!claimatpos.isExplosivesAllowed()) {
                    event.setCancelled(true);
                }
            }

        }

        if (!(event instanceof EntityDamageByEntityEvent)) return;
        WorldConfig wc = plugin.getWorldCfg(event.getEntity().getWorld());
        // monsters are never protected
        if (event.getEntity() instanceof Monster) return;

        EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

        // determine which player is attacking, if any
        Player attacker = null;
        Projectile projectile = null;
        Entity damageSource = subEvent.getDamager();
        if (damageSource instanceof Player) {
            attacker = (Player) damageSource;
        } else if (damageSource instanceof Projectile) {
            projectile = (Projectile) damageSource;
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        // if the attacker is a player and defender is a player (pvp combat)
        if (attacker != null && event.getEntity() instanceof Player && event.getEntity().getWorld().getPVP()) {
            // FEATURE: prevent pvp in the first minute after spawn, and prevent pvp when one or both players have no inventory

            // doesn't apply when the attacker has the no pvp immunity permission
            // this rule is here to allow server owners to have a world with no spawn camp protection by assigning permissions based on the player's world
            if (attacker.hasPermission("griefprevention.nopvpimmunity")) return;

            Player defender = (Player) (event.getEntity());

            PlayerData defenderData = plugin.getDataStore().getPlayerData(((Player) event.getEntity()).getName());
            PlayerData attackerData = plugin.getDataStore().getPlayerData(attacker.getName());

            // otherwise if protecting spawning players
            if (wc.getProtectFreshSpawns()) {
                if (defenderData.isPvpImmune()) {
                    event.setCancelled(true);
                    plugin.sendMessage(attacker, TextMode.ERROR, Messages.ThatPlayerPvPImmune);
                    return;
                }

                if (attackerData.isPvpImmune()) {
                    event.setCancelled(true);
                    plugin.sendMessage(attacker, TextMode.ERROR, Messages.CantFightWhileImmune);
                    return;
                }
            }

            // FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
            if (wc.getPvPNoCombatInPlayerClaims() || wc.getNoPvPCombatInAdminClaims()) {
                Claim attackerClaim = plugin.getDataStore().getClaimAt(attacker.getLocation(), false, attackerData.getLastClaim());
                if (attackerClaim != null &&
                        (attackerClaim.isAdminClaim() && wc.getNoPvPCombatInAdminClaims() ||
                                !attackerClaim.isAdminClaim() && wc.getPvPNoCombatInPlayerClaims())) {
                    attackerData.setLastClaim(attackerClaim);
                    event.setCancelled(true);
                    plugin.sendMessage(attacker, TextMode.ERROR, Messages.CantFightWhileImmune);
                    return;
                }

                Claim defenderClaim = plugin.getDataStore().getClaimAt(defender.getLocation(), false, defenderData.getLastClaim());
                if (defenderClaim != null &&
                        (defenderClaim.isAdminClaim() && wc.getNoPvPCombatInAdminClaims() ||
                                !defenderClaim.isAdminClaim() && wc.getPvPNoCombatInPlayerClaims())) {
                    defenderData.setLastClaim(defenderClaim);
                    event.setCancelled(true);
                    plugin.sendMessage(attacker, TextMode.ERROR, Messages.PlayerInPvPSafeZone);
                    return;
                }
            }

            // FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
            // FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated

            long now = Calendar.getInstance().getTimeInMillis();
            defenderData.setLastPvpTimestamp(now);
            defenderData.setLastPvpPlayer(attacker.getName());
            attackerData.setLastPvpTimestamp(now);
            attackerData.setLastPvpPlayer(defender.getName());
        }

        // FEATURE: protect claimed animals, boats, minecarts
        // NOTE: animals can be lead with wheat, vehicles can be pushed around.
        // so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway

        // if theft protection is enabled
        if (event instanceof EntityDamageByEntityEvent) {
            // if the entity is an non-monster creature (remember monsters disqualified above), or a vehicle
            //
            if ((subEvent.getEntity() instanceof Creature && wc.getClaimsProtectCreatures())) {

                Claim cachedClaim = null;
                PlayerData playerData = null;
                if (attacker != null) {
                    playerData = plugin.getDataStore().getPlayerData(attacker.getName());
                    cachedClaim = playerData.getLastClaim();
                }

                Claim claim = plugin.getDataStore().getClaimAt(event.getEntity().getLocation(), false, cachedClaim);

                // if it's claimed
                if (claim != null) {
                    // if damaged by anything other than a player (exception villagers injured by zombies in admin claims), cancel the event
                    // why exception?  so admins can set up a village which can't be CHANGED by players, but must be "protected" by players.
                    // Additional exception added: cactus, lava, and drowning of entities happens.
                    if (attacker == null) {
                        // exception case
                        if (event.getEntity() instanceof Villager && damageSource instanceof Monster && claim.isAdminClaim()) {
                            return;
                        } else if (event.getCause().equals(DamageCause.CONTACT) || event.getCause().equals(DamageCause.DROWNING)) {
                            return;
                        }
                        // all other cases
                        else {
                            event.setCancelled(true);
                        }
                    }

                    // otherwise the player damaging the entity must have permission,
                    else {
                        String noContainersReason = claim.allowContainers(attacker);
                        if (noContainersReason != null) {
                            event.setCancelled(true);
                            // kill the arrow to avoid infinite bounce between crowded together animals
                            if (projectile != null) {
                                if (projectile instanceof Arrow) {
                                    projectile.remove();
                                }
                            }
                            plugin.sendMessage(attacker, TextMode.ERROR, Messages.NoDamageClaimedEntity, claim.getFriendlyOwnerName());
                        }
                        // cache claim for later
                        if (playerData != null) {
                            playerData.setLastClaim(claim);
                        }
                    }
                }
            }
        }
    }

    // when a vehicle is damaged
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onVehicleDamage(VehicleDamageEvent event) {
        WorldConfig wc = new WorldConfig(plugin, event.getVehicle().getWorld());

        // all of this is anti theft code
        if (!wc.getClaimsPreventTheft()) return;

        // determine which player is attacking, if any
        Player attacker = null;
        Entity damageSource = event.getAttacker();
        if (damageSource instanceof Player) {
            attacker = (Player) damageSource;
        } else if (damageSource instanceof Arrow) {
            Arrow arrow = (Arrow) damageSource;
            if (arrow.getShooter() instanceof Player) {
                attacker = (Player) arrow.getShooter();
            }
        } else if (damageSource instanceof ThrownPotion) {
            ThrownPotion potion = (ThrownPotion) damageSource;
            if (potion.getShooter() instanceof Player) {
                attacker = (Player) potion.getShooter();
            }
        }
        // if Damage source is unspecified and we allow environmental damage, don't cancel the event.
        else if (damageSource == null && wc.getEnvironmentalVehicleDamage().allowed(event.getVehicle().getLocation(), attacker).allowed()) {
            return;
        }
        // NOTE: vehicles can be pushed around.
        // so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
        Claim cachedClaim = null;
        PlayerData playerData = null;
        if (attacker != null) {
            playerData = plugin.getDataStore().getPlayerData(attacker.getName());
            cachedClaim = playerData.getLastClaim();
        }

        Claim claim = plugin.getDataStore().getClaimAt(event.getVehicle().getLocation(), false, cachedClaim);

        // if it's claimed
        if (claim != null) {
            // if damaged by anything other than a player, or a cactus,
            if (attacker == null) {
                event.setCancelled(true);
            }

            // otherwise the player damaging the entity must have permission
            else {
                String noContainersReason = claim.allowContainers(attacker);
                if (noContainersReason != null) {
                    event.setCancelled(true);
                    plugin.sendMessage(attacker, TextMode.ERROR, Messages.NoDamageClaimedEntity, claim.getFriendlyOwnerName());
                }

                // cache claim for later
                if (playerData != null) {
                    playerData.setLastClaim(claim);
                }
            }
        }
    }
}
