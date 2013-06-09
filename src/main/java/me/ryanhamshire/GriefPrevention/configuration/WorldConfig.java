package me.ryanhamshire.GriefPrevention.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.SerializationUtil;
import me.ryanhamshire.GriefPrevention.data.MaterialCollection;
import me.ryanhamshire.GriefPrevention.data.MaterialInfo;

import me.ryanhamshire.GriefPrevention.tasks.CleanupUnusedClaimsTask;
import me.ryanhamshire.GriefPrevention.tasks.DeliverClaimBlocksTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * represents the configuration settings of a single world.
 *
 * @author BC_Programming
 *
 */
public class WorldConfig {

	// Explosion and similar effect information.
	// we've moved this to another class for brevity as well as to make it easier to deal with and more flexible.
	// you know- all the standard reasons for moving things into a class. I'll shut up now and get to writing the applicable code.

	private ClaimBehaviourData trashBlockPlacementBehaviour;
    private ClaimBehaviourData creeperExplosionBehaviour; 	// data for Creeper Explosions. This indicates where they can occur.
    private ClaimBehaviourData tntExplosionBehaviour;       // data for TNT Explosions. this indicates where they can occur. Applies for both TNT and TNT minecarts.
    private ClaimBehaviourData witherExplosionBehaviour;
    private ClaimBehaviourData witherEatBehaviour;
    private ClaimBehaviourData otherExplosionBehaviour;
    private ClaimBehaviourData witherSpawnBehaviour;        // data for how Withers can be spawned.
    private ClaimBehaviourData ironGolemSpawnBehaviour;     // data for how IronGolems can be spawned.
    private ClaimBehaviourData snowGolemSpawnBehaviour;     // data for now Snow Golems can be spawned.
    private ClaimBehaviourData waterBucketBehaviour;
    private ClaimBehaviourData lavaBucketBehaviour;
    private ClaimBehaviourData villagerTrades;                     // prevent trades on claims players don't have permissions on
    private ClaimBehaviourData environmentalVehicleDamage;
    private ClaimBehaviourData zombieDoorBreaking;
    private ClaimBehaviourData sheepShearingRules;
    private ClaimBehaviourData sheepDyeing;
    private ClaimBehaviourData bonemealGrass;
    private ClaimBehaviourData playerTrampleRules;

    private boolean claimsEnabled;
    private boolean configClaimsCreativeRules;
    private boolean configEntityCleanupEnabled;
    private boolean configClaimCleanupEnabled;                       // whether the cleanup task is activated.
    private boolean configClaimsPreventTheft;						// whether containers and crafting blocks are protectable
    private boolean configClaimsProtectCreatures;					// whether claimed animals may be injured by players without permission
    private boolean configClaimsPreventButtonsSwitches;			// whether buttons and switches are protectable
    private boolean configClaimsLockWoodenDoors;					// whether wooden doors should be locked by default (require /accesstrust)
    private boolean configClaimsLockTrapDoors;						// whether trap doors should be locked by default (require /accesstrust)
    private boolean configClaimsLockFenceGates;					// whether fence gates should be locked by default (require /accesstrust)
    private boolean configClaimsEnderPearlsRequireAccessTrust;		// whether teleporting into a claim with a pearl requires access trust
    private boolean configClaimsCreationRequiresPermission;		// whether creating claims with the shovel requires a permission
    private boolean configClaimsAllowUnclaim;			// whether players may unclaim land (resize or abandon)
    private boolean configClaimsAutoRestoreUnclaimed; 	// whether unclaimed land in creative worlds is automatically /restorenature-d
    private boolean configClaimsApplyTrashBlockRules;				// whether players can build in survival worlds outside their claimed areas
    private boolean configClaimsAutoNatureRestoration;		// whether survival claims will be automatically restored to nature when auto-deleted
    private boolean configClaimsAbandonNatureRestoration; // whether survival claims will be automatically restored to nature when abandoned.
    private boolean configPVPProtectFreshSpawns;					// whether to make newly spawned players immune until they pick up an item
    private boolean configPVPPunishLogout;						    // whether to kill players who log out during PvP combat
    private boolean configPVPAllowCombatItemDrop;					// whether a player can drop items during combat to hide them
    private boolean configPVPBlockContainers;
    private boolean configPVPNoCombatInPlayerLandClaims;			// whether players may fight in player-owned land claims
    private boolean configPVPNoCombatInAdminLandClaims;			// whether players may fight in admin-owned land claims
    private boolean configTreesTemoveFloatingTreetops;				// whether to automatically remove partially cut trees
    private boolean configTreesRegrowGriefedTrees;					// whether to automatically replant partially cut trees
    private boolean configBlockSkyTrees;							// whether players can build trees on platforms in the sky
    private boolean configFireSpreads;								// whether fire spreads outside of claims
    private boolean configFireDestroys;								// whether fire destroys blocks outside of claims
    private boolean configAddItemsToClaimedChests;					// whether players may add items to claimed chests by left-clicking them
    private boolean configSignEavesdrop;                           // whether to allow sign eavesdropping at all.
    private boolean configEavesdrop; 								// whether whispered messages will be visible to administrators
    private boolean configSmartBan;									// whether to ban accounts which very likely owned by a banned player
    private boolean configEndermenMoveBlocks;						// whether or not endermen may move blocks around
    private boolean configSilverfishBreakBlocks;					// whether silverfish may break blocks
    private boolean configCreaturesTrampleCrops;					// whether or not non-player entities may trample crops
    private boolean configClaimsWarnOnBuildOutside;				// whether players should be warned when they're building in an unclaimed area
    private int configSeaLevelOverride;
    private int configMessageCooldownClaims = 0;      // claims cooldown. 0= no cooldown.
    private int configMessageCooldownStuck = 0;       // stuck cooldown. 0= no cooldown.
    private int configClaimCleanupMaximumSize;        // maximum size of claims to cleanup. larger claims are not cleaned up.
    private int configClaimCleanupMaxInvestmentScore; // maximum investmentscore. claims with a higher score will not be cleaned up. if set to 0, claim cleanup will not have it's score calculated.
    private float configClaimsBlocksAccruedPerHour;					// how many additional blocks players get each hour of play (can be zero)
    private int configClaimsMaxDepth;								// limit on how deep claims can go
    private int configClaimsExpirationDays;						// how many days of inactivity before a player loses his claims
    private int configClaimsAutomaticClaimsForNewPlayersRadius;	// how big automatic new player claims (when they place a chest) should be.  0 to disable
    private int configClaimsClaimsExtendIntoGroundDistance;		// how far below the shoveled block a new claim will reach
    private int configClaimsMinSize;								// minimum width and height for non-admin claims
    private int configClaimsChestClaimExpirationDays;				// number of days of inactivity before an automatic chest claim will be deleted
    private int configClaimsUnusedClaimExpirationDays;				// number of days of inactivity before an unused (nothing build) claim will be deleted
    private int configClaimsTrappedCooldownHours;					// number of hours between uses of the /trapped command
    private int configPVPCombatTimeoutSeconds;						// how long combat is considered to continue after the most recent damage
    private int configClaimsWildernessBlocksDelay;                   	// the number of non-trash blocks that can be placed before warning.  0 disables the display entirely.
    private int configClaimsPerPlayerClaimLimit;                        // maximum number of claims a user can have.
    private double configClaimsAbandonReturnRatio;                // return ratio when abandoning a claim- .80 will result in players getting 80% of the used claim blocks back.
    private String WorldName;
    private List<Material> configTrashBlocks = null;
    private List<String> configModsIgnoreClaimsAccounts;			// list of player names which ALWAYS ignore claims
    private MaterialCollection configModsExplodableIds;			// list of block IDs which can be destroyed by explosions, even in claimed areas
    private Material configClaimsInvestigationTool;				// which material will be used to investigate claims with a right click
    private Material configClaimsModificationTool;	  				// which material will be used to create/resize claims with a right click
    private ArrayList<String> configPVPBlockedCommands;			// list of commands which may not be used during pvp combat
    private MaterialCollection configModsAccessTrustIds;			// list of block IDs which should require /accesstrust for player interaction
    private MaterialCollection configModsContainerTrustIds;		// list of block IDs which should require /containertrust for player interaction


