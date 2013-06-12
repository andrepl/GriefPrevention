package me.ryanhamshire.GriefPrevention.configuration;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.data.Claim;
import me.ryanhamshire.GriefPrevention.data.PlayerData;
import me.ryanhamshire.GriefPrevention.messages.Messages;
import me.ryanhamshire.GriefPrevention.messages.TextMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

// this enum is used for some of the configuration options.

// holds data pertaining to an option and where it works. 
// used primarily for information on explosions.
@SuppressWarnings("unused")
public class ClaimBehaviourData {

	public enum ClaimAllowanceConstants {
        ALLOW_FORCED,
        ALLOW,
        DENY,
        DENY_FORCED;

		public boolean allowed(){ return this == ALLOW || this == ALLOW_FORCED;}
		public boolean denied(){ return this == DENY || this == DENY_FORCED;}
	}


    public enum ClaimBehaviourMode {
        REQUIRE_NONE,
        FORCE_ALLOW,
        REQUIRE_OWNER,
        REQUIRE_MANAGER,
        REQUIRE_ACCESS,
        REQUIRE_CONTAINER,
        DISABLED,
        REQUIRE_BUILD;

		public static ClaimBehaviourMode parseMode(String name) {
			for(ClaimBehaviourMode cb: ClaimBehaviourMode.values()){
				if(cb.name().equalsIgnoreCase(name))
					return cb;
			}
			return ClaimBehaviourMode.REQUIRE_NONE;
		}

        public boolean performTest(GriefPrevention plugin, Location testLocation, Player testPlayer, boolean ShowMessages) {
            PlayerData pd;
            if (testPlayer == null) return true;
            pd = plugin.getDataStore().getPlayerData(testPlayer.getName());
            if ((pd != null) && pd.isIgnoreClaims() || this == REQUIRE_NONE) return true;
            String result;
            Claim atPosition  = plugin.getDataStore().getClaimAt(testLocation, false, null);
            if(atPosition == null) return true; //unexpected...
            switch(this) {
                case DISABLED:
                    plugin.sendMessage(testPlayer, TextMode.ERROR, Messages.ConfigDisabled);
                    return false;
                case REQUIRE_NONE:
                    return true;
                case REQUIRE_OWNER:
                    if (atPosition.getOwnerName().equalsIgnoreCase(testPlayer.getName())) {
                        return true;
                    } else {
                        if (ShowMessages) plugin.sendMessage(testPlayer, TextMode.ERROR, "You need to Own the claim to do that.");
                        return false;
                    }
                case REQUIRE_MANAGER:
                    if(atPosition.isManager(testPlayer.getName())){
                        return true; //success
                    } else {
                        //failed! if showmessages is on, show that message.
                        if(ShowMessages) plugin.sendMessage(testPlayer, TextMode.ERROR, "You need to have Manager trust to do that.");
                        return false;
                    }
                case REQUIRE_BUILD:
                    if(null == (result = atPosition.allowBuild(testPlayer))) {
                        return true; //success
                    } else {
                        //failed! if showmessages is on, show that message.
                        if(ShowMessages) plugin.sendMessage(testPlayer, TextMode.ERROR, result);
                        return false;
                    }
                case REQUIRE_ACCESS:
                    if (null == (result = atPosition.allowAccess(testPlayer))){
                        return true; //success
                    } else {
                        //failed! if showmessages is on, show that message.
                        if(ShowMessages)
                            plugin.sendMessage(testPlayer, TextMode.ERROR, result);
                        return false;
                    }
                case REQUIRE_CONTAINER:
                    if (null == (result = atPosition.allowContainers(testPlayer))) {
                        return true; //success
                    } else {
                        //failed! if displayMessages is on, show that message.
                        if(ShowMessages) plugin.sendMessage(testPlayer, TextMode.ERROR, result);
                        return false;
                    }
                default:
                    return false;
            }
        }
	}

    GriefPrevention plugin;
    private String behaviourName;
    private PlacementRules wilderness;
    private PlacementRules claims;
    private ClaimBehaviourMode claimBehaviour;

    public ClaimBehaviourData(ClaimBehaviourData source) {
        this.plugin = source.plugin;
        this.behaviourName = source.behaviourName;
        this.claims = new PlacementRules(source.claims);
        this.wilderness = new PlacementRules(source.wilderness);
        this.claimBehaviour = source.claimBehaviour;
    }

    public ClaimBehaviourMode getBehaviourMode() {
        return claimBehaviour;
    }

