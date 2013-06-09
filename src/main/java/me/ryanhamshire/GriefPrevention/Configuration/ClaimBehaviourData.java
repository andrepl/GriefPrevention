package me.ryanhamshire.GriefPrevention.Configuration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
// this enum is used for some of the configuration options.
import org.bukkit.entity.Player;

// holds data pertaining to an option and where it works. 
// used primarily for information on explosions.
public class ClaimBehaviourData {
	public enum ClaimAllowanceConstants {
		Allow_Forced,
		Allow,
		Deny,
		Deny_Forced;

		public boolean Allowed(){ return this==Allow || this==Allow_Forced;}
		public boolean Denied(){ return this==Deny || this==Deny_Forced;}
	}


    public enum ClaimBehaviourMode{
		None,
		ForceAllow,
		RequireOwner,
		RequireManager,
		RequireAccess,
		RequireContainer;

		public static ClaimBehaviourMode parseMode(String name){
			for(ClaimBehaviourMode cb:ClaimBehaviourMode.values()){
				if(cb.name().equalsIgnoreCase(name))
					return cb;
			}
			return ClaimBehaviourMode.None;
		}
	}


    private String behaviourName;
    private PlacementRules wilderness;
	private PlacementRules claims;
	private ClaimBehaviourMode claimBehaviour = ClaimBehaviourMode.None;
	
	public ClaimBehaviourMode getClaimBehaviour(){
        return claimBehaviour;
    }
	
	public ClaimAllowanceConstants Allowed(Location position,Player RelevantPlayer){
		String result=null;
		Claim testclaim = GriefPrevention.instance.dataStore.getClaimAt(position, true, null);
		if(claimBehaviour !=ClaimBehaviourMode.None && testclaim!=null){
			// if forcibly allowed, allow.
			if(claimBehaviour == ClaimBehaviourMode.ForceAllow){
				return ClaimAllowanceConstants.Allow;
			}
			else if(claimBehaviour == ClaimBehaviourMode.RequireOwner){
				// RequireOwner means it only applies if the player passed is the owner.
				// if the passed player is null, then we assume the operation has no relevant player, so allow it in this case.
				if(RelevantPlayer!=null){
					if(!testclaim.getOwnerName().equalsIgnoreCase(RelevantPlayer.getName())){
						// they aren't the owner, so fail the test.
						return ClaimAllowanceConstants.Deny;
					}
				}
			}
			else if(claimBehaviour == ClaimBehaviourMode.RequireAccess){
				if(RelevantPlayer!=null){
					if(null!=(result =testclaim.allowAccess(RelevantPlayer))){
						GriefPrevention.sendMessage(RelevantPlayer, TextMode.ERROR, result);
						return ClaimAllowanceConstants.Deny;
					}
				}
			}
			else if(claimBehaviour == ClaimBehaviourMode.RequireContainer){
				if(RelevantPlayer!=null){
					if(null!=(result = testclaim.allowContainers(RelevantPlayer))){
						GriefPrevention.sendMessage(RelevantPlayer, TextMode.ERROR, result);
						return ClaimAllowanceConstants.Deny;
					}
				}
			}
			else if(claimBehaviour == ClaimBehaviourMode.RequireManager){
				if(RelevantPlayer!=null){
					if(!testclaim.isManager(RelevantPlayer.getName())){
						return ClaimAllowanceConstants.Deny;
					}
				}
			}
		}

		if(testclaim==null){
			// we aren't inside a claim.
			return wilderness.allow(position)?ClaimAllowanceConstants.Allow:ClaimAllowanceConstants.Deny;
		} else {
			// we are inside a claim.
			return claims.allow(position)?ClaimAllowanceConstants.Allow:ClaimAllowanceConstants.Deny;
		}
	}

	public PlacementRules getWildernessRules() {
        return wilderness;
    }

	public PlacementRules getClaimsRules(){
        return claims;
    }

	public String getBehaviourName() {
        return behaviourName;
    }

    @Override
	public String toString(){
		return behaviourName + " in the wilderness " + getWildernessRules().toString() + " and in claims " + getClaimsRules().toString();
	}
	
	public ClaimBehaviourData(String pName, FileConfiguration source, FileConfiguration outConfig, String nodePath, ClaimBehaviourData defaults){
		behaviourName = pName;
		// we want to read NodePath.BelowSeaLevelWilderness and whatnot.
		// bases Defaults off another ClaimBehaviourData instance.
		wilderness = new PlacementRules(source,outConfig,nodePath + ".Wilderness",defaults.getWildernessRules());
		claims = new PlacementRules (source,outConfig,nodePath + ".Claims",defaults.getClaimsRules());
		
		
		String claimbehave = source.getString(nodePath + ".Claims.Behaviour","None");
		claimBehaviour = ClaimBehaviourMode.parseMode(claimbehave);
		
		outConfig.set(nodePath + ".Claims.Behaviour", claimBehaviour.name());
	}

	public ClaimBehaviourData(String pName,PlacementRules pWilderness,PlacementRules pClaims, ClaimBehaviourMode cb) {
		wilderness = pWilderness;
		claims = pClaims;
		claimBehaviour = cb;
		behaviourName =pName;
	}

	public static ClaimBehaviourData getOutsideClaims(String pName) { return new ClaimBehaviourData(pName,PlacementRules.Both,PlacementRules.Neither,ClaimBehaviourMode.None); }
	public static ClaimBehaviourData getInsideClaims(String pName) { return new ClaimBehaviourData(pName,PlacementRules.Neither,PlacementRules.Neither,ClaimBehaviourMode.None); }
	public static ClaimBehaviourData getAboveSeaLevel(String pName){ return new ClaimBehaviourData(pName,PlacementRules.AboveOnly,PlacementRules.AboveOnly,ClaimBehaviourMode.None); }
	public static ClaimBehaviourData getBelowSeaLevel(String pName){ return new ClaimBehaviourData(pName,PlacementRules.BelowOnly,PlacementRules.BelowOnly,ClaimBehaviourMode.None); }
	public static ClaimBehaviourData getNone(String pName){ return new ClaimBehaviourData(pName,PlacementRules.Neither,PlacementRules.Neither,ClaimBehaviourMode.None); }
	public static ClaimBehaviourData getAll(String pName){ return new ClaimBehaviourData(pName,PlacementRules.Both,PlacementRules.Both,ClaimBehaviourMode.None); }
}