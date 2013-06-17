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

package me.ryanhamshire.GriefPrevention.data;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.SerializationUtil;
import me.ryanhamshire.GriefPrevention.configuration.ClaimPermission;
import me.ryanhamshire.GriefPrevention.configuration.PlayerGroup;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import me.ryanhamshire.GriefPrevention.exceptions.InvalidFlagValueException;
import me.ryanhamshire.GriefPrevention.flags.BaseFlag;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.tasks.RestoreNatureProcessingTask;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

// represents a player claim
// creating an instance doesn't make an effective claim
// only claims which have been added to the datastore have any effect
public class Claim {

    // String representations of the lesser and Greater Boundary corners.
    // these are ONLY used when passed in the constructor.
    // an Attempt is made to parse them and if the world isn't loaded the claim will be
    // "dormant" which means it will return false for most queries.
    private GriefPrevention plugin;
    private HashMap<String, PluginClaimMeta> claimMeta = new HashMap<String, PluginClaimMeta>();
    private HashMap<String, String> flags = new HashMap<String, String>();

    // two locations, which together define the boundaries of the claim
    // note that the upper Y value is always ignored, because claims ALWAYS extend up to the sky
    Location min;
    Location max;

    /**
     * modification date.  this comes from the file timestamp during load, and is updated with runtime changes
     */
    private long modifiedDate;
    private UUID id = null;

    /**
     * ownername.  for admin claims, this is the empty string
     * use getOwnerName() to get a friendly name (will be "an administrator" for admin claims)
     */
    private String ownerName;

    /**
     * list of players who (beyond the claim owner) have permission to grant permissions in this claim
     */
    private ArrayList<String> managers = new ArrayList<String>();

    // permissions for this claim, see ClaimPermission class
    private HashMap<String, ClaimPermission> playerNameToClaimPermissionMap = new HashMap<String, ClaimPermission>();

    /**
     * whether or not this claim is in the data store
     * if a claim instance isn't in the data store, it isn't "active" - players can't interact with it
     * why keep this?  so that claims which have been removed from the data store can be correctly
     * ignored even though they may have references floating around
     */
    private boolean inDataStore = false;

    /**
     * self-explanatory: whether Explosives can affect this claim. This is an additional requirement in addition to
     * the world configuration allowing for Explosions within claims either above or below sea-level.
     */
    private boolean explosivesAllowed = false;

    /**
     * parent claim
     * only used for claim subdivisions.  top level claims have null here
     */
    private Claim parent = null;

    /**
     * children (subdivisions)
     * note subdivisions themselves never have children
     */
    private ArrayList<Claim> children = new ArrayList<Claim>();

    /**
     * This variable sets whether a claim gets deleted with the automatic cleanup.
     */
    private boolean neverDelete = false;

    /**
     * Whether or not this is an administrative claim.<br />
     * Administrative claims are created and maintained by players with the griefprevention.adminclaims permission.
     *
     * @return
     */
    public boolean isAdminClaim() {
        return (this.ownerName == null || this.ownerName.isEmpty());
    }

    // accessor for ID
    public UUID getId() {
        return this.id;
    }

    // basic constructor, just notes the creation time
    // see above declarations for other defaults
    Claim() {
        this.modifiedDate = System.currentTimeMillis();
    }