    public ClaimBehaviourData setBehaviourMode(ClaimBehaviourMode b) {
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
     * @return whether this behaviour is allowed or Denied in this claim.
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
    * @return whether this behaviour is allowed or Denied in this claim.
    */
    public ClaimAllowanceConstants allowed(Location position, Player relevantPlayer, boolean displayMessages) {
		String result = null;
        PlayerData pd = null;
        boolean ignoringClaims = false;
        if (relevantPlayer != null) {
            pd = plugin.getDataStore().getPlayerData(relevantPlayer.getName());
        }
        if (pd != null) {
            ignoringClaims = pd.isIgnoreClaims();
        }
        if (ignoringClaims) {
            return ClaimAllowanceConstants.ALLOW;
        }
		Claim testClaim = plugin.getDataStore().getClaimAt(position, true, null);
		if (testClaim != null) {
            if (!this.claimBehaviour.performTest(plugin, position, relevantPlayer, displayMessages)) {
                return ClaimAllowanceConstants.DENY;
            }
            boolean varResult =  this.claims.allow(plugin, position, relevantPlayer, displayMessages);
            return varResult ? ClaimAllowanceConstants.ALLOW : ClaimAllowanceConstants.DENY;
        }
        ClaimAllowanceConstants wildernessResult = wilderness.allow(plugin, position, relevantPlayer, false) ? ClaimAllowanceConstants.ALLOW : ClaimAllowanceConstants.DENY;
        if (wildernessResult.denied() && displayMessages) {
            plugin.sendMessage(relevantPlayer, TextMode.ERROR, Messages.ConfigDisabled, this.behaviourName);
        }
        return wildernessResult;
	}

    /**
     * retrieves the placement rules for this Behaviour outside claims (in the 'wilderness')
     * @return PlacementRules instance encapsulating applicable placement rules.
     */
	public PlacementRules getWildernessRules() {
        return wilderness;
    }

    /**
     * retrieves the placement rules for this Behaviour inside claims.
     * @return PlacementRules instance encapsulating applicable placement rules.
     */
	public PlacementRules getClaimsRules() {
        return claims;
    }

    /**
     * retrieves the name for this Behaviour. This will be used in any applicable messages.
     * @return Name for this behaviour.
     */
	public String getBehaviourName() {
        return behaviourName;
    }

    @Override
	public String toString() {
		return behaviourName + " in the wilderness " + getWildernessRules().toString() + " and in claims " + getClaimsRules().toString();
	}
	
	public ClaimBehaviourData(GriefPrevention plugin, String pName, FileConfiguration source, FileConfiguration outConfig, String nodePath, ClaimBehaviourData defaults) {
		this.plugin = plugin;
        behaviourName = pName;
		// we want to read NodePath.BelowSeaLevelWilderness and whatnot.
		// bases Defaults off another ClaimBehaviourData instance.
		wilderness = new PlacementRules(source, outConfig, nodePath + ".Wilderness", defaults.getWildernessRules());
		claims = new PlacementRules (source, outConfig, nodePath + ".Claims", defaults.getClaimsRules());

		String claimBehaviour = source.getString(nodePath + ".Claims.Behaviour","None");
		this.claimBehaviour = ClaimBehaviourMode.parseMode(claimBehaviour);
		
		outConfig.set(nodePath + ".Claims.Behaviour", this.claimBehaviour.name());
	}

	public ClaimBehaviourData(GriefPrevention plugin, String pName, PlacementRules pWilderness, PlacementRules pClaims, ClaimBehaviourMode cb) {
        this.plugin = plugin;
		wilderness = pWilderness;
		claims = pClaims;
		claimBehaviour = cb;
		behaviourName = pName;
	}

	public static ClaimBehaviourData getOutsideClaims(GriefPrevention plugin, String pName) {
        return new ClaimBehaviourData(plugin, pName, PlacementRules.BOTH, PlacementRules.NEITHER, ClaimBehaviourMode.REQUIRE_NONE);
    }

    public static ClaimBehaviourData getInsideClaims(GriefPrevention plugin, String pName) {
        return new ClaimBehaviourData(plugin, pName, PlacementRules.NEITHER, PlacementRules.NEITHER, ClaimBehaviourMode.REQUIRE_NONE);
    }

    public static ClaimBehaviourData getAboveSeaLevel(GriefPrevention plugin, String pName) {
        return new ClaimBehaviourData(plugin, pName, PlacementRules.ABOVE_ONLY, PlacementRules.ABOVE_ONLY, ClaimBehaviourMode.REQUIRE_NONE);
    }

    public static ClaimBehaviourData getBelowSeaLevel(GriefPrevention plugin, String pName) {
        return new ClaimBehaviourData(plugin, pName, PlacementRules.BELOW_ONLY, PlacementRules.BELOW_ONLY, ClaimBehaviourMode.REQUIRE_NONE);
    }

    public static ClaimBehaviourData getNone(GriefPrevention plugin, String pName) {
        return new ClaimBehaviourData(plugin, pName, PlacementRules.NEITHER, PlacementRules.NEITHER, ClaimBehaviourMode.REQUIRE_NONE);
    }

    public static ClaimBehaviourData getAll(GriefPrevention plugin, String pName) {
        return new ClaimBehaviourData(plugin, pName, PlacementRules.BOTH, PlacementRules.BOTH, ClaimBehaviourMode.REQUIRE_NONE);
    }
}