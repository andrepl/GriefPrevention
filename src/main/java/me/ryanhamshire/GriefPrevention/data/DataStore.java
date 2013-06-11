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

import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.ClaimPermission;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.data.persistence.FileSystemPersistence;
import me.ryanhamshire.GriefPrevention.data.persistence.IPersistence;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent;
import me.ryanhamshire.GriefPrevention.exceptions.ClaimOwnershipException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

// singleton class which manages all GriefPrevention data (except for config options)
public class DataStore {

    GriefPrevention plugin;
    IPersistence persistence;

    HashMap<String, PlayerData> playerData;
    HashSet<String> dirtyPlayerNames;

    ClaimMap claims;
    HashSet<UUID> dirtyClaimIds;

    public DataStore(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        // TODO Implement Configurable Persistence
        persistence = new FileSystemPersistence(plugin);
        persistence.onEnable();

        playerData = new HashMap<String, PlayerData>();
        dirtyPlayerNames = new HashSet<String>();
        claims = new ClaimMap();
        dirtyClaimIds = new HashSet<UUID>();

        for (PlayerData pd: persistence.loadPlayerData()) {
            playerData.put(pd.getPlayerName(), pd);
        }

        for (Claim claim: persistence.loadClaimData()) {
            if (claim.getParent() == null && claim.getOwnerName() != null && !claim.getOwnerName().equals("")) {
                PlayerData player = playerData.get(claim.getOwnerName());
                if (player != null) {
                    player.getClaims().add(claim);
                }
            }
            if (claim.getParent() == null) {
                claims.add(claim);
            }
        }
    }

    public void onDisable() {
        Claim[] dirtyClaims = new Claim[dirtyClaimIds.size()];
        int i = 0;
        for (UUID uuid: dirtyClaimIds) {
            plugin.debug("Saving dirty claim[" + uuid + "]: " + claims.get(uuid));
            dirtyClaims[i++] = (claims.get(uuid));
        }
        persistence.writeClaimDataSync(dirtyClaims);
        PlayerData[] dirtyPlayerData = new PlayerData[dirtyPlayerNames.size()];
        i = 0;
        for (String name: dirtyPlayerNames) {
            dirtyPlayerData[i++] = playerData.get(name);
        }
        persistence.writePlayerDataSync(dirtyPlayerData);
        persistence.onDisable();
    }

    public PlayerData getPlayerData(String playerName) {
        if (playerName == null) {
            return null;
        }
        if (playerData.containsKey(playerName)) {
            return playerData.get(playerName);
        }
        PlayerData pd = persistence.loadOrCreatePlayerData(playerName);
        playerData.put(playerName, pd);
        Collection<Claim> playerClaims = claims.getForPlayer(pd.getPlayerName());
        if (playerClaims != null) {
            pd.getClaims().addAll(playerClaims);
        }
        return pd;
    }

    public void savePlayerData(String playerName, PlayerData playerData) {
        this.playerData.put(playerName, playerData);
        this.dirtyPlayerNames.add(playerName);
    }

    public Claim getClaim(UUID id) {
        return claims.get(id);
    }

    public Claim getClaimAt(Location location, boolean ignoreHeight, Claim cachedClaim) {
        if (cachedClaim != null && cachedClaim.isInDataStore() && cachedClaim.contains(location, ignoreHeight, true)) {
            return cachedClaim;
        }

        // the claims list is ordered by greater boundary corner
        // create a temporary "fake" claim in memory for comparison purposes
        Claim tempClaim = new Claim();
        tempClaim.lesserBoundaryCorner = location;
        // Let's get all the claims in this block's chunk
        ArrayList<Claim> aclaims = claims.getForChunk(location.getChunk());

        // If there are no claims here, let's return null.
        if (aclaims == null) {
            return null;
        }

        // otherwise, search all existing claims in the chunk until we find the right claim
        for (Claim claim : aclaims) {
            //if we reach a claim which is greater than the temp claim created above, there's definitely no claim
            //in the collection which includes our location
            if (claim.greaterThan(tempClaim)) return null;

            //find a top level claim
            if (claim.contains(location, ignoreHeight, false)) {
                //when we find a top level claim, if the location is in one of its subdivisions,
                //return the SUBDIVISION, not the top level claim
                for (int j = 0; j < claim.getChildren().size(); j++) {
                    Claim subdivision = claim.getChildren().get(j);
                    if (subdivision.contains(location, ignoreHeight, false)) return subdivision;
                }
                return claim;
            }
        }
        //if no claim found, return null
        return null;
    }