    public ClaimBehaviourData getTrashBlockPlacementBehaviour() { return trashBlockPlacementBehaviour; }
    public ClaimBehaviourData getCreeperExplosionBehaviour() { return creeperExplosionBehaviour; }
	public ClaimBehaviourData getTntExplosionBehaviour() { return tntExplosionBehaviour; }
	public ClaimBehaviourData getWitherExplosionBehaviour() { return witherExplosionBehaviour; }
	public ClaimBehaviourData getWitherEatBehaviour() { return witherEatBehaviour; }
	public ClaimBehaviourData getOtherExplosionBehaviour() { return otherExplosionBehaviour; }
	public ClaimBehaviourData getWitherSpawnBehaviour() { return witherSpawnBehaviour; }
	public ClaimBehaviourData getIronGolemSpawnBehaviour() { return ironGolemSpawnBehaviour; }
	public ClaimBehaviourData getSnowGolemSpawnBehaviour() { return snowGolemSpawnBehaviour; }
	public ClaimBehaviourData getWaterBucketBehaviour() { return waterBucketBehaviour; }
	public ClaimBehaviourData getLavaBucketBehaviour() { return lavaBucketBehaviour; }
	public ClaimBehaviourData getVillagerTrades() { return villagerTrades; }
	public ClaimBehaviourData getEnvironmentalVehicleDamage() { return environmentalVehicleDamage; }
	public ClaimBehaviourData getZombieDoorBreaking() { return zombieDoorBreaking; }
	public ClaimBehaviourData getShearingRules() { return sheepShearingRules; }
	public ClaimBehaviourData getSheepDyeingRules() { return sheepDyeing; }
	public ClaimBehaviourData getBonemealGrassRules() { return bonemealGrass; }
	public ClaimBehaviourData getPlayerTrampleRules() { return playerTrampleRules; }

	/**
	 * returns whether Claims are enabled. Most configuration Options, while still present and readable, become redundant when this is false.
	 * @return
	 */
	public boolean getClaimsEnabled() { return claimsEnabled; }

