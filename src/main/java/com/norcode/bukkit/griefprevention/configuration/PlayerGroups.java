package com.norcode.bukkit.griefprevention.configuration;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerGroups {

	private HashMap<String,PlayerGroup> playerGroups = new HashMap<String,PlayerGroup>();

	public boolean groupExists(String testName){
		return getGroupByName(testName) !=null;
	}
	/**
	 * retrieves a a specified group, or null of the name does not match any group.
	 * @param groupname group name to look up
	 * @return the PlayerGroup instance matching the given name
	 */
	public PlayerGroup getGroupByName(String groupname){
		String capgroup = groupname.toUpperCase();
		if(!playerGroups.containsKey(capgroup))
			return null;
		return playerGroups.get(capgroup);
	}
/*
  Groups:
  Names:[Donator,HalfOp]:
  - Donator: [Chicken,Waffle]
  - HalfOp:  [Choodles,Smeagle]
 * 
 * 
 */
	/**
	 * Initializes this PlayerGroups collection based on data in the given configuration file at the specified node.
     *
	 * @param Source the FileConfiguration to load from
	 * @param SourceNode the name of the config node.
	 */
	public PlayerGroups(FileConfiguration Source, String SourceNode){
		List<PlayerGroup> checklist = PlayerGroup.getGroups(Source, SourceNode);
		for(PlayerGroup iterate:checklist){
			playerGroups.put(iterate.getGroupName().toUpperCase(), iterate);
		}
	}

	/**
	 * Saves this PlayerGroups list to a FileConfiguration.
     *
	 * @param target the FileConfiguration to write to
	 * @param targetNode the name of the target node in the config
	 */
    public void save(FileConfiguration target, String targetNode) {
		ArrayList<String> groupnames = new ArrayList<String>();
		for(PlayerGroup pg: playerGroups.values()){
			groupnames.add(pg.getGroupName());
		}
		target.set(targetNode + ".Names", groupnames);
		for(PlayerGroup iterate: playerGroups.values()){
			String usenode = targetNode + "." + iterate.getGroupName();
			iterate.Save(target, usenode);
		}
		
		
	}
}