    public void saveClaim(Claim claim) {
        if (claim.getId() == null) {
            claim.setId(UUID.randomUUID());
        }
        if (!claims.contains(claim.getId())) {
            claims.add(claim);
        }
        this.dirtyClaimIds.add(claim.getId());
    }

    /**
     * Creates a Claim.
     *
     * if the new claim would overlap an existing claim, returns a failure along with a reference to the existing claim
     * otherwise, returns a success along with a reference to the new claim.
     *
     * DOES adjust claim blocks available on success (players can go into negative quantity available)
     * does NOT check a player has permission to create a claim, or enough claim blocks.
     * does NOT check minimum claim size constraints
     * does NOT visualize the new claim for any players
     *
     * @param world the world to create the claim in
     * @param x1 the lesser x coordinate
     * @param x2 the greater x coordinate
     * @param y1 the lesser y coordinate
     * @param y2 the greater y coordinate
     * @param z1 the lesser z coordinate
     * @param z2 the greater z coordinate
     * @param ownerName the claim owner's name or "" for administrative claims
     * @param parent the parent claim if this is a subclaim, otherwise null
     * @param id the claims UUID
     * @param neverdelete if true, this claim will never be automatically deleted
     * @param oldclaim pass in the claim being resized if any.  it will be ignored during collision checks
     * @param claimcreator the player creating the claim
     * @param doRaiseEvent if true, fire a cancellable event.
     * @return CreateClaimResult
     */
    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2,
                                                       String ownerName, Claim parent, UUID id, boolean neverdelete,
                                                       Claim oldclaim,Player claimcreator,boolean doRaiseEvent) {

        CreateClaimResult result = new CreateClaimResult();
        WorldConfig wc = plugin.getWorldCfg(world);
        int smallx, bigx, smally, bigy, smallz, bigz;

        //determine small versus big inputs
        if (x1 < x2) {
            smallx = x1;
            bigx = x2;
        } else {
            smallx = x2;
            bigx = x1;
        }

        if (y1 < y2) {
            smally = y1;
            bigy = y2;
        } else {
            smally = y2;
            bigy = y1;
        }

        if (z1 < z2) {
            smallz = z1;
            bigz = z2;
        } else {
            smallz = z2;
            bigz = z1;
        }

        //creative mode claims always go to bedrock
        if (wc.getCreativeRules()) {
            smally = 2;
        }

        //create a new claim instance (but don't save it, yet)
        Claim newClaim = new Claim(plugin,
                new Location(world, smallx, smally, smallz),
                new Location(world, bigx, bigy, bigz),
                ownerName,
                new String[] {},
                new String[] {},
                new String[] {},
                new String[] {},
                id, false);

        newClaim.setParent(parent);

        //ensure this new claim won't overlap any existing claims
        ArrayList<Claim> claimsToCheck;
        if (newClaim.getParent() != null) {
            claimsToCheck = newClaim.getParent().getChildren();
        } else {
            Long[] claimchunks = ClaimMap.getChunks(newClaim);
            claimsToCheck = new ArrayList<Claim>();
            for (Long chunk: claimchunks) {
                ArrayList<Claim> chunkclaims = this.claims.getForChunk(world.getName(), chunk);
                if (chunkclaims == null) {
                    continue;
                }
                for (Claim claim: chunkclaims) {
                    if(!claimsToCheck.contains(claim)) {
                        claimsToCheck.add(claim);
                    }
                }
            }
        }

        for (Claim otherClaim : claimsToCheck) {
            //if we find an existing claim which will be overlapped
            if (otherClaim.overlaps(newClaim)) {
                //result = fail, return conflicting claim
                result.succeeded = CreateClaimResult.Result.CLAIM_OVERLAP;
                result.claim = otherClaim;
                return result;
            }
        }
        if (oldclaim == null) {
            if (doRaiseEvent) {
                ClaimCreatedEvent claimevent = new ClaimCreatedEvent(newClaim, claimcreator);
                Bukkit.getServer().getPluginManager().callEvent(claimevent);
                if (claimevent.isCancelled()) {
                    result.succeeded = CreateClaimResult.Result.CANCELED;
                    return result;
                }
            }
        }
        //otherwise add this new claim to the data store to make it effective
        this.claims.add(newClaim);
        this.saveClaim(newClaim);

        //then return success along with reference to new claim
        result.succeeded = CreateClaimResult.Result.SUCCESS;
        result.claim = newClaim;
        return result;
    }

    /**
     * Tries to resize a claim
     * @param claimResizing The claim to resize
     * @param newx1 corner 1 x
     * @param newx2 corner 2 x
     * @param newy1 corner 1 y
     * @param newy2 corner 2 y
     * @param newz1 corner 1 z
     * @param newz2 corner 2 z
     * @return
     */
    public CreateClaimResult resizeClaim(Claim claimResizing, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2, Player player) {

        Location newLesser = new Location(claimResizing.getLesserBoundaryCorner().getWorld(), newx1, newy1, newz1);
        Location newGreater = new Location(claimResizing.getLesserBoundaryCorner().getWorld(), newx2, newy2, newz2);
        ClaimResizeEvent cre = new ClaimResizeEvent(claimResizing, newLesser, newGreater, player);
        Bukkit.getPluginManager().callEvent(cre);
        if (cre.isCancelled()) {
            CreateClaimResult res = new CreateClaimResult();
            res.claim = claimResizing;
            res.succeeded = CreateClaimResult.Result.CANCELED;
            return res;
        }
        //remove old claim. We don't raise an event for this!
        this.deleteClaim(claimResizing, player, false);

        //try to create this new claim, ignoring the original when checking for overlap
        CreateClaimResult result = this.createClaim(claimResizing.getLesserBoundaryCorner().getWorld(), newx1, newx2, newy1, newy2, newz1, newz2,
                claimResizing.getOwnerName(), claimResizing.getParent(), claimResizing.getId(), claimResizing.isNeverDelete(), claimResizing, player ,false);

        //if succeeded
        if (result.succeeded == CreateClaimResult.Result.SUCCESS) {
            //copy permissions from old claim
            ArrayList<String> builders = new ArrayList<String>();
            ArrayList<String> containers = new ArrayList<String>();
            ArrayList<String> accessors = new ArrayList<String>();
            ArrayList<String> managers = new ArrayList<String>();
            claimResizing.getPermissions(builders, containers, accessors, managers);
            for (String builder : builders) {
                result.claim.setPermission(builder, ClaimPermission.BUILD);
            }
            for (String container : containers) {
                result.claim.setPermission(container, ClaimPermission.INVENTORY);
            }
            for (String accessor : accessors) {
                result.claim.setPermission(accessor, ClaimPermission.ACCESS);
            }
            for (String manager : managers) {
                result.claim.addManager(manager);
            }
            //copy subdivisions from old claim
            for (Claim subdivision: claimResizing.getChildren()) {
                subdivision.setParent(result.claim);
                result.claim.getChildren().add(subdivision);
            }
            //save those changes
            this.saveClaim(result.claim);
        } else {
            //put original claim back
            this.claims.add(claimResizing);
            this.saveClaim(claimResizing);
        }
        return result;
    }

    public boolean deleteClaim(Claim claim, Player player, boolean callEvent) {
        //fire the delete Claim event.
        if (callEvent) {
            ClaimDeletedEvent ev = new ClaimDeletedEvent(claim, player);
            Bukkit.getPluginManager().callEvent(ev);
            if(ev.isCancelled()) {
                return false;
            }
        }
        claims.remove(claim);
        persistence.deleteClaim(claim);
        //update player data, except for administrative claims, which have no owner
        if (!claim.isAdminClaim()) {
            PlayerData ownerData = this.getPlayerData(claim.getOwnerName());
            ownerData.getClaims().remove(claim);
            this.savePlayerData(claim.getOwnerName(), ownerData);
        }
        return true;
    }

    public UUID[] getTopLevelClaimIDs() {
        return claims.getTopLevelClaimIDs();
    }

    public int claimCount() {
        return claims.size();
    }

    public void extendClaim(Claim claim, int newDepth) {
        WorldConfig wc = plugin.getWorldCfg(claim.getLesserBoundaryCorner().getWorld());

        if(newDepth < wc.getClaimsMaxDepth()) newDepth = wc.getClaimsMaxDepth();

        if(claim.getParent() != null){
            claim = claim.getParent();
        }

        claim.getLesserBoundaryCorner().setY(newDepth);
        claim.getGreaterBoundaryCorner().setY(newDepth);

        for (Claim child: claim.getChildren()) {
            child.getLesserBoundaryCorner().setY(newDepth);
            child.getGreaterBoundaryCorner().setY(newDepth);
        }
        saveClaim(claim);
    }

    /**
     * Deletes all claims owned by a player
     *
     * @param ownerName Case SeNsItIvE player name
     * @param deleteCreativeClaims Delete all the player's creative claims?
     * @param deleteLockedClaims Should we delete claims that have been locked to not delete?
     */
    public void deleteClaimsForPlayer(String ownerName, boolean deleteCreativeClaims, boolean deleteLockedClaims) {
        //make a list of the player's claims
        ArrayList<Claim> claimsToDelete = new ArrayList<Claim>();
        for (Claim claim: this.claims.getAllTopLevel()) {
            if(claim.getOwnerName().equals(ownerName) &&
                    (deleteCreativeClaims || !plugin.creativeRulesApply(claim.getLesserBoundaryCorner())) &&
                    (!claim.isNeverDelete()|| deleteLockedClaims)) {
                claimsToDelete.add(claim);
            }
        }

        // delete them one by one
        for (Claim claim: claimsToDelete) {
            claim.removeSurfaceFluids(null);
            this.deleteClaim(claim, null, true);
            // if in a creative mode world, delete the claim
            if (plugin.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                plugin.restoreClaim(claim, 0);
            }
        }
    }

    public void changeClaimOwner(Claim claimToGive, String newOwnerName) throws ClaimOwnershipException {
        // if it's a subdivision, throw an exception
        if (claimToGive.getParent() != null) {
            throw new ClaimOwnershipException("Subdivisions can't be transferred.  Only top-level claims may change owners.");
        }

        //otherwise update information

        //determine current claim owner
        PlayerData ownerData = null;
        if (!claimToGive.isAdminClaim()) {
            ownerData = this.getPlayerData(claimToGive.getOwnerName());
        }

        //determine new owner
        PlayerData newOwnerData = this.getPlayerData(newOwnerName);

        //transfer
        claimToGive.setOwnerName(newOwnerName);
        this.saveClaim(claimToGive);

        //adjust blocks and other records
        if (ownerData != null) {
            ownerData.getClaims().remove(claimToGive);
            ownerData.setBonusClaimBlocks(ownerData.getBonusClaimBlocks() - claimToGive.getArea());
            this.savePlayerData(claimToGive.getOwnerName(), ownerData);
        }

        newOwnerData.getClaims().add(claimToGive);
        newOwnerData.setBonusClaimBlocks(newOwnerData.getBonusClaimBlocks() + claimToGive.getArea());
        this.savePlayerData(newOwnerName, newOwnerData);
    }

    public int getGroupBonusBlocks(String name) {
        return 0;  // TODO
    }

    public int adjustGroupBonusBlocks(String permissionIdentifier, int adjustment) {
        return 0;  // TODO
    }
}