	/**
	 * returns the List of Trash block materials for this world. These are Materials that can be
	 * -placed in survival
	 * @return
	 */
	public List<Material> getTrashBlocks() { return configTrashBlocks; }
    public double getClaimsAbandonReturnRatio() { return configClaimsAbandonReturnRatio; }
	public int getMessageCooldownClaims() { return configMessageCooldownClaims; }
	public int getMessageCooldownStuck() { return configMessageCooldownStuck; }
	public int getClaimCleanupMaximumSize() { return configClaimCleanupMaximumSize; }
	public int getClaimCleanupMaxInvestmentScore() { return configClaimCleanupMaxInvestmentScore; }
	public boolean getEntityCleanupEnabled() { return configEntityCleanupEnabled; }
	public boolean getClaimCleanupEnabled() { return configClaimCleanupEnabled; }
	public boolean getTreecleanupEnabled() { return configClaimCleanupEnabled; }
    public boolean getClaimsPreventTheft() { return configClaimsPreventTheft; }
	public boolean getClaimsProtectCreatures() { return configClaimsProtectCreatures; }
    public boolean getClaimsPreventButtonsSwitches() { return configClaimsPreventButtonsSwitches; }
	public boolean getClaimsLockWoodenDoors() { return configClaimsLockWoodenDoors; }
    public boolean getClaimsLockTrapDoors() { return configClaimsLockTrapDoors; }
	public boolean getEnderPearlsRequireAccessTrust() { return configClaimsEnderPearlsRequireAccessTrust; }
    public boolean getClaimsLockFenceGates() { return configClaimsLockFenceGates; }
	public float getClaimBlocksAccruedPerHour() { return configClaimsBlocksAccruedPerHour; }
	public int getClaimsMaxDepth() { return configClaimsMaxDepth; }
	public int getClaimsExpirationDays() { return configClaimsExpirationDays; }
	public int getAutomaticClaimsForNewPlayerRadius() { return configClaimsAutomaticClaimsForNewPlayersRadius; }
	public boolean getCreateClaimRequiresPermission() { return configClaimsCreationRequiresPermission; }
	public int getClaimsExtendIntoGroundDistance() { return configClaimsClaimsExtendIntoGroundDistance; }
	public int getMinClaimSize() { return configClaimsMinSize; }
	public boolean getCreativeRules() { return configClaimsCreativeRules; }
    public boolean getAutoRestoreUnclaimed() { return configClaimsAutoRestoreUnclaimed; }
	public boolean getAllowUnclaim() { return configClaimsAllowUnclaim; }
    public boolean getApplyTrashBlockRules() { return configClaimsApplyTrashBlockRules; }
	public int getChestClaimExpirationDays() { return configClaimsChestClaimExpirationDays; }
	public int getUnusedClaimExpirationDays() { return configClaimsUnusedClaimExpirationDays; }
	public boolean getClaimsAutoNatureRestoration() { return configClaimsAutoNatureRestoration; }
	public boolean getClaimsAbandonNatureRestoration() { return configClaimsAbandonNatureRestoration; }
	public int getClaimsTrappedCooldownHours() { return configClaimsTrappedCooldownHours; }
	public Material getClaimsInvestigationTool() { return configClaimsInvestigationTool; }
	public Material getClaimsModificationTool() { return configClaimsModificationTool; }
    public boolean getProtectFreshSpawns() { return configPVPProtectFreshSpawns; }
    public boolean getPvPPunishLogout() { return configPVPPunishLogout; }
	public int getPvPCombatTimeoutSeconds() { return configPVPCombatTimeoutSeconds; }
	public boolean getPvPBlockContainers() { return configPVPBlockContainers; }
	public boolean getAllowCombatItemDrop() { return configPVPAllowCombatItemDrop; }
	public List<String> getPvPBlockedCommands() { return configPVPBlockedCommands; }
	public boolean getPvPNoCombatInPlayerClaims() { return configPVPNoCombatInPlayerLandClaims; }
	public boolean getNoPvPCombatInAdminClaims() { return configPVPNoCombatInAdminLandClaims; }
	public boolean getRemoveFloatingTreetops() { return configTreesTemoveFloatingTreetops; }
    public boolean getRegrowGriefedTrees() { return configTreesRegrowGriefedTrees; }
	public boolean getBlockSkyTrees() { return configBlockSkyTrees; }
    public boolean getFireSpreads() { return configFireSpreads; }
	public boolean getFireDestroys() { return configFireDestroys; }
	public boolean getAddItemsToClaimedChests() { return configAddItemsToClaimedChests; }
	public boolean getSignEavesdrop() { return configSignEavesdrop; }
	public boolean getEavesDrop() { return configEavesdrop; }
	public boolean getSmartBan() { return configSmartBan; }
	public boolean endermenMoveBlocks() { return configEndermenMoveBlocks; }
	public boolean getSilverfishBreakBlocks() { return configSilverfishBreakBlocks; }
	public boolean creaturesTrampleCrops() { return configCreaturesTrampleCrops; }
    public MaterialCollection getModsAccessTrustIds() { return configModsAccessTrustIds; }
	public MaterialCollection getModsContainerTrustIds() { return configModsContainerTrustIds; }
	public List<String> getModsIgnoreClaimsAccounts() { return configModsIgnoreClaimsAccounts; }
    public MaterialCollection getModsExplodableIds() { return configModsExplodableIds; }
    public boolean claims_warnOnBuildOutside() { return configClaimsWarnOnBuildOutside; }

	public Integer getSeaLevelOverride() {
		if(configSeaLevelOverride == -1) {
    		return (configSeaLevelOverride =Bukkit.getWorld(this.getWorldName()).getSeaLevel());
        } else {
			return configSeaLevelOverride;
		}
    }

