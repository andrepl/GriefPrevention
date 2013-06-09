package me.ryanhamshire.GriefPrevention;

import me.ryanhamshire.GriefPrevention.exceptions.WorldNotFoundException;
import org.apache.commons.lang.SerializationException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

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
        World world = GriefPrevention.instance.getServer().getWorld(worldName);
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
