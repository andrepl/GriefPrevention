package me.ryanhamshire.GriefPrevention.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import me.ryanhamshire.GriefPrevention.PlayerGroups;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

/**
 * holds configuration data global to GP as well as 
 * providing accessors to retrieve/create configurations for individual worlds.
 * @author BC_Programming
 *
 */
public class ConfigData {

    private String templateFile;
	private HashMap<String, WorldConfig> WorldCfg = new HashMap<String, WorldConfig>();
    private HashMap<TextMode, ChatColor> colors = new HashMap<TextMode, ChatColor>();

    private PlayerGroups playerGroups;
    private double claimBlocksPurchaseCost = 0.0;
    private double claimBlocksSellValue = 0.0;
    private int maxAccruedBlocks = 5000;
    private int initialBlocks = 100;

    private String databaseUrl;
    private String databaseUserName;
    private String databasePassword;

    /**
     * Constructs a new ConfigData instance from the given core configuration location
     * and the passed in target out configuration.
     * @param coreConfig Configuration (config.yml) source file that contains core configuration information.
     * @param outConfig Target file to save back to.
     */
    public ConfigData(FileConfiguration coreConfig, FileConfiguration outConfig) {
        // core configuration is configuration that is Global.

        this.playerGroups = new PlayerGroups(coreConfig, "GriefPrevention.Groups");
        this.playerGroups.save(outConfig, "GriefPrevention.Groups");

        // ChatColor's should be configurable too
        colors.put(TextMode.ERROR, ChatColor.valueOf(coreConfig.getString("GriefPrevention.ChatColors.Error", "RED")));
        outConfig.set("GriefPrevention.ChatColors.Error", colors.get(TextMode.ERROR).name());
        colors.put(TextMode.INFO, ChatColor.valueOf(coreConfig.getString("GriefPrevention.ChatColors.Info", "AQUA")));
        outConfig.set("GriefPrevention.ChatColors.Info", colors.get(TextMode.INFO).name());
        colors.put(TextMode.INSTR, ChatColor.valueOf(coreConfig.getString("GriefPrevention.ChatColors.Instruction", "YELLOW")));
        outConfig.set("GriefPrevention.ChatColors.Instruction", colors.get(TextMode.INSTR).name());
        colors.put(TextMode.SUCCESS, ChatColor.valueOf(coreConfig.getString("GriefPrevention.ChatColors.Success", "GREEN")));
        outConfig.set("GriefPrevention.ChatColors.Success", colors.get(TextMode.SUCCESS).name());
        colors.put(TextMode.WARN, ChatColor.valueOf(coreConfig.getString("GriefPrevention.ChatColors.Warning", "GOLD")));
        outConfig.set("GriefPrevention.ChatColors.Warning", colors.get(TextMode.WARN).name());

        // economy / block accrual
        this.maxAccruedBlocks = coreConfig.getInt("GriefPrevention.Claims.MaxAccruedBlocks", 5000);
        outConfig.set("GriefPrevention.Claims.MaxAccruedBlocks", maxAccruedBlocks);
        this.claimBlocksPurchaseCost = coreConfig.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", this.claimBlocksPurchaseCost);
        this.claimBlocksSellValue = coreConfig.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksSellValue", this.claimBlocksSellValue);
        this.initialBlocks = coreConfig.getInt("GriefPrevention.Claims.InitialBlocks", 100);
        outConfig.set("GriefPrevention.Claims.InitialBlocks", initialBlocks);

        // Database Settings
        databaseUrl = coreConfig.getString("GriefPrevention.Database.URL", "");
        outConfig.set("GriefPrevention.Database.URL", databaseUrl);
        databaseUserName = coreConfig.getString("GriefPrevention.Database.UserName", "");
        outConfig.set("GriefPrevention.Database.UserName", databaseUserName);
        databasePassword = coreConfig.getString("GriefPrevention.Database.Password", "");
        outConfig.set("GriefPrevention.Database.Password", databasePassword);

        // we try to avoid these now. Normally the primary interest is the
        // GriefPrevention.WorldConfigFolder setting.
        String defaultConfigFolder = DataStore.dataLayerFolderPath + File.separator + "WorldConfigs" + File.separator;
        String defaultTemplateFile = defaultConfigFolder + "_template.cfg";
        // Configurable template file.
        templateFile = coreConfig.getString("GriefPrevention.WorldConfig.TemplateFile", defaultTemplateFile);
        if (!(new File(templateFile).exists())) {
            templateFile = defaultTemplateFile;
        }
        outConfig.set("GriefPrevention.WorldConfig.TemplateFile", templateFile);

        // check for appropriate configuration in given FileConfiguration. Note we also save out this configuration information.
        // configurable World Configuration folder.
        // save the configuration.
        String configFolder = coreConfig.getString("GriefPrevention.WorldConfigFolder");

        if (configFolder == null || configFolder.length() == 0) {
            configFolder = defaultConfigFolder;
        }

        File configLocation = new File(configFolder);
        if (!configLocation.exists()) {
            // if not found, create the directory.
            GriefPrevention.instance.getLogger().log(Level.INFO, "mkdirs() on " + configLocation.getAbsolutePath());
            configLocation.mkdirs();
        }

        if (configLocation.exists() && configLocation.isDirectory()) {
            for(File lookfile: configLocation.listFiles()) {
                if(lookfile.isFile()) {
                    String extension = lookfile.getName().substring(lookfile.getName().indexOf('.')+1);
                    String baseName = extension.length()==0?
                            lookfile.getName():
                            lookfile.getName().substring(0,lookfile.getName().length()-extension.length()-1);
                    if(baseName.startsWith("_")) continue; // configs starting with underscore are templates. Normally just _template.cfg.
                    // if baseName is an existing world...
                    // read it in...
                    GriefPrevention.addLogEntry(lookfile.getAbsolutePath());
                    FileConfiguration source = YamlConfiguration.loadConfiguration(new File(lookfile.getAbsolutePath()));
                    FileConfiguration target = new YamlConfiguration();
                    // load in the WorldConfig...
                    WorldConfig wc = new WorldConfig(baseName, source, target);
                    try {
                        target.save(lookfile);
                    } catch (IOException iex) {
                        GriefPrevention.instance.getLogger().log(Level.SEVERE, "Failed to save to " + lookfile.getAbsolutePath());
                    }
                }
            }
        } else if (configLocation.exists() && configLocation.isFile()) {
            GriefPrevention.instance.getLogger().log(Level.SEVERE, "World Configuration Folder found, but it's a File. Double-check your GriefPrevention configuration files, and try again.");
        }
    }