	public int getClaimsWildernessBlocksDelay() { return configClaimsWildernessBlocksDelay; }
    public int getClaimsPerPlayerLimit() { return configClaimsPerPlayerClaimLimit; }
	public String getWorldName() { return WorldName; }

	// constructor accepts a Name and a FileConfiguration.
	public WorldConfig(String pName,FileConfiguration config,FileConfiguration outConfig) {

		// determine defaults based on the world itself (isCreative, isPvP)
		boolean isCreative=false,isPvP=false;
		WorldName = pName;
		World getworld = Bukkit.getWorld(pName);
		if(getworld!=null) {
			isCreative = Bukkit.getServer().getDefaultGameMode()==GameMode.CREATIVE;
			isPvP = getworld.getPVP();
		}

		this.configSeaLevelOverride = config.getInt("GriefPrevention.SeaLevelOverride",-1);

		outConfig.set("GriefPrevention.SeaLevelOverride", configSeaLevelOverride);
		// read in the data for TNT explosions and Golem/Wither placements.

		this.creeperExplosionBehaviour = new ClaimBehaviourData("Creeper Explosions",config,outConfig,"GriefPrevention.CreeperExplosions",
				ClaimBehaviourData.getOutsideClaims("Creeper Explosions"));

		this.witherExplosionBehaviour = new ClaimBehaviourData("Wither Explosions",config,outConfig,"GriefPrevention.WitherExplosions",
				ClaimBehaviourData.getOutsideClaims("Wither Explosions"));

		this.witherEatBehaviour = new ClaimBehaviourData("Wither Eating",config,outConfig,"GriefPrevention.WitherEating",
				ClaimBehaviourData.getOutsideClaims("Wither Eating"));

		this.tntExplosionBehaviour = new ClaimBehaviourData("TNT Explosions",config,outConfig,"GriefPrevention.TNTExplosions",
				ClaimBehaviourData.getOutsideClaims("TNTExplosions"));

		this.otherExplosionBehaviour = new ClaimBehaviourData("Other Explosions",config,outConfig,"GriefPrevention.OtherExplosions",
				ClaimBehaviourData.getOutsideClaims("Other Explosions"));

		this.waterBucketBehaviour = new ClaimBehaviourData("Water Placement",config,outConfig,"GriefPrevention.WaterBuckets",
		        ClaimBehaviourData.getAboveSeaLevel("Water Placement"));

		this.lavaBucketBehaviour = new ClaimBehaviourData("Lava Placement",config,outConfig,"GriefPrevention.LavaBuckets",
				ClaimBehaviourData.getAboveSeaLevel("Lava Placement"));

		// golem spawn rules.
		this.ironGolemSpawnBehaviour = new ClaimBehaviourData("Iron Golem Spawning",config,outConfig,"GriefPrevention.BuildIronGolem",
				ClaimBehaviourData.getInsideClaims("Iron Golem Spawning"));

		this.snowGolemSpawnBehaviour = new ClaimBehaviourData("Snow Golem Spawning",config,outConfig,"GriefPrevention.BuildSnowGolem",
				ClaimBehaviourData.getInsideClaims("Snow Golem Spawning"));


		this.witherSpawnBehaviour = new ClaimBehaviourData("Wither Spawning",config,outConfig,"GriefPrevention.BuildWither",
				ClaimBehaviourData.getInsideClaims("Wither Spawning"));

		trashBlockPlacementBehaviour = new ClaimBehaviourData("Trash Block Placement",config,outConfig,"GriefPrevention.TrashBlockPlacementRules",
				ClaimBehaviourData.getOutsideClaims("Trash Block Placement"));

		villagerTrades = new ClaimBehaviourData("Villager Trading",config,outConfig,"GriefPrevention.Claims.VillagerTrading",
				ClaimBehaviourData.getInsideClaims("Villager Trading"));

		this.environmentalVehicleDamage = new ClaimBehaviourData("Environmental Vehicle Damage",config,outConfig,"GriefPrevention.Claims.EnvironmentalVehicleDamage",
				ClaimBehaviourData.getOutsideClaims("Environmental Vehicle Damage"));


		this.zombieDoorBreaking = new ClaimBehaviourData("Zombie Door Breaking",config,outConfig,"GriefPrevention.ZombieDoorBreaking",
				ClaimBehaviourData.getNone("Zombie Door Breaking"));

		sheepShearingRules = new ClaimBehaviourData("Sheep Shearing",config,outConfig,"GriefPrevention.SheepShearing",
				ClaimBehaviourData.getInsideClaims("Sheep Shearing"));

		sheepDyeing = new ClaimBehaviourData("Sheep Dyeing",config,outConfig,"GriefPrevention.SheepDyeing",
				ClaimBehaviourData.getInsideClaims("Sheep Dyeing"));

		this.bonemealGrass = new ClaimBehaviourData("Bonemeal",config,outConfig,"GriefPrevention.BonemealGrass",
				ClaimBehaviourData.getInsideClaims("Bonemeal"));

		this.playerTrampleRules = new ClaimBehaviourData("Crop Trampling",config,outConfig,"GriefPrevention.PlayerCropTrample",
				ClaimBehaviourData.getInsideClaims("Crop Trampling"));

		// read trash blocks.
		// Cobblestone,Torch,Dirt,Sapling,Gravel,Sand,TNT,Workbench
		this.configTrashBlocks = new ArrayList<Material>();
		for(Material trashblock:new Material[]{Material.COBBLESTONE, Material.TORCH, Material.DIRT, Material.SAPLING,
                Material.GRAVEL, Material.SAND, Material.TNT, Material.WORKBENCH}) {
		    this.configTrashBlocks.add(trashblock);
		}
		List<String> trashBlocks= config.getStringList("GriefPrevention.Claims.TrashBlocks");
		if(trashBlocks == null || trashBlocks.size() == 0) {
		// go with the default, which we already set.
			trashBlocks = new ArrayList<String>();
			for(String iterate:new String[] {"COBBLESTONE", "TORCH", "DIRT", "SAPLING", "GRAVEL", "SAND", "TNT", "WORKBENCH"}) {
				trashBlocks.add(iterate);
			}
		} else {
			// reset...
			this.configTrashBlocks = new ArrayList<Material>();
			for(String trashmaterial: trashBlocks) {
				 try {
                     // replace spaces with underscores...
                     trashmaterial = trashmaterial.replace(" ", "_");
                     Material parsed = Material.valueOf(trashmaterial.toUpperCase());
                     configTrashBlocks.add(parsed);
				 } catch (IllegalArgumentException iae) {
					 // nothing special, log though.
					 GriefPrevention.addLogEntry("failed to parse trashmaterial Entry:" + trashmaterial.toUpperCase());
				 }
			}
		}

        this.claimsEnabled = config.getBoolean("GriefPrevention.Claims.Enabled",true);
		outConfig.set("GriefPrevention.Claims.Enabled", claimsEnabled);

        this.configEntityCleanupEnabled = config.getBoolean("GriefPrevention.CleanupTasks.Claims",true);
		outConfig.set("GriefPrevention.CleanupTasks.Entity", this.configEntityCleanupEnabled);

        this.configClaimCleanupEnabled = config.getBoolean("GriefPrevention.ClaimCleanup.Enabled",true);
        outConfig.set("GriefPrevention.ClaimCleanup.Enabled",this.configClaimCleanupEnabled);

		this.configClaimCleanupMaximumSize = config.getInt("GriefPrevention.ClaimCleanup.MaximumSize",25);
        outConfig.set("GriefPrevention.ClaimCleanup.MaximumSize", configClaimCleanupMaximumSize);

		// max investment score, defaults to 400 for creative worlds.
		this.configClaimCleanupMaxInvestmentScore =	config.getInt("GriefPrevention.ClaimCleanup.MaxInvestmentScore",isCreative?400:100);
        outConfig.set("GriefPrevention.ClaimCleanup.MaxInvestmentScore", this.configClaimCleanupMaxInvestmentScore);

        configClaimsBlocksAccruedPerHour = config.getInt("GriefPrevention.Claims.BlocksAccruedPerHour",100);
		outConfig.set("GriefPrevention.Claims.BlocksAccruedPerHour", configClaimsBlocksAccruedPerHour);

		this.configMessageCooldownClaims = config.getInt("GriefPrevention.Expiration.MessageCooldown.Claim",0);
		outConfig.set("GriefPrevention.Expiration.MessageCooldown.Claim", configMessageCooldownClaims);

        this.configMessageCooldownStuck = config.getInt("GriefPrevention.Expiration.MessageCooldown.Stuck",0);
		outConfig.set("GriefPrevention.Expiration.MessageCooldown.Stuck", configMessageCooldownStuck);

		this.configClaimsWildernessBlocksDelay = config.getInt("GriefPrevention.Claims.WildernessWarningBlockCount",15); // number of blocks,0 will disable the wilderness warning.
        outConfig.set("GriefPrevention.Claims.WildernessWarningBlockCount", this.configClaimsWildernessBlocksDelay);

        this.configClaimsCreativeRules = config.getBoolean("GriefPrevention.CreativeRules",Bukkit.getServer().getDefaultGameMode()==GameMode.CREATIVE);
        outConfig.set("GriefPrevention.CreativeRules", configClaimsCreativeRules);

		this.configClaimsAbandonReturnRatio = config.getDouble("GriefPrevention.Claims.AbandonReturnRatio",1);
		outConfig.set("GriefPrevention.Claims.AbandonReturnRatio", this.configClaimsAbandonReturnRatio);

		this.configSignEavesdrop = config.getBoolean("GriefPrevention.SignEavesDrop",true);
		outConfig.set("GriefPrevention.SignEavesDrop", this.configSignEavesdrop);

		this.configClaimsPerPlayerClaimLimit = config.getInt("GriefPrevention.Claims.PerPlayerLimit",0);
		outConfig.set("GriefPrevention.Claims.PerPlayerLimit", configClaimsPerPlayerClaimLimit);

		this.configClaimsPreventTheft = config.getBoolean("GriefPrevention.Claims.PreventTheft", true);
        outConfig.set("GriefPrevention.Claims.PreventTheft", this.configClaimsPreventTheft);

		this.configClaimsProtectCreatures = config.getBoolean("GriefPrevention.Claims.ProtectCreatures", true);
        outConfig.set("GriefPrevention.Claims.ProtectCreatures", this.configClaimsProtectCreatures);

		this.configClaimsPreventButtonsSwitches = config.getBoolean("GriefPrevention.Claims.PreventButtonsSwitches", true);
        outConfig.set("GriefPrevention.Claims.PreventButtonsSwitches", this.configClaimsPreventButtonsSwitches);

		this.configClaimsLockWoodenDoors = config.getBoolean("GriefPrevention.Claims.LockWoodenDoors", false);
        outConfig.set("GriefPrevention.Claims.LockWoodenDoors", this.configClaimsLockWoodenDoors);

		this.configClaimsLockTrapDoors = config.getBoolean("GriefPrevention.Claims.LockTrapDoors", false);
        outConfig.set("GriefPrevention.Claims.LockTrapDoors", this.configClaimsLockTrapDoors);

        this.configClaimsLockFenceGates = config.getBoolean("GriefPrevention.Claims.LockFenceGates", true);
        outConfig.set("GriefPrevention.Claims.LockFenceGates", this.configClaimsLockFenceGates);

        this.configClaimsEnderPearlsRequireAccessTrust = config.getBoolean("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", true);
        outConfig.set("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", this.configClaimsEnderPearlsRequireAccessTrust);

		this.configClaimsAutomaticClaimsForNewPlayersRadius = config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", 4);
        outConfig.set("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", this.configClaimsAutomaticClaimsForNewPlayersRadius);

		this.configClaimsClaimsExtendIntoGroundDistance = config.getInt("GriefPrevention.Claims.ExtendIntoGroundDistance", 5);
        outConfig.set("GriefPrevention.Claims.ExtendIntoGroundDistance", this.configClaimsClaimsExtendIntoGroundDistance);

		this.configClaimsCreationRequiresPermission = config.getBoolean("GriefPrevention.Claims.CreationRequiresPermission", false);
        outConfig.set("GriefPrevention.Claims.CreationRequiresPermission", this.configClaimsCreationRequiresPermission);

		this.configClaimsMinSize = config.getInt("GriefPrevention.Claims.MinimumSize", 10);
        outConfig.set("GriefPrevention.Claims.MinimumSize", this.configClaimsMinSize);

		this.configClaimsMaxDepth = config.getInt("GriefPrevention.Claims.MaximumDepth", 0);
        outConfig.set("GriefPrevention.Claims.MaximumDepth", this.configClaimsMaxDepth);

		this.configClaimsTrappedCooldownHours = config.getInt("GriefPrevention.Claims.TrappedCommandCooldownHours", 8);
        outConfig.set("GriefPrevention.Claims.TrappedCommandCooldownHours", this.configClaimsTrappedCooldownHours);

		this.configClaimsApplyTrashBlockRules = config.getBoolean("GriefPrevention.Claims.NoSurvivalBuildingOutsideClaims", false);
        outConfig.set("GriefPrevention.Claims.NoSurvivalBuildingOutsideClaims", this.configClaimsApplyTrashBlockRules);

        this.configClaimsWarnOnBuildOutside = config.getBoolean("GriefPrevention.Claims.WarnWhenBuildingOutsideClaims", true);
        outConfig.set("GriefPrevention.Claims.WarnWhenBuildingOutsideClaims", this.configClaimsWarnOnBuildOutside);

		this.configClaimsAllowUnclaim = config.getBoolean("GriefPrevention.Claims.AllowUnclaimingLand", true);
        outConfig.set("GriefPrevention.Claims.AllowUnclaimingLand", this.configClaimsAllowUnclaim);

		this.configClaimsAutoRestoreUnclaimed = config.getBoolean("GriefPrevention.Claims.AutoRestoreUnclaimedLand", true);
        outConfig.set("GriefPrevention.Claims.AutoRestoreUnclaimedLand", this.configClaimsAutoRestoreUnclaimed);

		this.configClaimsAbandonNatureRestoration = config.getBoolean("GriefPrevention.Claims.AbandonAutoRestore",false);
		outConfig.set("GriefPrevention.Claims.AbandonAutoRestore",this.configClaimsAbandonNatureRestoration);

		this.configClaimsChestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
		outConfig.set("GriefPrevention.Claims.Expiration.ChestClaimDays", this.configClaimsChestClaimExpirationDays);

		this.configClaimsUnusedClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.UnusedClaimDays", 14);
		outConfig.set("GriefPrevention.Claims.Expiration.UnusedClaimDays", this.configClaimsUnusedClaimExpirationDays);

		this.configClaimsExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaimDays", 0);
		outConfig.set("GriefPrevention.Claims.Expiration.AllClaimDays", this.configClaimsExpirationDays);

		this.configClaimsAutoNatureRestoration = config.getBoolean("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration", false);
		outConfig.set("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration", this.configClaimsAutoNatureRestoration);

		this.configPVPProtectFreshSpawns = config.getBoolean("GriefPrevention.PvP.ProtectFreshSpawns", true);
        outConfig.set("GriefPrevention.PvP.ProtectFreshSpawns", this.configPVPProtectFreshSpawns);

		this.configPVPPunishLogout = config.getBoolean("GriefPrevention.PvP.PunishLogout", true);
        outConfig.set("GriefPrevention.PvP.PunishLogout", this.configPVPPunishLogout);

		this.configPVPCombatTimeoutSeconds = config.getInt("GriefPrevention.PvP.CombatTimeoutSeconds", 15);
        outConfig.set("GriefPrevention.PvP.CombatTimeoutSeconds", this.configPVPCombatTimeoutSeconds);

		this.configPVPAllowCombatItemDrop = config.getBoolean("GriefPrevention.PvP.AllowCombatItemDrop", false);
        outConfig.set("GriefPrevention.PvP.AllowCombatItemDrop", this.configPVPAllowCombatItemDrop);

        this.configTreesRegrowGriefedTrees = config.getBoolean("GriefPrevention.Trees.RegrowGriefedTrees", true);
        outConfig.set("GriefPrevention.Trees.RegrowGriefedTrees", this.configTreesRegrowGriefedTrees);

        this.configPVPBlockContainers = config.getBoolean("GriefPrevention.PvP.BlockContainers",true);
        outConfig.set("GriefPrevention.PvP.BlockContainers", configPVPBlockContainers);

        String bannedPvPCommandsList = config.getString("GriefPrevention.PvP.BlockedSlashCommands", "/home;/vanish;/spawn;/tpa");
        outConfig.set("GriefPrevention.PvP.BlockedSlashCommands", bannedPvPCommandsList);

		this.configTreesTemoveFloatingTreetops = config.getBoolean("GriefPrevention.Trees.RemoveFloatingTreetops", true);
        outConfig.set("GriefPrevention.Trees.RemoveFloatingTreetops", this.configTreesTemoveFloatingTreetops);

		this.configBlockSkyTrees = config.getBoolean("GriefPrevention.LimitSkyTrees", true);
        outConfig.set("GriefPrevention.LimitSkyTrees", this.configBlockSkyTrees);

        this.configFireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        outConfig.set("GriefPrevention.FireSpreads", this.configFireSpreads);

		this.configFireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);
        outConfig.set("GriefPrevention.FireDestroys", this.configFireDestroys);

