package com.norcode.bukkit.griefprevention;

import com.norcode.bukkit.griefprevention.configuration.MaterialCollection;
import com.norcode.bukkit.griefprevention.configuration.MaterialInfo;
import com.norcode.bukkit.griefprevention.exceptions.WorldNotFoundException;
import org.apache.commons.lang.SerializationException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.List;

public class SerializationUtil {

    // turns a location into a string, useful in data storage
    private static String locationStringDelimiter = ";";

    public static String locationToString(Location location) {
        StringBuilder stringBuilder = new StringBuilder(location.getWorld().getName());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockX());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockY());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockZ());
        return stringBuilder.toString();
    }

    public static void parseMaterialListFromConfig(List<String> stringsToParse, MaterialCollection materialCollection) {
        materialCollection.clear();

        // for each string in the list
        for (int i = 0; i < stringsToParse.size(); i++) {
            // try to parse the string value into a material info
            MaterialInfo materialInfo = MaterialInfo.fromString(stringsToParse.get(i));

            // null value returned indicates an error parsing the string from the config file
            if (materialInfo == null) {
                // update string, which will go out to config file to help user find the error entry
                if (!stringsToParse.get(i).contains("can't")) {
                    stringsToParse.set(i, stringsToParse.get(i) + "     <-- can't understand this entry, see BukkitDev documentation");
                }
            }
            // otherwise store the valid entry in config data
            else {
                materialCollection.add(materialInfo);
            }
        }
    }

    // turns a location string back into a location
    public static Location locationFromString(String string) throws SerializationException, WorldNotFoundException {
        // split the input string on the space
        String[] elements = string.split(locationStringDelimiter);

        // expect four elements - world name, X, Y, and Z, respectively
        if (elements.length != 4) {
            throw new SerializationException("Expected four distinct parts to the location string:{" + string + "}");
        }
        // identify world the claim is in
        World world = Bukkit.getServer().getWorld(elements[0]);
        if (world == null) {
            // try to load it...
            world = Bukkit.createWorld(new WorldCreator(elements[0]));
            if (world == null) { // well... we tried!
                throw new WorldNotFoundException("World not found: \"" + elements[0] + "\"");
            }
        }
        return new Location(world, Integer.parseInt(elements[1]), Integer.parseInt(elements[2]), Integer.parseInt(elements[3]));
    }
}
