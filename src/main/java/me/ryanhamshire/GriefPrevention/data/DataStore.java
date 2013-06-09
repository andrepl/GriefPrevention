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

import me.ryanhamshire.GriefPrevention.*;
import me.ryanhamshire.GriefPrevention.configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.events.*;
import me.ryanhamshire.GriefPrevention.exceptions.DatastoreInitializationException;
import me.ryanhamshire.GriefPrevention.exceptions.SubdivisionException;
import me.ryanhamshire.GriefPrevention.exceptions.WorldNotFoundException;
import org.apache.commons.lang.SerializationException;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// singleton class which manages all GriefPrevention data (except for config options)
public abstract class DataStore {
    // in-memory cache for player data
    protected ConcurrentHashMap<String, PlayerData> playerNameToPlayerDataMap = new ConcurrentHashMap<String, PlayerData>();

    // in-memory cache for group (permission-based) data
    protected ConcurrentHashMap<String, Integer> permissionToBonusBlocksMap = new ConcurrentHashMap<String, Integer>();

    // in-memory cache for claim data
    ClaimArray claims = new ClaimArray();

    // next claim ID
    Long nextClaimID = (long) 0;

    // path information, for where stuff stored on disk is well...  stored
    public final static String dataLayerFolderPath = "plugins" + File.separator + "GriefPrevention";
    public final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";

    // initialization!
    void initialize() throws DatastoreInitializationException {
        GriefPrevention.addLogEntry(this.claims.size() + " total claims loaded.");
        // make a list of players who own claims
        Vector<String> playerNames = new Vector<String>();
        for (int i = 0; i < this.claims.size(); i++) {
            Claim claim = this.claims.get(i);

            // ignore admin claims
            if (claim.isAdminClaim()) continue;

            if (!playerNames.contains(claim.getOwnerName())) {
                playerNames.add(claim.getOwnerName());
            }
        }
        GriefPrevention.addLogEntry(playerNames.size() + " players have staked claims.");
    }

    /**
     * Removes cached player data from memory
     *
     * @param playerName
     */
    public synchronized void clearCachedPlayerData(String playerName) {
        this.playerNameToPlayerDataMap.remove(playerName);
    }