		this.configAddItemsToClaimedChests = config.getBoolean("GriefPrevention.AddItemsToClaimedChests", true);
        outConfig.set("GriefPrevention.AddItemsToClaimedChests", this.configAddItemsToClaimedChests);

        this.configEavesdrop = config.getBoolean("GriefPrevention.EavesdropEnabled", false);
        outConfig.set("GriefPrevention.EavesdropEnabled", this.configEavesdrop);

        String whisperCommandsToMonitor = config.getString("GriefPrevention.WhisperCommands", "/tell;/pm;/r");
        outConfig.set("GriefPrevention.WhisperCommands", whisperCommandsToMonitor);

		this.configSmartBan = config.getBoolean("GriefPrevention.SmartBan", true);
        outConfig.set("GriefPrevention.SmartBan", this.configSmartBan);

		this.configEndermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        outConfig.set("GriefPrevention.EndermenMoveBlocks", this.configEndermenMoveBlocks);

        this.configSilverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        outConfig.set("GriefPrevention.SilverfishBreakBlocks", this.configSilverfishBreakBlocks);

        this.configCreaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        outConfig.set("GriefPrevention.CreaturesTrampleCrops", this.configCreaturesTrampleCrops);

        this.configModsIgnoreClaimsAccounts = config.getStringList("GriefPrevention.Mods.PlayersIgnoringAllClaims");

