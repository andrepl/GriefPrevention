package me.ryanhamshire.GriefPrevention.configuration;

import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.Messages;
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
        REQUIRE_NONE,
        FORCE_ALLOW,
        REQUIRE_OWNER,
        REQUIRE_MANAGER,
        REQUIRE_ACCESS,
        REQUIRE_CONTAINER,
        DISABLED,
        REQUIRE_BUILD;


		public static ClaimBehaviourMode parseMode(String name){
			for(ClaimBehaviourMode cb:ClaimBehaviourMode.values()){
				if(cb.name().equalsIgnoreCase(name))
					return cb;
			}
			return ClaimBehaviourMode.REQUIRE_NONE;
		}
        public boolean performTest(Location testLocation, Player testPlayer, boolean ShowMessages) {
            WorldConfig wc = GriefPrevention.instance.getWorldCfg(testLocation.getWorld());
            PlayerData pd = null;
            if (testPlayer == null) return true;
            if (testPlayer != null) pd = GriefPrevention.instance.dataStore.getPlayerData(testPlayer.getName());
            if ((pd != null) && pd.isIgnoreClaims() || this == REQUIRE_NONE) return true;
            String result = null;
            Claim atPosition  = GriefPrevention.instance.dataStore.getClaimAt(testLocation, false, null);
            if(atPosition == null) return true; //unexpected...
            switch(this) {
                case DISABLED:
                    GriefPrevention.sendMessage(testPlayer, TextMode.ERROR, Messages.ConfigDisabled);
                    return false;
                case REQUIRE_NONE:
                    return true;
                case REQUIRE_OWNER:
                    if (atPosition.getOwnerName().equalsIgnoreCase(testPlayer.getName())) {
                        return true;
                    } else {
                        if (ShowMessages) GriefPrevention.sendMessage(testPlayer, TextMode.ERROR, "You need to Own the claim to do that.");
                        return false;
                    }
                case REQUIRE_MANAGER:
                    if(atPosition.isManager(testPlayer.getName())){
                        return true; //success
                    } else {
                        //failed! if showmessages is on, show that message.
                        if(ShowMessages) GriefPrevention.sendMessage(testPlayer, TextMode.ERROR, "You need to have Manager trust to do that.");
                        return false;
                    }
                case REQUIRE_BUILD:
                    if(null == (result = atPosition.allowBuild(testPlayer))) {
                        return true; //success
                    } else {
                        //failed! if showmessages is on, show that message.
                        if(ShowMessages) GriefPrevention.sendMessage(testPlayer, TextMode.ERROR, result);
                        return false;
                    }
                case REQUIRE_ACCESS:
                    if (null == (result = atPosition.allowAccess(testPlayer))){
                        return true; //success
                    } else {
                        //failed! if showmessages is on, show that message.
                        if(ShowMessages)
                            GriefPrevention.sendMessage(testPlayer, TextMode.ERROR, result);
                        return false;
                    }
                case REQUIRE_CONTAINER:
                    if (null == (result = atPosition.allowContainers(testPlayer))) {
                        return true; //success
                    } else {
                        //failed! if displayMessages is on, show that message.
                        if(ShowMessages) GriefPrevention.sendMessage(testPlayer, TextMode.ERROR, result);
                        return false;
                    }
                default:
                    return false;
            }
        }
	}


    private String behaviourName;
    private PlacementRules wilderness;
	private PlacementRules claims;
    private ClaimBehaviourMode claimBehaviour;

    public Object clone() {
        return new ClaimBehaviourData(this);
    }

    public ClaimBehaviourData(ClaimBehaviourData source){
        this.behaviourName = source.behaviourName;
        this.claims= (PlacementRules) source.claims.clone();
        this.wilderness = (PlacementRules)source.wilderness.clone();
        this.claimBehaviour = source.claimBehaviour;
    }

    public ClaimBehaviourMode getBehaviourMode() {
        return claimBehaviour;
    }

    public ClaimBehaviourData setBehaviourMode(ClaimBehaviourMode b){
        if (b == null) {
            b = ClaimBehaviourMode.REQUIRE_NONE;
        }
        ClaimBehaviourData cdc = new ClaimBehaviourData(this);
        cdc.claimBehaviour = b;
        return cdc;
    }

    /**
     * returns whether this Behaviour is allowed at the given location. if the passed player currently has
     * ignoreclaims on, this will return true no matter what. This delegates to the overload that displays messages
     * and passes true for the omitted argument.
     * @param position Position to test.
     * @param relevantPlayer Player to test. Can be null for actions or behaviours that do not involve a player.
     * @return whether this behaviour is Allowed or Denied in this claim.
     */
    public ClaimAllowanceConstants allowed(Location position, Player relevantPlayer) {
        return allowed(position, relevantPlayer, true);
    }

   /**
    * returns whether this Behaviour is allowed at the given location. if the passed player currently has
    * ignoreclaims on, this will return true no matter what. This delegates to the overload that displays messages
    * and passes true for the omitted argument.
    * @param position Position to test.
    * @param relevantPlayer Player to test. Can be null for actions or behaviours that do not involve a player.
    * @param displayMessages whether or not to display a messages when denied
    * @return whether this behaviour is Allowed or Denied in this claim.
    */
    public ClaimAllowanceConstants allowed(Location position, Player relevantPlayer, boolean displayMessages) {
		String result = null;
        PlayerData pd = null;
        boolean ignoringclaims = false;
        if (relevantPlayer != null) {
            pd = GriefPrevention.instance.dataStore.getPlayerData(relevantPlayer.getName());
        }
        if (pd != null) {
            ignoringclaims = pd.isIgnoreClaims();
        }
        if (ignoringclaims) {
            return ClaimAllowanceConstants.ALLOW;
        }
		Claim testClaim = GriefPrevention.instance.dataStore.getClaimAt(position, true, null);
		if (testClaim != null) {
            if (!this.claimBehaviour.performTest(position, relevantPlayer, displayMessages)) {
                return ClaimAllowanceConstants.DENY;
            }
            boolean varresult =  this.claims.allow(position, relevantPlayer, displayMessages);
            return varresult ? ClaimAllowanceConstants.ALLOW : ClaimAllowanceConstants.DENY;
        }
        ClaimAllowanceConstants wildernessResult = wilderness.allow(position, relevantPlayer, false) ? ClaimAllowanceConstants.ALLOW : ClaimAllowanceConstants.DENY;
        if (wildernessResult.Denied() && displayMessages) {
            GriefPrevention.sendMessage(relevantPlayer, TextMode.ERROR, Messages.ConfigDisabled, this.behaviourName);
        }
        return wildernessResult;
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

	public static ClaimBehaviourData getOutsideClaims(String pName) { return new ClaimBehaviourData(pName,PlacementRules.BOTH,PlacementRules.NEITHER,ClaimBehaviourMode.REQUIRE_NONE); }
	public static ClaimBehaviourData getInsideClaims(String pName) { return new ClaimBehaviourData(pName,PlacementRules.NEITHER,PlacementRules.NEITHER,ClaimBehaviourMode.REQUIRE_NONE); }
	public static ClaimBehaviourData getAboveSeaLevel(String pName) { return new ClaimBehaviourData(pName,PlacementRules.ABOVE_ONLY,PlacementRules.ABOVE_ONLY,ClaimBehaviourMode.REQUIRE_NONE); }
	public static ClaimBehaviourData getBelowSeaLevel(String pName) { return new ClaimBehaviourData(pName,PlacementRules.BELOW_ONLY,PlacementRules.BELOW_ONLY,ClaimBehaviourMode.REQUIRE_NONE); }
	public static ClaimBehaviourData getNone(String pName) { return new ClaimBehaviourData(pName,PlacementRules.NEITHER,PlacementRules.NEITHER,ClaimBehaviourMode.REQUIRE_NONE); }
	public static ClaimBehaviourData getAll(String pName) { return new ClaimBehaviourData(pName,PlacementRules.BOTH,PlacementRules.BOTH,ClaimBehaviourMode.REQUIRE_NONE); }
}