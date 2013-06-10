package me.ryanhamshire.GriefPrevention.data;

import org.bukkit.Chunk;

import java.util.*;

public class ClaimMap {
    private HashMap<UUID, Claim> byId;
    private HashMap<String, HashMap<Long, ArrayList<Claim>>> byChunk;
    private HashMap<UUID, Claim> childrenById;

    public ClaimMap() {
        byId = new HashMap<UUID, Claim>();
        byChunk = new HashMap<String, HashMap<Long, ArrayList<Claim>>>();
        childrenById = new HashMap<UUID, Claim>();
    }

    public void add(Claim claim) {
        claim.setInDataStore(true);
        if (claim.getParent() != null) {
            childrenById.put(claim.getId(), claim);
            return;
        }
        byId.put(claim.getId(), claim);
        Long[] chunkKeys = getChunks(claim);
        String worldName = claim.getLesserBoundaryCorner().getWorld().getName();
        if (!byChunk.containsKey(worldName)) {
            byChunk.put(worldName, new HashMap<Long, ArrayList<Claim>>());
        }
        HashMap<Long, ArrayList<Claim>> worldMap = byChunk.get(worldName);
        for (long point: chunkKeys) {
            ArrayList<Claim> aclaims = worldMap.get(point);
            if(aclaims == null) {
                aclaims = new ArrayList<Claim>();
                aclaims.add(claim);
                worldMap.put(point, aclaims);
            }else {
                int k = 0;
                while(k < aclaims.size() && !aclaims.get(k).greaterThan(claim)) k++;
                if (k < aclaims.size()) {
                    aclaims.add(k, claim);
                } else {
                    aclaims.add(aclaims.size(), claim);
                }
            }
        }
        // Add Children
        for (Claim child: claim.getChildren()) {
            childrenById.put(child.getId(), child);
        }
    }

    public void remove(Claim claim) {
        if (claim.getParent() == null) {
            claim.setInDataStore(false);
            // top level claims must their children removed
            byId.remove(claim.getId());
            // and be removed from the chunk map
            HashMap<Long, ArrayList<Claim>> worldMap = byChunk.get(claim.getLesserBoundaryCorner().getWorld().getName());
            if (worldMap != null) {
                for (long point: getChunks(claim)) {
                    if (worldMap.containsKey(point)) {
                        worldMap.get(point).remove(claim);
                    }
                }
            }
            // Remove children
            for (Claim childClaim: claim.getChildren()) {
                claim.setInDataStore(false);
                childrenById.remove(childClaim.getId());
            }
        } else {
            childrenById.remove(claim.getId());
        }
    }

    public int size() {
        return byId.size();
    }
    // Packs 2 ints (x,z) into a long
    private static long point(int x, int z) {
        return (((long)x) << 32) | z;
    }

    // Extracts the x coordinate from a point long.
    private static int x(long point) {
        return (int)(point >> 32);
    }

    // Extracts the z coordinate from a point long.
    private static int z(long point) {
        return (int) point;
    }

    public Claim get(UUID id) {
        Claim c = byId.get(id);
        return c;
    }


    public static Long[] getChunks(Claim claim) {
        int lx = claim.getLesserBoundaryCorner().getBlockX();
        int lz = claim.getLesserBoundaryCorner().getBlockZ();
        int gx = claim.getGreaterBoundaryCorner().getBlockX();
        int gz = claim.getGreaterBoundaryCorner().getBlockZ();

        ArrayList<Long> chunks = new ArrayList<Long>();

        for (int tx = lx; (tx >> 4) <= (gx >> 4); tx += 16) {
            for (int tz = lz; (tz >> 4) <= (gz >> 4); tz += 16) {
                int chunkX = tx >> 4;
                int chunkZ = tz >> 4;
                chunks.add(point(chunkX,chunkZ));
            }
        }
        return chunks.toArray(new Long[chunks.size()]);
    }

    public Collection<Claim> getAllTopLevel() {
        return byId.values();
    }

    public ArrayList<Claim> getForChunk(Chunk chunk) {
        return getForChunk(chunk.getWorld().getName(), point(chunk.getX(), chunk.getZ()));
    }

    public ArrayList<Claim> getForChunk(String worldName, long chunkPoint) {
        if (!byChunk.containsKey(worldName)) {
            return null;
        }
        return byChunk.get(worldName).get(chunkPoint);
    }

    public Collection<Claim> getPossiblyOverlappingClaims(Claim claim) {
        Long[] chunks = getChunks(claim);
        HashSet<Claim> claims = new HashSet<Claim>();
        HashMap<Long, ArrayList<Claim>> worldMap = byChunk.get(claim.getLesserBoundaryCorner().getWorld().getName());
        for (long point: chunks) {
            if (worldMap.containsKey(point)) {
                claims.addAll(worldMap.get(chunks));
            }
        }
        return claims;
    }

    public UUID[] getTopLevelClaimIDs() {
        UUID[] ids = new UUID[byId.size()];
        int i = 0;
        for (UUID key: byId.keySet()) {
            ids[i++] = key;
        }
        return ids;
    }

    public boolean contains(UUID id) {
        return byId.containsKey(id);
    }

}