		if(this.configModsIgnoreClaimsAccounts == null) this.configModsIgnoreClaimsAccounts = new ArrayList<String>();
        outConfig.set("GriefPrevention.Mods.PlayersIgnoringAllClaims", this.configModsIgnoreClaimsAccounts);

		this.configModsAccessTrustIds = new MaterialCollection();
		List<String> accessTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringAccessTrust");
		// default values for access trust mod blocks
		if(accessTrustStrings == null || accessTrustStrings.size() == 0)
		{
			// none by default
		}
		SerializationUtil.parseMaterialListFromConfig(accessTrustStrings, this.configModsAccessTrustIds);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringAccessTrust", this.configModsAccessTrustIds);

		this.configModsContainerTrustIds = new MaterialCollection();
		List<String> containerTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringContainerTrust");
		// default values for container trust mod blocks
		if(containerTrustStrings == null || containerTrustStrings.size() == 0)
		{
			containerTrustStrings.add(new MaterialInfo(227, "Battery Box").toString());
			containerTrustStrings.add(new MaterialInfo(130, "Transmutation Tablet").toString());
			containerTrustStrings.add(new MaterialInfo(128, "Alchemical Chest and Energy Condenser").toString());
			containerTrustStrings.add(new MaterialInfo(181, "Various Chests").toString());
			containerTrustStrings.add(new MaterialInfo(178, "Ender Chest").toString());
			containerTrustStrings.add(new MaterialInfo(150, "Various BuildCraft Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(155, "Filler").toString());
			containerTrustStrings.add(new MaterialInfo(157, "Builder").toString());
			containerTrustStrings.add(new MaterialInfo(158, "Template Drawing Table").toString());
			containerTrustStrings.add(new MaterialInfo(126, "Various EE Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(138, "Various RedPower Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(137, "BuildCraft Project Table and Furnaces").toString());
			containerTrustStrings.add(new MaterialInfo(250, "Various IC2 Machines").toString());
			containerTrustStrings.add(new MaterialInfo(161, "BuildCraft Engines").toString());
			containerTrustStrings.add(new MaterialInfo(169, "Automatic Crafting Table").toString());
			containerTrustStrings.add(new MaterialInfo(177, "Wireless Components").toString());
			containerTrustStrings.add(new MaterialInfo(183, "Solar Arrays").toString());
			containerTrustStrings.add(new MaterialInfo(187, "Charging Benches").toString());
			containerTrustStrings.add(new MaterialInfo(188, "More IC2 Machines").toString());
			containerTrustStrings.add(new MaterialInfo(190, "Generators, Fabricators, Strainers").toString());
			containerTrustStrings.add(new MaterialInfo(194, "More Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(207, "Computer").toString());
			containerTrustStrings.add(new MaterialInfo(208, "Computer Peripherals").toString());
			containerTrustStrings.add(new MaterialInfo(246, "IC2 Generators").toString());
			containerTrustStrings.add(new MaterialInfo(24303, "Teleport Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(24304, "Waterproof Teleport Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(24305, "Power Teleport Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(4311, "Diamond Sorting Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(216, "Turtle").toString());

		}

		// parse the strings from the config file
		SerializationUtil.parseMaterialListFromConfig(containerTrustStrings, this.configModsContainerTrustIds);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringContainerTrust", this.configModsContainerTrustIds);

        this.configModsExplodableIds = new MaterialCollection();
		List<String> explodableStrings = config.getStringList("GriefPrevention.Mods.BlockIdsExplodable");

		// default values for explodable mod blocks
		if(explodableStrings == null || explodableStrings.size() == 0)
		{
			explodableStrings.add(new MaterialInfo(161, "BuildCraft Engines").toString());
			explodableStrings.add(new MaterialInfo(246, (byte)5 ,"Nuclear Reactor").toString());
		}

		// parse the strings from the config file
		SerializationUtil.parseMaterialListFromConfig(explodableStrings, this.configModsExplodableIds);

		// default for claim investigation tool
		String investigationToolMaterialName = Material.STICK.name();

		// get investigation tool from config
		investigationToolMaterialName = config.getString("GriefPrevention.Claims.InvestigationTool", investigationToolMaterialName);

		// validate investigation tool
		this.configClaimsInvestigationTool = Material.getMaterial(investigationToolMaterialName);
		if(this.configClaimsInvestigationTool == null)
		{
			GriefPrevention.addLogEntry("ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
			this.configClaimsInvestigationTool = Material.STICK;
		}

		// default for claim creation/modification tool
		String modificationToolMaterialName = Material.GOLD_SPADE.name();

		// get modification tool from config
		modificationToolMaterialName = config.getString("GriefPrevention.Claims.ModificationTool", modificationToolMaterialName);

		// validate modification tool
		this.configClaimsModificationTool = Material.getMaterial(modificationToolMaterialName);
		if(this.configClaimsModificationTool == null)
		{
			GriefPrevention.addLogEntry("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the golden shovel.  Please update your config.yml.");
			this.configClaimsModificationTool = Material.GOLD_SPADE;
		}

		this.configPVPNoCombatInPlayerLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", false);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", this.configPVPNoCombatInPlayerLandClaims);

		this.configPVPNoCombatInAdminLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", true);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", this.configPVPNoCombatInAdminLandClaims);
        outConfig.set("GriefPrevention.Mods.BlockIdsExplodable", this.configModsExplodableIds);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringAccessTrust", accessTrustStrings);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringContainerTrust", containerTrustStrings);
        outConfig.set("GriefPrevention.Mods.BlockIdsExplodable", explodableStrings);
        outConfig.set("GriefPrevention.Claims.InvestigationTool", this.configClaimsInvestigationTool.name());
        outConfig.set("GriefPrevention.Claims.ModificationTool", this.configClaimsModificationTool.name());
        outConfig.set("GriefPrevention.Claims.WarnWhenBuildingOutsideClaims", this.configClaimsWarnOnBuildOutside);
        outConfig.set("GriefPrevention.Claims.AllowUnclaimingLand", this.configClaimsAllowUnclaim);
        outConfig.set("GriefPrevention.Claims.TrashBlocks",trashBlocks);
        // Task startup.
        // if we have a blockaccrued value and the ClaimTask for delivering claim blocks is null,
        // create and schedule it to run.
        if (configClaimsBlocksAccruedPerHour > 0 && GriefPrevention.instance.claimTask == null) {
            GriefPrevention.instance.claimTask = new DeliverClaimBlocksTask();
            GriefPrevention.instance.getServer().getScheduler().scheduleSyncRepeatingTask(GriefPrevention.instance,
                         GriefPrevention.instance.claimTask, 60L*20*2, 60L*20*5);
        }

        //similar logic for ClaimCleanup: if claim cleanup is enabled and there isn't a cleanup task, start it.
        if (this.getClaimCleanupEnabled() && GriefPrevention.instance.cleanupTask==null) {
            CleanupUnusedClaimsTask task2 = new CleanupUnusedClaimsTask();
            GriefPrevention.instance.getServer().getScheduler().scheduleSyncRepeatingTask(GriefPrevention.instance, task2, 20L * 60 * 2, 20L * 60 * 5);
        }
	}

	public WorldConfig(String worldname) {
		this(worldname,new YamlConfiguration(),ConfigData.createTargetConfiguration(worldname) );
	}
	public WorldConfig(World grabfor) {
		// // construct WorldConfig with default settings.
		// we construct a default FileConfiguration and call ourselves...
		this(grabfor.getName());
	}
}