    public Map<String, WorldConfig> getWorldConfigs() {
		return Collections.unmodifiableMap(WorldCfg);
	}

    public String getTemplateFile() {
        return templateFile;
    }

    public List<WorldConfig> getCreativeRulesConfigs() {
		ArrayList<WorldConfig> buildList = new ArrayList<WorldConfig>();
		for(WorldConfig wcon:WorldCfg.values()) {
			if(wcon.getCreativeRules()) {
                buildList.add(wcon);
            }
		}
		return buildList;
	}
    
	public WorldConfig getWorldConfig(World forWorld) {
		return getWorldConfig(forWorld.getName());
	}
    
	/**
	 * retrieves the WorldConfiguration for the given world name.
	 * If the world is not valid, a log entry will be posted, but the config should still be loaded and returned.
	 * @param worldName Name of world to get configuration of.
	 * @return WorldConfig instance representing the configuration for the given world.
	 */
	public WorldConfig getWorldConfig(String worldName) {
		World grabfor =null;
		// print log message if the passed world is not currently loaded or present.
		// it will still go ahead and try to load the configuration.
		if((grabfor = Bukkit.getWorld(worldName))==null) {
			GriefPrevention.instance.getLogger().log(Level.SEVERE, "invalid World:" + worldName);
		}
		
		// if it's not in the hashmap...
		if(!WorldCfg.containsKey(worldName)) {
			// special code: it's possible a configuration might already exist for this file, so we'll
			// check 
			String checkyamlfile = DataStore.dataLayerFolderPath + File.separator + "WorldConfigs/" + worldName + ".cfg";
			// if it exists...
			if (new File(checkyamlfile).exists()) {
				// attempt to load the configuration from the given file.
				YamlConfiguration existingcfg = YamlConfiguration.loadConfiguration(new File(checkyamlfile));
				YamlConfiguration outConfiguration = new YamlConfiguration();
				// place it in the hashmap.
				WorldCfg.put(worldName,new WorldConfig(worldName,existingcfg,outConfiguration));
				// try to save it. this can error out for who knows what reason. If it does, we'll
				// squirt the issue to the log.
				try {
                    outConfiguration.save(new File(checkyamlfile));
                } catch (IOException iex) {
					GriefPrevention.instance.getLogger().log(Level.SEVERE,"Failed to save World Config for world " + worldName);
				}
			} else {
				// if the file doesn't exist, then we will go ahead and create a new configuration.
				// set the input Yaml to default to the template.
				// if the template file exists, load it's configuration and use the result as useSource. 
				// Otherwise, we create a blank configuration.
				FileConfiguration useSource = (new File(templateFile).exists()?YamlConfiguration.loadConfiguration(new File(templateFile)):new YamlConfiguration());
				// The target save location.
				FileConfiguration Target = new YamlConfiguration();
				// place it in the hashmap.
				WorldCfg.put(worldName, new WorldConfig(grabfor.getName(),useSource,Target));
				try {
					Target.save(new File(checkyamlfile));
				}catch(IOException ioex) {
					GriefPrevention.instance.getLogger().log(Level.SEVERE, "Failed to write world configuration to " + checkyamlfile);
				}		
            }
			// save target
        }
		// after the above logic, we know it's in the hashmap, so return that.
		return WorldCfg.get(worldName);
	}
    
	public static FileConfiguration createTargetConfiguration(String sName) {
		return YamlConfiguration.loadConfiguration(new File(sName));
	}
    
	/**
	 * returns the Configuration file location for the given world. Note that this file may or may not exist.
	 * @param sName Name of the world.
	 * @return the path name at which this configuration file will be found if it exists.
	 */
	public static String getWorldConfigLocation(String sName) {
		return DataStore.dataLayerFolderPath + File.separator + "WorldConfigs/" + sName + ".cfg";
	}

    public ChatColor getColor(TextMode color) {
        if (colors.containsKey(color)) {
            return colors.get(color);
        }
        return ChatColor.WHITE;
    }

    public PlayerGroups getPlayerGroups() {
        return playerGroups;
    }

    public double getClaimBlocksSellValue() {
        return claimBlocksSellValue;
    }

    public double getClaimBlocksPurchaseCost() {
        return claimBlocksPurchaseCost;
    }

    public int getMaxAccruedBlocks() {
        return maxAccruedBlocks;
    }

    public int getInitialBlocks() {
        return initialBlocks;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public String getDatabaseUserName() {
        return databaseUserName;
    }
}