    /**
     * Removes any fluids above sea level in a claim.
     *
     * @param exclusionClaim another claim indicating a sub-area to be excluded from this operation. Can be null.
     */
    public void removeSurfaceFluids(Claim exclusionClaim) {
        WorldConfig wc = plugin.getWorldCfg(getMin().getWorld());

        // don't do this for administrative claims
        if (this.isAdminClaim()) return;

        // don't do it for very large claims
        if (this.getArea() > 10000) return;

        // don't do it when surface fluids are allowed to be dumped
        if (wc.getWaterBucketBehaviour().allowed(getMin(), null).denied())
            return;

        Location lesser = this.getMin();
        Location greater = this.getMax();

        if (lesser.getWorld().getEnvironment() == Environment.NETHER) return;  // don't clean up lava in the nether

        int seaLevel = 0;  // clean up all fluids in the end

        // respect sea level in normal worlds
        if (lesser.getWorld().getEnvironment() == Environment.NORMAL)
            seaLevel = plugin.getWorldCfg(lesser.getWorld()).getSeaLevelOverride();

        for (int x = lesser.getBlockX(); x <= greater.getBlockX(); x++) {
            for (int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++) {
                for (int y = seaLevel - 1; y <= lesser.getWorld().getMaxHeight(); y++) {
                    // dodge the exclusion claim
                    Block block = lesser.getWorld().getBlockAt(x, y, z);
                    if (exclusionClaim != null && exclusionClaim.contains(block.getLocation(), true, false)) continue;

                    if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA || block.getType() == Material.LAVA || block.getType() == Material.WATER) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    // determines whether or not a claim has surface fluids (lots of water blocks, or any lava blocks)
    // used to warn players when they abandon their claims about automatic fluid cleanup
    public boolean hasSurfaceFluids() {
        Location lesser = this.getMin();
        Location greater = this.getMax();

        // don't bother for very large claims, too expensive
        if (this.getArea() > 10000) return false;

        int seaLevel = 0;  // clean up all fluids in the end

        // respect sea level in normal worlds
        if (lesser.getWorld().getEnvironment() == Environment.NORMAL)
            seaLevel = plugin.getWorldCfg(lesser.getWorld()).getSeaLevelOverride();

        int waterCount = 0;
        for (int x = lesser.getBlockX(); x <= greater.getBlockX(); x++) {
            for (int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++) {
                for (int y = seaLevel - 1; y <= lesser.getWorld().getMaxHeight(); y++) {
                    // dodge the exclusion claim
                    Block block = lesser.getWorld().getBlockAt(x, y, z);

                    if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.WATER) {
                        waterCount++;
                        if (waterCount > 10) return true;
                    } else if (block.getType() == Material.STATIONARY_LAVA || block.getType() == Material.LAVA) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Main constructor.  Note that only creating a claim instance does nothing - a claim must be added to the data store to be effective.
     *
     * @param min
     * @param max
     * @param ownerName
     * @param builderNames
     * @param containerNames
     * @param accessorNames
     * @param managerNames
     * @param id
     * @param neverDelete
     */
    public Claim(GriefPrevention plugin, Location min, Location max, String ownerName, String[] builderNames, String[] containerNames, String[] accessorNames, String[] managerNames, UUID id, boolean neverDelete) {
        
        this.plugin = plugin;
        
        // modification date
        this.modifiedDate = System.currentTimeMillis();

        // id
        this.id = id;

        // store corners
        this.min = min;
        this.max = max;

        // owner
        this.ownerName = ownerName;

        // other permissions
        for (String name : builderNames) {
            if (name != null && !name.isEmpty()) {
                this.playerNameToClaimPermissionMap.put(name, ClaimPermission.BUILD);
            }
        }

        for (String name : containerNames) {
            if (name != null && !name.isEmpty()) {
                this.playerNameToClaimPermissionMap.put(name, ClaimPermission.INVENTORY);
            }
        }

        for (String name : accessorNames) {
            if (name != null && !name.isEmpty()) {
                this.playerNameToClaimPermissionMap.put(name, ClaimPermission.ACCESS);
            }
        }

        for (String name : managerNames) {
            if (name != null && !name.isEmpty()) {
                this.managers.add(name);
            }
        }

        this.neverDelete = neverDelete;
    }

    /**
     * Measurements.  All measurements are in blocks
     *
     * @return
     */
    public int getArea() {
        int claimWidth = this.max.getBlockX() - this.min.getBlockX() + 1;
        int claimHeight = this.max.getBlockZ() - this.min.getBlockZ() + 1;

        return claimWidth * claimHeight;
    }

    public void setLocation(Location FirstBorder, Location SecondBorder) {
        if (FirstBorder.getWorld() != SecondBorder.getWorld()) return;
        Location pA = FirstBorder;
        Location pB = SecondBorder;
        int MinX = Math.min(pA.getBlockX(), pB.getBlockX());
        int MinY = Math.min(pA.getBlockY(), pB.getBlockY());
        int MinZ = Math.min(pA.getBlockZ(), pB.getBlockZ());
        int MaxX = Math.max(pA.getBlockX(), pB.getBlockX());
        int MaxY = Math.max(pA.getBlockY(), pB.getBlockY());
        int MaxZ = Math.max(pA.getBlockZ(), pB.getBlockZ());
        Location FirstPos = new Location(FirstBorder.getWorld(), MinX, MinY, MinZ);
        Location SecondPos = new Location(FirstBorder.getWorld(), MaxX, MaxY, MaxZ);
        min = FirstPos;
        max = SecondPos;


    }

    /**
     * Gets the width of the claim.
     *
     * @return
     */
    public int getWidth() {
        return this.max.getBlockX() - this.min.getBlockX() + 1;
    }

    /**
     * Gets the height of the claim.
     *
     * @return
     */
    public int getHeight() {
        return this.max.getBlockZ() - this.min.getBlockZ() + 1;
    }

    /**
     * Distance check for claims, distance in this case is a band around the outside of the claim rather then euclidean distance.
     *
     * @param location
     * @param howNear
     * @return
     */
    public boolean isNear(Location location, int howNear) {
        Claim claim = new Claim(plugin, new Location(this.min.getWorld(), this.min.getBlockX() - howNear, this.min.getBlockY(), this.min.getBlockZ() - howNear),
                        new Location(this.max.getWorld(), this.max.getBlockX() + howNear, this.max.getBlockY(), this.max.getBlockZ() + howNear),
                        "", new String[]{}, new String[]{}, new String[]{}, new String[]{}, null, false);

        return claim.contains(location, false, true);
    }

    /**
     * Permissions.  Note administrative "public" claims have different rules than other claims.<br />
     * All of these return NULL when a player has permission, or a String error message when the player doesn't have permission.
     *
     * @param player
     * @return
     */
    public String allowEdit(Player player) {
        // if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
        if (player == null) return "";

        // special cases...

        // admin claims need adminclaims permission only.
        if (this.isAdminClaim()) {
            if (player.hasPermission("griefprevention.adminclaims")) return null;
        }

        // anyone with deleteclaims permission can modify non-admin claims at any time
        else {
            if (player.hasPermission("griefprevention.deleteclaims")) return null;
        }

        if (this.ownerName.equals(player.getName())) {
            return null;
        }

        // permission inheritance for subdivisions
        if (this.parent != null)
            return this.parent.allowBuild(player);

        // error message if all else fails
        return plugin.getMessageManager().getMessage(Messages.OnlyOwnersModifyClaims, this.getOwnerName());
    }

    /**
     * Build permission check
     *
     * @param player
     * @return
     */
    public String allowBuild(Player player) {
        // if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
        if (player == null) return "";

        // admin claims can always be modified by admins, no exceptions
        if (this.isAdminClaim()) {
            if (player.hasPermission("griefprevention.adminclaims")) return null;
        }

        // no building while in pvp combat
        PlayerData playerData = plugin.getDataStore().getPlayerData(player.getName());
        if (playerData.inPvpCombat()) {
            return plugin.getMessageManager().getMessage(Messages.NoBuildPvP);
        }

        // owners can make changes, or admins with ignore claims mode enabled
        if (this.ownerName.equals(player.getName()) || plugin.getDataStore().getPlayerData(player.getName()).isIgnoreClaims())
            return null;

        // anyone with explicit build permission can make changes
        if (this.hasExplicitPermission(player, ClaimPermission.BUILD)) return null;

        // also everyone is a member of the "public", so check for public permission
        ClaimPermission permissionLevel = this.playerNameToClaimPermissionMap.get("public");
        if (ClaimPermission.BUILD == permissionLevel) return null;

        // subdivision permission inheritance
        if (this.parent != null)
            return this.parent.allowBuild(player);

        // failure message for all other cases
        String reason = plugin.getMessageManager().getMessage(Messages.NoBuildPermission, this.getOwnerName());
        if (player.hasPermission("griefprevention.ignoreclaims"))
            reason += "  " + plugin.getMessageManager().getMessage(Messages.IgnoreClaimsAdvertisement);
        return reason;
    }

    /**
     * returns whether the given player name fits the given identifier.
     * this is added to allow for Group Permissions on a claim.
     *
     * @param identifier name of player, or a name of a group. group names must be prefixed with g:
     * @param pName      name of player to test.
     * @return
     */
    private boolean isApplicablePlayer(String identifier, String pName) {
        if (identifier.equalsIgnoreCase(pName)) return true;
        if (identifier.toUpperCase().startsWith("G:")) {
            identifier = identifier.substring(2);
            // try to get the player (pName).

            // try to get this group from the GP instance PlayerGroups cfg.
            PlayerGroup FoundGroup = plugin.configuration.getPlayerGroups().getGroupByName(identifier);
            if (FoundGroup == null) return false; // group not found. Well THIS is awkward.

            return FoundGroup.MatchPlayer(pName);
        }
        return false;
    }

    private boolean hasExplicitPermission(Player player, ClaimPermission level) {
        String playerName = player.getName();
        Set<String> keys = this.playerNameToClaimPermissionMap.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String identifier = iterator.next();

            boolean forcedeny = false;
            // special logic: names starting with ! mean to explicitly deny that permission to any player that matches the name after !.
            // in order to allow group ability, we have this flag. if forcedeny is false, a match means
            // we need to forcibly return false if it matches.
            if (identifier.startsWith("!")) {
                // if it starts with a exclamation, remove it and set forcedeny to true.
                identifier = identifier.substring(1);
                forcedeny = true;
            }


            if (isApplicablePlayer(identifier, playerName) && this.playerNameToClaimPermissionMap.get(identifier) == level) {
                // it matches. if we started with a !, however, it means that we are to explicitly
                // DENY that permission. Otherwise, we return true.
                return !forcedeny;

            } else if (identifier.startsWith("[") && identifier.endsWith("]")) {
                // drop the brackets
                String permissionIdentifier = identifier.substring(1, identifier.length() - 1);

                // defensive coding
                if (permissionIdentifier == null || permissionIdentifier.isEmpty()) continue;

                // check permission
                if (player.hasPermission(permissionIdentifier) && this.playerNameToClaimPermissionMap.get(identifier) == level)
                    return !forcedeny;
            }
        }
        return false;
    }

    public String allowBreak(Player player, Location l) {
        return allowBreak(player, l.getBlock());
    }

    /**
     * Break permission check
     *
     * @param player
     * @param BlocktoCheck
     * @return
     */
    public String allowBreak(Player player, Block BlocktoCheck) {
        Material material = BlocktoCheck.getType();
        WorldConfig wc = plugin.getWorldCfg(player.getWorld());
        return this.allowBuild(player);
    }

    /**
     * Access permission check
     *
     * @param player
     * @return
     */
    public String allowAccess(Player player) {
        // admin claims need adminclaims permission only.
        if (this.isAdminClaim()) {
            if (player.hasPermission("griefprevention.adminclaims")) return null;
        }

        // claim owner and admins in ignoreclaims mode have access
        if (this.ownerName.equals(player.getName()) || plugin.getDataStore().getPlayerData(player.getName()).isIgnoreClaims())
            return null;

        // look for explicit individual access, inventory, or build permission
        if (this.hasExplicitPermission(player, ClaimPermission.ACCESS)) return null;
        if (this.hasExplicitPermission(player, ClaimPermission.INVENTORY)) return null;
        if (this.hasExplicitPermission(player, ClaimPermission.BUILD)) return null;

        // also check for public permission
        ClaimPermission permissionLevel = this.playerNameToClaimPermissionMap.get("public");
        if (ClaimPermission.BUILD == permissionLevel || ClaimPermission.INVENTORY == permissionLevel || ClaimPermission.ACCESS == permissionLevel)
            return null;

        // permission inheritance for subdivisions
        if (this.parent != null)
            return this.parent.allowAccess(player);

        // catch-all error message for all other cases
        String reason = plugin.getMessageManager().getMessage(Messages.NoAccessPermission, this.getOwnerName());
        if (player.hasPermission("griefprevention.ignoreclaims"))
            reason += "  " + plugin.getMessageManager().getMessage(Messages.IgnoreClaimsAdvertisement);
        return reason;
    }

    /**
     * Inventory permission check
     *
     * @param player
     * @return
     */
    public String allowContainers(Player player) {
        // if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
        if (player == null) return "";

        // owner and administrators in ignoreclaims mode have access
        if (this.ownerName.equals(player.getName()) || plugin.getDataStore().getPlayerData(player.getName()).isIgnoreClaims())
            return null;

        // admin claims need adminclaims permission only.
        if (this.isAdminClaim()) {
            if (player.hasPermission("griefprevention.adminclaims")) return null;
        }

        // check for explicit individual container or build permission 
        if (this.hasExplicitPermission(player, ClaimPermission.INVENTORY)) return null;
        if (this.hasExplicitPermission(player, ClaimPermission.BUILD)) return null;

        // check for public container or build permission
        ClaimPermission permissionLevel = this.playerNameToClaimPermissionMap.get("public");
        if (ClaimPermission.BUILD == permissionLevel || ClaimPermission.INVENTORY == permissionLevel) return null;

        // permission inheritance for subdivisions
        if (this.parent != null)
            return this.parent.allowContainers(player);

        // error message for all other cases
        String reason = plugin.getMessageManager().getMessage(Messages.NoContainersPermission, this.getOwnerName());
        if (player.hasPermission("griefprevention.ignoreclaims"))
            reason += "  " + plugin.getMessageManager().getMessage(Messages.IgnoreClaimsAdvertisement);
        return reason;
    }

    /**
     * Grant permission check, relatively simple
     *
     * @param player
     * @return
     */
    public String allowGrantPermission(Player player) {
        // if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
        if (player == null) return "";

        // anyone who can modify the claim can do this
        if (this.allowEdit(player) == null) return null;

        // anyone who's in the managers (/PermissionTrust) list can do this
        for (String managerID : this.managers) {
            if (this.isApplicablePlayer(managerID, player.getName())) return null;

            else if (managerID.startsWith("[") && managerID.endsWith("]")) {
                managerID = managerID.substring(1, managerID.length() - 1);
                if (managerID == null || managerID.isEmpty()) continue;
                if (player.hasPermission(managerID)) return null;
            }
        }

        // permission inheritance for subdivisions
        if (this.parent != null)
            return this.parent.allowGrantPermission(player);

        // generic error message
        String reason = plugin.getMessageManager().getMessage(Messages.NoPermissionTrust, this.getOwnerName());
        if (player.hasPermission("griefprevention.ignoreclaims")) {
            reason += "  " + plugin.getMessageManager().getMessage(Messages.IgnoreClaimsAdvertisement);
        }
        return reason;
    }

    /**
     * Grants a permission for a player or the public
     *
     * @param playerName
     * @param permissionLevel
     * @return
     */
    public boolean setPermission(String playerName, ClaimPermission permissionLevel) {
        // we only want to send events if the claim is in the data store
        if (inDataStore) {
            ClaimModifiedEvent.Type permtype;
            switch (permissionLevel) {
                case ACCESS:
                    permtype = ClaimModifiedEvent.Type.AddedAccessTrust;
                    break;
                case BUILD:
                    permtype = ClaimModifiedEvent.Type.AddedBuildTrust;
                    break;
                case INVENTORY:
                    permtype = ClaimModifiedEvent.Type.AddedInventoryTrust;
                    break;
                default:
                    permtype = null;
            }


            ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, null, permtype);
            Bukkit.getServer().getPluginManager().callEvent(claimevent);
            if (claimevent.isCancelled()) {
                return false;
            }
        }
        this.playerNameToClaimPermissionMap.put(playerName.toLowerCase(), permissionLevel);
        return true;
    }

    /**
     * Revokes a permission for a player or the public
     *
     * @param playerName
     */
    public boolean dropPermission(String playerName) {
        // we only want to send events if the claim is in the data store
        if (inDataStore) {
            ClaimPermission perm = this.playerNameToClaimPermissionMap.get(playerName.toLowerCase());
            // If they aren't in the map, let's just return
            if (perm == null) {
                return true;
            }
            ClaimModifiedEvent.Type permtype;
            switch (perm) {
                case ACCESS:
                    permtype = ClaimModifiedEvent.Type.RemovedAccessTrust;
                    break;
                case BUILD:
                    permtype = ClaimModifiedEvent.Type.RemovedBuildTrust;
                    break;
                case INVENTORY:
                    permtype = ClaimModifiedEvent.Type.RemovedInventoryTrust;
                    break;
                default:
                    permtype = null;
            }
            ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, null, permtype);
            Bukkit.getServer().getPluginManager().callEvent(claimevent);
            if (claimevent.isCancelled()) {
                return false;
            }
        }
        this.playerNameToClaimPermissionMap.remove(playerName.toLowerCase());
        return true;
    }

    /**
     * Clears all permissions (except owner of course)
     *
     * @return true if permissions were cleared successfully, false if a plugin prevented it from clearing them.
     */
    public boolean clearPermissions() {
        // we only want to send events if the claim is in the data store
        if (inDataStore) {
            ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, null, ClaimModifiedEvent.Type.PermissionsCleared);
            Bukkit.getServer().getPluginManager().callEvent(claimevent);
            if (claimevent.isCancelled()) {
                return false;
            }
        }
        this.playerNameToClaimPermissionMap.clear();
        return true;
    }

    /**
     * Gets ALL permissions.<br />
     * Useful for  making copies of permissions during a claim resize and listing all permissions in a claim.
     *
     * @param builders
     * @param containers
     * @param accessors
     * @param managers
     */
    public void getPermissions(ArrayList<String> builders, ArrayList<String> containers, ArrayList<String> accessors, ArrayList<String> managers) {
        // loop through all the entries in the hash map
        // if we have a parent, add the parent permissions first, then overwrite them.
        for (Map.Entry<String, ClaimPermission> entry : this.playerNameToClaimPermissionMap.entrySet()) {
            // build up a list for each permission level
            if (entry.getValue() == ClaimPermission.BUILD) {
                builders.add(entry.getKey());
            } else if (entry.getValue() == ClaimPermission.INVENTORY) {
                containers.add(entry.getKey());
            } else {
                accessors.add(entry.getKey());
            }
        }

        // managers are handled a little differently
        for (String manager : this.managers) {
            managers.add(manager);
        }
    }

    /**
     * Returns a copy of the location representing lower x, y, z limits
     *
     * @return
     */
    public Location getMin() {
        return this.min.clone();
    }

    /**
     * Returns a copy of the location representing upper x, y, z limits.<br />
     * NOTE: remember upper Y will always be ignored, all claims always extend to the sky.
     *
     * @return
     */
    public Location getMax() {
        return this.max.clone();
    }

    /**
     * Returns a friendly owner name (for admin claims, returns "an administrator" as the owner)
     *
     * @return
     */
    public String getOwnerName() {
        if (this.parent != null)
            return this.parent.getOwnerName();

        if (this.ownerName.length() == 0)
            return plugin.getMessageManager().getMessage(Messages.OwnerNameForAdminClaims);

        return this.ownerName;
    }

    public boolean contains(Claim otherclaim, boolean ignoreHeight) {
        return (contains(otherclaim.min, ignoreHeight, false) &&
                contains(otherclaim.max, ignoreHeight, false));
    }

    public boolean contains(int lx, int lz, int gx, int gz) {
        if (lx >= this.min.getBlockX() && lz >= this.min.getBlockZ()) {
            return gx <= this.getMax().getBlockX() && gz <= this.getMax().getBlockZ();
        }
        return false;
    }

    public static boolean contains(Location pA, Location pB, Location target, boolean ignoreHeight) {

        int minX = Math.min(pA.getBlockX(), pB.getBlockX());
        int maxX = Math.max(pA.getBlockX(), pB.getBlockX());
        int minZ = Math.min(pA.getBlockZ(), pB.getBlockZ());
        int maxZ = Math.max(pA.getBlockZ(), pB.getBlockZ());


        if (target.getBlockX() < minX || target.getBlockX() > maxX) {
            return false;
        } else if (target.getBlockZ() < minZ || target.getBlockZ() > maxZ) {
            return false;
        } else if (!ignoreHeight) {
            int minY = Math.min(pA.getBlockY(), pB.getBlockY());
            int maxY = Math.max(pA.getBlockY(), pB.getBlockY());
            if (target.getBlockY() < minY || target.getBlockY() > maxY) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether or not a location is in the claim.
     *
     * @param location
     * @param ignoreHeight        true means location UNDER the claim will return TRUE
     * @param excludeSubdivisions true means that locations inside subdivisions of the claim will return FALSE
     * @return
     */
    public boolean contains(Location location, boolean ignoreHeight, boolean excludeSubdivisions) {
        // not in the same world implies false
        if (!location.getWorld().equals(this.min.getWorld())) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // main check
        boolean inClaim = (ignoreHeight || y >= this.min.getBlockY()) &&
                x >= this.min.getBlockX() &&
                x <= this.max.getBlockX() &&
                z >= this.min.getBlockZ() &&
                z <= this.max.getBlockZ();

        if (!inClaim) return false;

        // additional check for subdivisions
        // you're only in a subdivision when you're also in its parent claim
        // NOTE: if a player creates subdivions then resizes the parent claim, it's possible that
        // a subdivision can reach outside of its parent's boundaries.  so this check is important!
        if (this.parent != null) {
            return this.parent.contains(location, ignoreHeight, false);
        }

        // code to exclude subdivisions in this check
        else if (excludeSubdivisions) {
            // search all subdivisions to see if the location is in any of them
            for (Claim aChildren : this.children) {
                // if we find such a subdivision, return false
                if (aChildren.contains(location, ignoreHeight, true)) {
                    return false;
                }
            }
        }
        // otherwise yes
        return true;
    }

    // whether or not two claims overlap
    // used internally to prevent overlaps when creating claims
    boolean overlaps(Claim otherClaim) {
        // NOTE:  if trying to understand this makes your head hurt, don't feel bad - it hurts mine too.  
        // try drawing pictures to visualize test cases.

        if (!this.min.getWorld().equals(otherClaim.getMin().getWorld())) return false;

        // first, check the corners of this claim aren't inside the other
        if (otherClaim.contains(this.min, true, false)) return true;
        if (otherClaim.contains(this.max, true, false)) return true;
        if (otherClaim.contains(new Location(this.min.getWorld(), this.min.getBlockX(), 0, this.max.getBlockZ()), true, false))
            return true;
        if (otherClaim.contains(new Location(this.min.getWorld(), this.max.getBlockX(), 0, this.min.getBlockZ()), true, false))
            return true;

        // verify that no claim's lesser boundary point is inside this new claim, to cover the "existing claim is entirely inside new claim" case
        if (this.contains(otherClaim.getMin(), true, false)) return true;

        // verify this claim doesn't band across an existing claim, either horizontally or vertically		
        if (this.getMin().getBlockZ() <= otherClaim.getMax().getBlockZ() &&
                this.getMin().getBlockZ() >= otherClaim.getMin().getBlockZ() &&
                this.getMin().getBlockX() < otherClaim.getMin().getBlockX() &&
                this.getMax().getBlockX() > otherClaim.getMax().getBlockX())
            return true;

        if (this.getMax().getBlockZ() <= otherClaim.getMax().getBlockZ() &&
                this.getMax().getBlockZ() >= otherClaim.getMin().getBlockZ() &&
                this.getMin().getBlockX() < otherClaim.getMin().getBlockX() &&
                this.getMax().getBlockX() > otherClaim.getMax().getBlockX())
            return true;

        if (this.getMin().getBlockX() <= otherClaim.getMax().getBlockX() &&
                this.getMin().getBlockX() >= otherClaim.getMin().getBlockX() &&
                this.getMin().getBlockZ() < otherClaim.getMin().getBlockZ() &&
                this.getMax().getBlockZ() > otherClaim.getMax().getBlockZ())
            return true;

        if (this.getMax().getBlockX() <= otherClaim.getMax().getBlockX() &&
                this.getMax().getBlockX() >= otherClaim.getMin().getBlockX() &&
                this.getMin().getBlockZ() < otherClaim.getMin().getBlockZ() &&
                this.getMax().getBlockZ() > otherClaim.getMax().getBlockZ())
            return true;

        return false;
    }

    /**
     * Whether more entities may be added to a claim
     *
     * @return
     */
    public String allowMoreEntities() {
        if (this.parent != null) return this.parent.allowMoreEntities();

        // this rule only applies to creative mode worlds
        if (!plugin.creativeRulesApply(this.getMin())) return null;

        // admin claims aren't restricted
        if (this.isAdminClaim()) return null;

        // don't apply this rule to very large claims
        if (this.getArea() > 10000) return null;

        // determine maximum allowable entity count, based on claim size
        int maxEntities = this.getArea() / 50;
        if (maxEntities == 0) return plugin.getMessageManager().getMessage(Messages.ClaimTooSmallForEntities);

        // count current entities (ignoring players)
        Chunk lesserChunk = this.getMin().getChunk();
        Chunk greaterChunk = this.getMax().getChunk();

        int totalEntities = 0;
        for (int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
            for (int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++) {
                Chunk chunk = lesserChunk.getWorld().getChunkAt(x, z);
                Entity[] entities = chunk.getEntities();
                for (Entity entity : entities) {
                    if (!(entity instanceof Player) && this.contains(entity.getLocation(), false, false)) {
                        totalEntities++;
                        if (totalEntities > maxEntities) entity.remove();
                    }
                }
            }
        if (totalEntities > maxEntities) {
            return plugin.getMessageManager().getMessage(Messages.TooManyEntitiesInClaim);
        }
        return null;
    }

    // implements a strict ordering of claims, used to keep the claims collection sorted for faster searching
    boolean greaterThan(Claim otherClaim) {
        Location thisCorner = this.getMin();
        Location otherCorner = otherClaim.getMin();
        if (thisCorner.getBlockX() > otherCorner.getBlockX()) return true;
        if (thisCorner.getBlockX() < otherCorner.getBlockX()) return false;
        if (thisCorner.getBlockZ() > otherCorner.getBlockZ()) return true;
        if (thisCorner.getBlockZ() < otherCorner.getBlockZ()) return false;
        return thisCorner.getWorld().getName().compareTo(otherCorner.getWorld().getName()) < 0;
    }

    public long getPlayerInvestmentScore() {
        // decide which blocks will be considered player placed
        Location lesserBoundaryCorner = this.getMin();
        ArrayList<Integer> playerBlocks = RestoreNatureProcessingTask.getPlayerBlocks(lesserBoundaryCorner.getWorld().getEnvironment(), lesserBoundaryCorner.getBlock().getBiome());

        // scan the claim for player placed blocks
        double score = 0;
        boolean creativeMode = plugin.creativeRulesApply(lesserBoundaryCorner);
        for (int x = this.min.getBlockX(); x <= this.max.getBlockX(); x++) {
            for (int z = this.min.getBlockZ(); z <= this.max.getBlockZ(); z++) {
                int y = this.min.getBlockY();
                for (; y < plugin.getWorldCfg(this.getMin().getWorld()).getSeaLevelOverride() - 5; y++) {
                    Block block = this.min.getWorld().getBlockAt(x, y, z);
                    if (playerBlocks.contains(block.getTypeId())) {
                        if (block.getType() == Material.CHEST && !creativeMode) {
                            score += 10;
                        } else {
                            score += .5;
                        }
                    }
                }

                for (; y < this.min.getWorld().getMaxHeight(); y++) {
                    Block block = this.min.getWorld().getBlockAt(x, y, z);
                    if (playerBlocks.contains(block.getTypeId())) {
                        if (block.getType() == Material.CHEST && !creativeMode) {
                            score += 10;
                        } else if (creativeMode && (block.getType() == Material.LAVA || block.getType() == Material.STATIONARY_LAVA)) {
                            score -= 10;
                        } else {
                            score += 1;
                        }
                    }
                }
            }
        }
        return (long) score;
    }

    /**
     * special routine added for Group support. We need to be able to recognize
     * player names from a list where the name is actually within a group that is in that list.
     *
     * @param testlist list of names (possibly including groups)
     * @param testfor  player name to test for.
     * @return
     */
    private boolean playerInList(List<String> testlist, String testfor) {
        // if it starts with g: or G:, remove it.

        for (String iteratename : testlist) {
            // test if applicable.
            if (isApplicablePlayer(iteratename, testfor)) {
                return true;
            }
        }
        // we tried to find it in the list and all groups that were in that list, but we didn't find it.
        return false;
    }

    /**
     * Checks to see if this player is a manager.
     *
     * @param player
     * @return
     */
    public boolean isManager(String player) {
        // if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
        return player != null && playerInList(managers, player);
    }

    /**
     * Adds a manager to the claim.
     *
     * @param player
     * @return
     */
    public boolean addManager(String player) {
        // we only want to send events if the claim is in the data store
        if (inDataStore) {
            ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, player, ClaimModifiedEvent.Type.AddedManager);
            Bukkit.getServer().getPluginManager().callEvent(claimevent);
            if (claimevent.isCancelled()) {
                return false;
            }
        }
        managers.add(player);
        return true;
    }

    /**
     * Removes a manager from the claim.
     *
     * @param player
     * @return
     */
    public boolean removeManager(String player) {
        // we only want to send events if the claim is in the data store
        if (inDataStore) {
            ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, player, ClaimModifiedEvent.Type.RemovedManager);
            Bukkit.getServer().getPluginManager().callEvent(claimevent);
            if (claimevent.isCancelled()) {
                return false;
            }
        }
        managers.remove(player);
        return true;
    }

    /**
     * This returns a copy of the managers list. Additions and removals in this list do not affect the claim.
     *
     * @return The list of the managers.
     */
    public ArrayList<String> getManagerList() {
        return (ArrayList<String>) managers.clone();
    }

    public long getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(long modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public boolean isInDataStore() {
        return inDataStore;
    }

    public void setInDataStore(boolean inDataStore) {
        this.inDataStore = inDataStore;
    }

    public ArrayList<Claim> getChildren() {
        return children;
    }

    public Claim getParent() {
        return parent;
    }

    public void setParent(Claim parent) {
        this.parent = parent;
    }

    public boolean isNeverDelete() {
        return neverDelete;
    }

    public void setNeverDelete(boolean neverDelete) {
        this.neverDelete = neverDelete;
    }

    public boolean isExplosivesAllowed() {
        return explosivesAllowed;
    }

    public void setExplosivesAllowed(boolean isExplosivesAllowed) {
        this.explosivesAllowed = isExplosivesAllowed;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @SuppressWarnings("unused")
    public PluginClaimMeta getClaimMeta(Plugin plugin, boolean create) {
        if (!claimMeta.containsKey(plugin.getName())) {
            if (!create) {
                return null;
            }
            claimMeta.put(plugin.getName(), new PluginClaimMeta(plugin));
        }
        return claimMeta.get(plugin.getName());
    }

    public HashMap<String, PluginClaimMeta> getAllClaimMeta() {
        return claimMeta;
    }

    public void loadClaimMeta(ConfigurationSection cfg) {
        claimMeta.clear();
        for (String key: cfg.getKeys(false)) {
            PluginClaimMeta meta = PluginClaimMeta.deserialize(cfg.getValues(true));
            meta.pluginName = cfg.getName();
            claimMeta.put(key, meta);
        }
    }

    public String getFlag(BaseFlag flag) {
        if (plugin.getFlagManager().isFlagRegistered(flag)) {
            if (!flags.containsKey(flag.getKey().toLowerCase())) {
                flags.put(flag.getKey().toLowerCase(), flag.getDefaultValue());
            }
            return flags.get(flag.getKey().toLowerCase());
        }
        return null;
    }

    public void setFlag(BaseFlag flag, String value) throws InvalidFlagValueException {
        for (String v: flag.getValidOptions()) {
            if (v.equalsIgnoreCase(value)) {
                flags.put(flag.getKey().toLowerCase(), v);
                return;
            }
        }
        throw new InvalidFlagValueException(value + " is not a valid value for " + flag.getKey());
    }

    public HashMap<String, String> getFlags() {
        return flags;
    }

    public void loadFlags(Map<String, Object> rawFlags) {
        for (Map.Entry<String, Object> entry: rawFlags.entrySet()) {
             flags.put(entry.getKey(), (String) entry.getValue());
        }
    }

    public String toString() {
        return String.format("<Claim: %s, p1:%s, p2:%s>", this.id.toString(), this.min.toVector(), this.max.toVector());

    }

    public void setMin(Location min) {
        this.min = min;
    }

    public void setMax(Location max) {
        this.max = max;
    }

    public Map<String, Object> serialize() {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("modifiedDate", this.getModifiedDate());
        data.put("minimumPoint", SerializationUtil.locationToString(this.getMin()));
        data.put("maximumPoint", SerializationUtil.locationToString(this.getMax()));
        data.put("ownerName", this.getOwnerName());
        data.put("neverDelete", this.isNeverDelete());
        HashMap<String, String> flagMap = new HashMap<String, String>(flags);
        data.put("flags", flagMap);
        if (this.getParent() != null) {
            data.put("parentId", this.getParent().getId().toString());
        }
        ArrayList<String> builders = new ArrayList<String>();
        ArrayList<String> containers = new ArrayList<String>();
        ArrayList<String> accessors = new ArrayList<String>();
        ArrayList<String> managers = new ArrayList<String>();
        getPermissions(builders, containers, accessors, managers);
        data.put("builders", builders);
        data.put("containers", containers);
        data.put("accessors", accessors);
        data.put("managers", managers);
        if (!this.getAllClaimMeta().isEmpty()) {
            HashMap<String, Object> meta = new HashMap<String, Object>();
            for (String key: this.getAllClaimMeta().keySet()) {
                meta.put(key, this.getAllClaimMeta().get(key).serialize());
            }
            data.put("meta", meta);
        }
        return data;
    }
}