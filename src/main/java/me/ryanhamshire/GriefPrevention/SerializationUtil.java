package me.ryanhamshire.GriefPrevention;

import me.ryanhamshire.GriefPrevention.configuration.MaterialCollection;
import me.ryanhamshire.GriefPrevention.configuration.MaterialInfo;
import me.ryanhamshire.GriefPrevention.exceptions.WorldNotFoundException;
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
                // show error in log
                GriefPrevention.addLogEntry("ERROR: Unable to read a material entry from the config file.  Please update your config.yml.");
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

        String worldName = elements[0];
        String xString = elements[1];
        String yString = elements[2];
        String zString = elements[3];

        // identify world the claim is in
        World world = Bukkit.getServer().getWorld(worldName);
        if (world == null) {
            // try to load it...
            world = Bukkit.createWorld(new WorldCreator(worldName));
            if (world == null) { // well... we tried!
                throw new WorldNotFoundException("World not found: \"" + worldName + "\"");
            }
        }

        // convert those numerical strings to integer values
        int x = Integer.parseInt(xString);
        int y = Integer.parseInt(yString);
        int z = Integer.parseInt(zString);

        return new Location(world, x, y, z);
    }
}