    /**
     * Gets the number of bonus blocks a player has from his permissions
     *
     * @param playerName
     * @return
     */
    public synchronized int getGroupBonusBlocks(String playerName) {
        int bonusBlocks = 0;
        Set<String> keys = permissionToBonusBlocksMap.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String groupName = iterator.next();
            Player player = GriefPrevention.instance.getServer().getPlayer(playerName);
            if (player != null && player.hasPermission(groupName)) {
                bonusBlocks += this.permissionToBonusBlocksMap.get(groupName);
            }
        }
        return bonusBlocks;
    }

    /**
     * Grants a group (players with a specific permission) bonus claim blocks as long as they're still members of the group.
     *
     * @param groupName
     * @param amount
     * @return
     */
    synchronized public int adjustGroupBonusBlocks(String groupName, int amount) {
        Integer currentValue = this.permissionToBonusBlocksMap.get(groupName);
        if (currentValue == null) currentValue = 0;

        currentValue += amount;
        this.permissionToBonusBlocksMap.put(groupName, currentValue);

        // write changes to storage to ensure they don't get lost
        this.saveGroupBonusBlocks(groupName, currentValue);

        return currentValue;
    }

    abstract void saveGroupBonusBlocks(String groupName, int amount);

    /**
     * Changes the claim's owner.
     *
     * @param claim the claim to change ownership of
     * @param newOwnerName the new owner's name
     * @throws Exception
     */
    synchronized public void changeClaimOwner(Claim claim, String newOwnerName) throws SubdivisionException {
        // if it's a subdivision, throw an exception
        if (claim.getParent() != null) {
            throw new SubdivisionException("Subdivisions can't be transferred.  Only top-level claims may change owners.");
        }

        // otherwise update information

        // determine current claim owner
        PlayerData ownerData = null;
        if (!claim.isAdminClaim()) {
            ownerData = this.getPlayerData(claim.getOwnerName());
        }

        // determine new owner
        PlayerData newOwnerData = this.getPlayerData(newOwnerName);

        // transfer
        claim.setOwnerName(newOwnerName);
        this.saveClaim(claim);

        // adjust blocks and other records
        if (ownerData != null) {
            ownerData.getClaims().remove(claim);
            ownerData.setBonusClaimBlocks(ownerData.getBonusClaimBlocks() - claim.getArea());
            this.savePlayerData(claim.getOwnerName(), ownerData);
        }

        newOwnerData.getClaims().add(claim);
        newOwnerData.setBonusClaimBlocks(newOwnerData.getBonusClaimBlocks() + claim.getArea());
        this.savePlayerData(newOwnerName, newOwnerData);
    }

    /**
     * Adds a claim to the datastore, making it an effective claim.
     *
     * @param newClaim
     */
    synchronized void addClaim(Claim newClaim) {
        // subdivisions are easy
        if (newClaim.getParent() != null) {
            if (newClaim.subClaimid == null) {
                GriefPrevention.addLogEntry("Setting Subclaim ID to:" + String.valueOf(1 + newClaim.getParent().getChildren().size()));
                newClaim.subClaimid = (long) (newClaim.getParent().getChildren().size() + 1);
            }
            newClaim.getParent().getChildren().add(newClaim);
            newClaim.setInDataStore(true);
            this.saveClaim(newClaim);
            return;
        }

        // Get a unique identifier for the claim which will be used to name the file on disk
        if (newClaim.getID() == null) {
            newClaim.setID(this.nextClaimID);
            this.incrementNextClaimID();
        }

        // add it and mark it as added
        int j = 0;
        while (j < this.claims.size() && !this.claims.get(j).greaterThan(newClaim)) j++;
        if (j < this.claims.size()) {
            this.claims.add(j, newClaim);
        } else {
            this.claims.add(this.claims.size(), newClaim);
        }
        newClaim.setInDataStore(true);

        // except for administrative claims (which have no owner), update the owner's playerData with the new claim
        if (!newClaim.isAdminClaim()) {
            PlayerData ownerData = this.getPlayerData(newClaim.getOwnerName());
            ownerData.getClaims().add(newClaim);
            this.savePlayerData(newClaim.getOwnerName(), ownerData);
        }

        // make sure the claim is saved to disk
        this.saveClaim(newClaim);
    }


    /**
     * Saves any changes to a claim to secondary storage.
     *
     * @param claim
     */
    synchronized public void saveClaim(Claim claim) {
        // subdivisions don't save to their own files, but instead live in their parent claim's file
        // so any attempt to save a subdivision will save its parent (and thus the subdivision)
        if (claim.getParent() != null) {
            this.saveClaim(claim.getParent());
            return;
        }

        // Get a unique identifier for the claim which will be used to name the file on disk
        if (claim.getID() == null) {
            claim.setID(this.nextClaimID);
            this.incrementNextClaimID();
        }

        this.writeClaimToStorage(claim);
    }

    abstract void writeClaimToStorage(Claim claim);

    // increments the claim ID and updates secondary storage to be sure it's saved
    abstract void incrementNextClaimID();

    /**
     * Retrieves player data from memory or secondary storage, as necessary.
     * If the player has never been on the server before, this will return a fresh player data with default values.
     *
     * @param playerName
     * @return
     */
    synchronized public PlayerData getPlayerData(String playerName) {
        // first, look in memory
        PlayerData playerData = this.playerNameToPlayerDataMap.get(playerName);

        // if not there, look in secondary storage
        if (playerData == null) {
            playerData = this.getPlayerDataFromStorage(playerName);
            playerData.setPlayerName(playerName);

            // find all the claims belonging to this player and note them for future reference
            for (int i = 0; i < this.claims.size(); i++) {
                Claim claim = this.claims.get(i);
                if (claim.getOwnerName().equals(playerName)) {
                    playerData.getClaims().add(claim);
                }
            }

            // shove that new player data into the hash map cache
            this.playerNameToPlayerDataMap.put(playerName, playerData);
        }

        // try the hash map again.  if it's STILL not there, we have a bug to fix
        return this.playerNameToPlayerDataMap.get(playerName);
    }

    abstract PlayerData getPlayerDataFromStorage(String playerName);

    synchronized public boolean deleteClaim(Claim claim) {
        return deleteClaim(claim, null);
    }

    /**
     * Deletes a claim or subdivision
     *
     * @param claim
     */
    synchronized public boolean deleteClaim(Claim claim, Player p) {
        return deleteClaim(claim, true, p);
    }

    // deletes a claim or subdivision
    synchronized private boolean deleteClaim(Claim claim, boolean sendevent, Player p) {
        // fire the delete Claim event.
        if (sendevent) {
            ClaimDeletedEvent ev = new ClaimDeletedEvent(claim, p);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled())
                return false;

        }

        // subdivisions are simple - just remove them from their parent claim and save that claim
        if (claim.getParent() != null) {
            Claim parentClaim = claim.getParent();
            parentClaim.getChildren().remove(claim);
            this.saveClaim(parentClaim);
            return true;
        }

        // remove from memory
        claims.removeID(claim.getID());
        claim.setInDataStore(false);
        for (int j = 0; j < claim.getChildren().size(); j++) {
            claim.getChildren().get(j).setInDataStore(false);
        }

        // remove from secondary storage
        this.deleteClaimFromSecondaryStorage(claim);

        // update player data, except for administrative claims, which have no owner
        if (!claim.isAdminClaim()) {
            PlayerData ownerData = this.getPlayerData(claim.getOwnerName());
            for (int i = 0; i < ownerData.getClaims().size(); i++) {
                if (ownerData.getClaims().get(i).getID().equals(claim.getID())) {
                    ownerData.getClaims().remove(i);
                    break;
                }
            }
            this.savePlayerData(claim.getOwnerName(), ownerData);
        }
        return true;
    }

    abstract void deleteClaimFromSecondaryStorage(Claim claim);

    /**
     * Gets the claim at a specific location
     *
     * @param location
     * @param ignoreHeight TRUE means that a location UNDER an existing claim will return the claim
     * @param cachedClaim  can be NULL, but will help performance if you have a reasonable guess about which claim the location is in
     * @return claim in the given location. Null, if no Claim at the given location.
     */
    synchronized public Claim getClaimAt(Location location, boolean ignoreHeight, Claim cachedClaim) {
        // check cachedClaim guess first.  if it's in the datastore and the location is inside it, we're done
        if (cachedClaim != null && cachedClaim.isInDataStore() && cachedClaim.contains(location, ignoreHeight, true))
            return cachedClaim;


        // the claims list is ordered by greater boundary corner
        // create a temporary "fake" claim in memory for comparison purposes
        Claim tempClaim = new Claim();
        tempClaim.lesserBoundaryCorner = location;

        // Let's get all the claims in this block's chunk
        ArrayList<Claim> aclaims = claims.chunkmap.get(getChunk(location));

        // If there are no claims here, let's return null.
        if (aclaims == null) {
            return null;
        }

        // otherwise, search all existing claims in the chunk until we find the right claim
        for (int i = 0; i < aclaims.size(); i++) {
            Claim claim = aclaims.get(i);

            // if we reach a claim which is greater than the temp claim created above, there's definitely no claim
            // in the collection which includes our location
            if (claim.greaterThan(tempClaim)) return null;

            // find a top level claim
            if (claim.contains(location, ignoreHeight, false)) {
                // when we find a top level claim, if the location is in one of its subdivisions,
                // return the SUBDIVISION, not the top level claim
                for (int j = 0; j < claim.getChildren().size(); j++) {
                    Claim subdivision = claim.getChildren().get(j);
                    if (subdivision.contains(location, ignoreHeight, false)) return subdivision;
                }

                return claim;
            }
        }

        // if no claim found, return null
        return null;
    }

    /**
     * Gets a claim by it's ID.
     *
     * @param i The ID of the claim.
     * @return null if there is no claim by that ID, otherwise, the claim.
     */
    synchronized public Claim getClaim(long i) {
        return claims.getID(i);
    }

    /**
     * Using the claim array to add or delete claims is NOT recommended
     * and it should only be used to read claim data.
     *
     * @return The Claim Array.
     */
    synchronized public ClaimArray getClaimArray() {
        return claims;
    }

    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, String ownerName, Claim parent, Long id, boolean neverdelete, Player creator) {
        return createClaim(world, x1, x2, y1, y2, z1, z2, ownerName, parent, id, neverdelete, creator, true);
    }

    /**
     * Creates a claim.
     * If the new claim would overlap an existing claim, returns a failure along with a reference to the existing claim
     * otherwise, returns a success along with a reference to the new claim.<br />
     * Use ownerName == "" for administrative claims.<br />
     * For top level claims, pass parent == NULL<br />
     * DOES adjust claim blocks available on success (players can go into negative quantity available)
     * Does NOT check a player has permission to create a claim, or enough claim blocks.
     * Does NOT check minimum claim size constraints
     * Does NOT visualize the new claim for any players
     *
     * @param world
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @param z1
     * @param z2
     * @param ownerName
     * @param parent
     * @param id          Unless you are overwriting another claim this should be set to null
     * @param neverdelete Should this claim be locked against accidental deletion?
     * @return
     */
    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, String ownerName, Claim parent, Long id, boolean neverdelete, Player creator, boolean doRaiseEvent) {
        return createClaim(world, x1, x2, y1, y2, z1, z2, ownerName, parent, id, false, null, creator, doRaiseEvent);
    }

    @Deprecated
    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, String ownerName, Claim parent, Long id) {
        return createClaim(world, x1, x2, y1, y2, z1, z2, ownerName, parent, id, false, null);
    }

    synchronized private CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, String ownerName, Claim parent, Long id, boolean neverdelete, Claim oldclaim, Player claimcreator, boolean doRaiseEvent) {
        CreateClaimResult result = new CreateClaimResult();
        WorldConfig wc = GriefPrevention.instance.getWorldCfg(world);
        int smallx, bigx, smally, bigy, smallz, bigz;

        Player gotplayer = Bukkit.getPlayer(ownerName);
        // determine small versus big inputs
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

        // creative mode claims always go to bedrock
        if (wc.getCreativeRules()) {
            smally = 2;
        }

        // create a new claim instance (but don't save it, yet)
        Claim newClaim = new Claim(
                new Location(world, smallx, smally, smallz),
                new Location(world, bigx, bigy, bigz),
                ownerName,
                new String[]{},
                new String[]{},
                new String[]{},
                new String[]{},
                id, false);

        newClaim.setParent(parent);

        // ensure this new claim won't overlap any existing claims
        ArrayList<Claim> claimsToCheck;
        if (newClaim.getParent() != null) {
            claimsToCheck = newClaim.getParent().getChildren();
        } else {
            ArrayList<String> claimchunks = ClaimArray.getChunks(newClaim);
            claimsToCheck = new ArrayList<Claim>();
            for (String chunk : claimchunks) {
                ArrayList<Claim> chunkclaims = this.claims.chunkmap.get(chunk);
                if (chunkclaims == null) {
                    continue;
                }
                for (Claim claim : chunkclaims) {
                    if (!claimsToCheck.contains(claim)) {
                        claimsToCheck.add(claim);
                    }
                }
            }
        }

        for (int i = 0; i < claimsToCheck.size(); i++) {
            Claim otherClaim = claimsToCheck.get(i);

            // if we find an existing claim which will be overlapped
            if (otherClaim.overlaps(newClaim)) {
                // result = fail, return conflicting claim
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
        // otherwise add this new claim to the data store to make it effective
        this.addClaim(newClaim);

        // then return success along with reference to new claim
        result.succeeded = CreateClaimResult.Result.SUCCESS;
        result.claim = newClaim;
        return result;
    }

    /**
     * saves changes to player data to secondary storage.
     * MUST be called after you're done making changes, otherwise a reload will lose them.
     *
     * @param playerName
     * @param playerData
     */
    public abstract void savePlayerData(String playerName, PlayerData playerData);

    /**
     * Extends a claim to a new depth while respecting the max depth config variable.
     *
     * @param claim    The claim to act on.
     * @param newDepth The new depth to extend it to.
     */
    synchronized public void extendClaim(Claim claim, int newDepth) {

        WorldConfig wc = GriefPrevention.instance.getWorldCfg(claim.getLesserBoundaryCorner().getWorld());

        if (newDepth < wc.getClaimsMaxDepth()) newDepth = wc.getClaimsMaxDepth();

        if (claim.getParent() != null) claim = claim.getParent();

        claim.lesserBoundaryCorner.setY(newDepth);
        claim.greaterBoundaryCorner.setY(newDepth);

        // make all subdivisions reach to the same depth
        for (int i = 0; i < claim.getChildren().size(); i++) {
            claim.getChildren().get(i).lesserBoundaryCorner.setY(newDepth);
            claim.getChildren().get(i).greaterBoundaryCorner.setY(newDepth);
        }
        saveClaim(claim);
    }

    /**
    // deletes all claims owned by a player with the exception of locked claims
    @Deprecated
    synchronized public void deleteClaimsForPlayer(String playerName, boolean deleteCreativeClaims) {
        deleteClaimsForPlayer(playerName, deleteCreativeClaims, false);
    }

    /**
     * Deletes all claims owned by a player
     *
     * @param playerName           Case SeNsItIvE player name
     * @param deleteCreativeClaims Delete all the player's creative claims?
     * @param deleteLockedClaims   Should we delete claims that have been locked to not delete?
     */
    synchronized public void deleteClaimsForPlayer(String playerName, boolean deleteCreativeClaims, boolean deleteLockedClaims) {
        // make a list of the player's claims
        ArrayList<Claim> claimsToDelete = new ArrayList<Claim>();
        for (int i = 0; i < this.claims.size(); i++) {
            Claim claim = this.claims.get(i);
            if (claim.getOwnerName().equals(playerName) &&
                    (deleteCreativeClaims || !GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) &&
                    (!claim.isNeverdelete() || deleteLockedClaims)) {
                claimsToDelete.add(claim);
            }
        }

        // delete them one by one
        for (int i = 0; i < claimsToDelete.size(); i++) {
            Claim claim = claimsToDelete.get(i);
            claim.removeSurfaceFluids(null);
            this.deleteClaim(claim);
            if (GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                GriefPrevention.instance.restoreClaim(claim, 0);
            }
        }
    }

    synchronized public CreateClaimResult resizeClaim(Claim claim, Location p1, Location p2, Player resizer) {
        int x1 = Math.min(p1.getBlockX(), p2.getBlockX());
        int y1 = Math.min(p1.getBlockY(), p2.getBlockY());
        int z1 = Math.min(p1.getBlockZ(), p2.getBlockZ());

        int x2 = Math.max(p1.getBlockX(), p2.getBlockX());
        int y2 = Math.max(p1.getBlockY(), p2.getBlockY());
        int z2 = Math.max(p1.getBlockZ(), p2.getBlockZ());

        return resizeClaim(claim, x1, x2, y1, y2, z1, z2, resizer);

    }

    /**
     * Tries to resize a claim
     *
     * @param claim The claim to resize
     * @param newx1 corner 1 x
     * @param newx2 corner 2 x
     * @param newy1 corner 1 y
     * @param newy2 corner 2 y
     * @param newz1 corner 1 z
     * @param newz2 corner 2 z
     * @return
     */
    synchronized public CreateClaimResult resizeClaim(Claim claim, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2, Player claimcreator) {

        Location newLesser = new Location(claim.getLesserBoundaryCorner().getWorld(), newx1, newy1, newz1);
        Location newGreater = new Location(claim.getLesserBoundaryCorner().getWorld(), newx2, newy2, newz2);

        ClaimResizeEvent cre = new ClaimResizeEvent(claim, newLesser, newGreater, claimcreator);
        Bukkit.getPluginManager().callEvent(cre);
        if (cre.isCancelled()) {
            CreateClaimResult res = new CreateClaimResult();
            res.claim = claim;
            res.succeeded = CreateClaimResult.Result.CANCELED;
            return res;
        }


        // remove old claim. We don't raise an event for this!
        this.deleteClaim(claim, false, claimcreator);

        // try to create this new claim, ignoring the original when checking for overlap
        CreateClaimResult result = this.createClaim(claim.getLesserBoundaryCorner().getWorld(),
                newx1, newx2, newy1, newy2, newz1, newz2,
                claim.getOwnerName(), claim.getParent(), claim.getID(), claim.isNeverdelete(), claim, claimcreator, false);

        // if succeeded
        if (result.succeeded == CreateClaimResult.Result.SUCCESS) {
            // copy permissions from old claim
            ArrayList<String> builders = new ArrayList<String>();
            ArrayList<String> containers = new ArrayList<String>();
            ArrayList<String> accessors = new ArrayList<String>();
            ArrayList<String> managers = new ArrayList<String>();
            claim.getPermissions(builders, containers, accessors, managers);

            for (int i = 0; i < builders.size(); i++)
                result.claim.setPermission(builders.get(i), ClaimPermission.Build);

            for (int i = 0; i < containers.size(); i++)
                result.claim.setPermission(containers.get(i), ClaimPermission.Inventory);

            for (int i = 0; i < accessors.size(); i++)
                result.claim.setPermission(accessors.get(i), ClaimPermission.Access);

            for (int i = 0; i < managers.size(); i++) {
                result.claim.addManager(managers.get(i));
                // result.claim.managers.add(managers.get(i));
            }

            // copy subdivisions from old claim
            for (int i = 0; i < claim.getChildren().size(); i++) {
                Claim subdivision = claim.getChildren().get(i);
                subdivision.setParent(result.claim);
                result.claim.getChildren().add(subdivision);
            }

            // save those changes
            this.saveClaim(result.claim);
        } else {
            // put original claim back
            this.addClaim(claim);
        }

        return result;
    }

    synchronized public Long[] getClaimIds() {
        return claims.claimmap.keySet().toArray(new Long[claims.claimmap.size()]);
    }

    public abstract void close();

    public int getClaimsSize() {
        return claims.size();
    }

    private String getChunk(Location loc) {
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        return loc.getWorld().getName() + ";" + chunkX + "," + chunkZ;
    }
}
