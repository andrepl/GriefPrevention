package me.ryanhamshire.GriefPrevention.Configuration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

/**
 * When it comes to handling extra information on a per-claim basis,
 * the current method appears to rest on simply leaving it to the plugins.
 * My intention here is to provide a useful framework that makes saving and loading
 * extra data by dependent plugins on a per-claim basis much easier.
 * The envisions idea:
 * GriefPrevention.instance.getClaimMeta(Claim, plugin) would retrieve the ClaimMeta 
 * for the given claim, of the given plugin.
 * 
 * @author BC_Programming
 */
public class ClaimMetaHandler {
	// handles claim metadata setting and retrieval.
	// Claim meta is retrieved and in the form of a FileConfiguration.
	// Metadata is found in dataLayerFolderPath + "/ClaimMeta/PluginName/ClaimID-SubID.yml"
	
	// outer hashmap indexes by the Plugin Name; the value of that is another HashMap that indexes by a string consisting
	// if the Claim, a hyphen, and a subclaim id. if this is the top-level claim we are dealing with, there will
	// be no hyphen.
	private HashMap<String, HashMap<String, FileConfiguration>> metaData = new HashMap<String, HashMap<String, FileConfiguration>>();
	private String metaFolder ="";

	/**
	 * constructs default MetaHandler instance.
	 */
	public ClaimMetaHandler() {
		this(GriefPrevention.instance.dataStore.dataLayerFolderPath + "/ClaimMeta/");
	}

	/**
	 * constructs a MetaHandler for the given source path.
	 * @param sourcePath
	 */
	public ClaimMetaHandler(String sourcePath) {
		metaFolder = sourcePath;
	}

	public String getClaimTag(Claim c) {
		if (c.parent!=null) {
			return getClaimTag(c);
        } else { 
		    return String.valueOf(c.getID());
        }
	}
    
	public Claim getClaimFromTag(String tag) {
		String[] splitVal = tag.split("-");
		if (splitVal.length == 1) {
			return GriefPrevention.instance.dataStore.getClaim(Long.parseLong(splitVal[0]));
        } else {
			long parentID = Long.parseLong(splitVal[0]);
			long subID = Long.parseLong(splitVal[1]);
			Claim parentClaim = GriefPrevention.instance.dataStore.getClaim(parentID);
			if (parentClaim == null) {
                return null;
            }
			return parentClaim.getSubClaim(subID);
		}
	}

	/**
	 * retrieves the metadata for the given claim for the given Plugin Key.
	 * @param pluginKey Unique key for your Plugin. The Plugin Name is usually sufficient.
	 * @param c Claim to get meta for.
	 * @return a FileConfiguration of metadata for that Claim. This will be empty if it is not set. When you make the needed changes,
	 * pass the resulting changed FileConfiguration to setClaimMeta() to save it back.
	 */
	public FileConfiguration getClaimMeta(String pluginKey, Claim c) {
		String useClaimKey = null;
		if (c.parent == null) useClaimKey = String.valueOf(c.getID());
		if (c.parent != null) useClaimKey = String.valueOf(getClaimTag(c));
		return getClaimMeta(pluginKey, useClaimKey);
	}

	/**
	 * retrieves a list of All Meta Keys currently
	 * registered.
	 * @return
	 */
	public List<String> getMetaPluginKeys() {
		String LookFolder = metaFolder + "/";
		//retrieve all Directories in this folder.
		File di = new File(LookFolder);
		if (!di.exists()) {
			return new ArrayList<String>(); //return empty list.
		}
		else {
			ArrayList<String> resultKeys = new ArrayList<String>();
			//directory does exist. Each plugin meta is a folder.
			for(File iterate: di.listFiles()) {
				if (iterate.isDirectory()) {
					String pluginName = iterate.getName();
					//it's a plugin key.
					resultKeys.add(pluginName);
				}
			}
			return resultKeys;
		}
	}
    
	public List<Claim> getClaimsForKey(String pluginKey) {	
		File pluginFolder = new File(metaFolder + "/" + pluginKey + "/");
		if (!pluginFolder.exists()) {
            return new ArrayList<Claim>();
        } else {
			ArrayList<Claim> resultValue = new ArrayList<Claim>();
			//if the folder does exist, read each one. return it as a list.
			if (pluginFolder.isDirectory()) {
				for(File iterateFile: pluginFolder.listFiles()) {
					if (iterateFile.getName().toUpperCase().endsWith(".YML")) {
						String basename = iterateFile.getName().substring(0, iterateFile.getName().lastIndexOf(".")+1);
						Claim gotFromTag = getClaimFromTag(basename);
						if (gotFromTag!=null) {resultValue.add(gotFromTag);}
					}
				}
			}
			return resultValue;
		}
		
	}
    
	//retrieves the name of the appropriate claim file, making sure that the path exists.
	private String getClaimMetaFile(String pluginKey, String claimKey) {
		String pluginFolder = metaFolder + "/" + pluginKey;
		// make sure that directory exists...
		File pFolder = new File(pluginFolder);
		if (!pFolder.exists()) {
			pFolder.mkdirs();
		}
		
		// now add the Claim Key to the path.
		String claimMeta = pluginFolder + "/" + claimKey + ".yml";
		return claimMeta;
	}

	/**
	 * sets the ClaimMeta data of a given claim for the given PluginKey to a given FileConfiguration.
	 * @param pluginKey  Unique Key of your Plugin. The Plugin Name is usually sufficient.
	 * @param c Claim to get meta for.
	 * 
	 */
	public void setClaimMeta(String pluginKey, Claim c, FileConfiguration result) {
		String useClaimKey=null;
		if (c.parent == null) useClaimKey = String.valueOf(c.getID());
		if (c.parent != null) useClaimKey = String.valueOf(getClaimTag(c));
		setClaimMeta(pluginKey, useClaimKey, result);
	}

	public void setClaimMeta(String pluginKey, String claimKey, FileConfiguration result) {
		String claimMeta = getClaimMetaFile(pluginKey, claimKey);
		try {
            result.save(claimMeta);
		} catch (IOException iox) {
		    GriefPrevention.instance.getLogger().log(Level.SEVERE, "Failed to save Claim Meta to file, " + claimMeta);
		    iox.printStackTrace();
		}
	}

	private FileConfiguration getClaimMeta(String pluginKey, String claimKey) {
		// find the Plugin key folder.
		String ClaimMeta = getClaimMetaFile(pluginKey, claimKey);
		// if the file exists...
		if (new File(ClaimMeta).exists()) {
			return YamlConfiguration.loadConfiguration(new File(ClaimMeta));
		} else {
			// return a new configuration.
			return new YamlConfiguration();
		}
	}
}
