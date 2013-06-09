package me.ryanhamshire.GriefPrevention.configuration;

import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
// this enum is used for some of the configuration options.
import org.bukkit.entity.Player;

// holds data pertaining to an option and where it works. 
// used primarily for information on explosions.
public class ClaimBehaviourData {
	public enum ClaimAllowanceConstants {
        ALLOW_FORCED,
        ALLOW,
        DENY,
        DENY_FORCED;

		public boolean Allowed(){ return this == ALLOW || this == ALLOW_FORCED;}
		public boolean Denied(){ return this == DENY || this == DENY_FORCED;}
	}


    public enum ClaimBehaviourMode{
        NONE,
        FORCE_ALLOW,
        REQUIRE_OWNER,
        REQUIRE_MANAGER,
        REQUIRE_ACCESS,
        REQUIRE_CONTAINER;

		public static ClaimBehaviourMode parseMode(String name){
			for(ClaimBehaviourMode cb:ClaimBehaviourMode.values()){
				if(cb.name().equalsIgnoreCase(name))
					return cb;
			}
			return ClaimBehaviourMode.NONE;
		}
	}


    private String behaviourName;
    private PlacementRules wilderness;
	private PlacementRules claims;
	private ClaimBehaviourMode claimBehaviour = ClaimBehaviourMode.NONE;
	
	public ClaimBehaviourMode getClaimBehaviour() {
        return claimBehaviour;
    }
	
	public ClaimAllowanceConstants allowed(Location position, Player relevantPlayer) {
		String result=null;
		Claim testClaim = GriefPrevention.instance.dataStore.getClaimAt(position, true, null);
		if (claimBehaviour != ClaimBehaviourMode.NONE && testClaim != null) {
			// if forcibly allowed, allow.
			if (claimBehaviour == ClaimBehaviourMode.FORCE_ALLOW) {
				return ClaimAllowanceConstants.ALLOW;
			} else if (claimBehaviour == ClaimBehaviourMode.REQUIRE_OWNER) {
				// RequireOwner means it only applies if the player passed is the owner.
				// if the passed player is null, then we assume the operation has no relevant player, so allow it in this case.
				if (relevantPlayer != null) {
					if (!testClaim.getOwnerName().equalsIgnoreCase(relevantPlayer.getName())) {
						// they aren't the owner, so fail the test.
						return ClaimAllowanceConstants.DENY;
					}
				}
			} else if (claimBehaviour == ClaimBehaviourMode.REQUIRE_ACCESS) {
				if (relevantPlayer != null) {
					if (null != (result = testClaim.allowAccess(relevantPlayer))) {
						GriefPrevention.sendMessage(relevantPlayer, TextMode.ERROR, result);
						return ClaimAllowanceConstants.DENY;
					}
				}
			} else if (claimBehaviour == ClaimBehaviourMode.REQUIRE_CONTAINER) {
				if (relevantPlayer!=null){
					if (null != (result = testClaim.allowContainers(relevantPlayer))) {
						GriefPrevention.sendMessage(relevantPlayer, TextMode.ERROR, result);
						return ClaimAllowanceConstants.DENY;
					}
				}
            } else if (claimBehaviour == ClaimBehaviourMode.REQUIRE_MANAGER){
				if(relevantPlayer != null){
					if(!testClaim.isManager(relevantPlayer.getName())){
						return ClaimAllowanceConstants.DENY;
					}
				}
			}
		}

		if(testClaim == null){
			// we aren't inside a claim.
			return wilderness.allow(position) ? ClaimAllowanceConstants.ALLOW : ClaimAllowanceConstants.DENY;
		} else {
			// we are inside a claim.
			return claims.allow(position) ? ClaimAllowanceConstants.ALLOW : ClaimAllowanceConstants.DENY;
		}
	}

	public PlacementRules getWildernessRules() {
        return wilderness;
    }

	public PlacementRules getClaimsRules() {
        return claims;
    }

	public String getBehaviourName() {
        return behaviourName;
    }

    @Override
	public String toString() {
		return behaviourName + " in the wilderness " + getWildernessRules().toString() + " and in claims " + getClaimsRules().toString();
	}
	
	public ClaimBehaviourData(String pName, FileConfiguration source, FileConfiguration outConfig, String nodePath, ClaimBehaviourData defaults){
		behaviourName = pName;
		// we want to read NodePath.BelowSeaLevelWilderness and whatnot.
		// bases Defaults off another ClaimBehaviourData instance.
		wilderness = new PlacementRules(source, outConfig, nodePath + ".Wilderness", defaults.getWildernessRules());
		claims = new PlacementRules (source, outConfig, nodePath + ".Claims", defaults.getClaimsRules());

		String claimBehaviour = source.getString(nodePath + ".Claims.Behaviour","None");
		this.claimBehaviour = ClaimBehaviourMode.parseMode(claimBehaviour);
		
		outConfig.set(nodePath + ".Claims.Behaviour", this.claimBehaviour.name());
	}

	public ClaimBehaviourData(String pName, PlacementRules pWilderness, PlacementRules pClaims, ClaimBehaviourMode cb) {
		wilderness = pWilderness;
		claims = pClaims;
		claimBehaviour = cb;
		behaviourName = pName;
	}

	public static ClaimBehaviourData getOutsideClaims(String pName) { return new ClaimBehaviourData(pName,PlacementRules.BOTH,PlacementRules.NEITHER,ClaimBehaviourMode.NONE); }
	public static ClaimBehaviourData getInsideClaims(String pName) { return new ClaimBehaviourData(pName,PlacementRules.NEITHER,PlacementRules.NEITHER,ClaimBehaviourMode.NONE); }
	public static ClaimBehaviourData getAboveSeaLevel(String pName) { return new ClaimBehaviourData(pName,PlacementRules.ABOVE_ONLY,PlacementRules.ABOVE_ONLY,ClaimBehaviourMode.NONE); }
	public static ClaimBehaviourData getBelowSeaLevel(String pName) { return new ClaimBehaviourData(pName,PlacementRules.BELOW_ONLY,PlacementRules.BELOW_ONLY,ClaimBehaviourMode.NONE); }
	public static ClaimBehaviourData getNone(String pName) { return new ClaimBehaviourData(pName,PlacementRules.NEITHER,PlacementRules.NEITHER,ClaimBehaviourMode.NONE); }
	public static ClaimBehaviourData getAll(String pName) { return new ClaimBehaviourData(pName,PlacementRules.BOTH,PlacementRules.BOTH,ClaimBehaviourMode.NONE); }
}