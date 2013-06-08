package me.ryanhamshire.GriefPrevention.Configuration;

import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * represents the placement rules for a particular Claim Behaviour 'packet'. This is designed to allow for unneeded
 * flexibility later, or something.
 *
 * @author BC_Programming
 */
public class PlacementRules {

// above and below placement rules.
    public enum BasicPermissionConstants {
        Deny,
        Allow;

        public boolean allowed() {
            return this == Allow;
        }

        public boolean denied() {
            return !allowed();
        }

        public static BasicPermissionConstants fromBoolean(boolean value) {
            return value ? Allow : Deny;
        }

        public static BasicPermissionConstants fromString(String source) {
            if (source.equalsIgnoreCase(Boolean.TRUE.toString())) return Allow;
            if (source.equalsIgnoreCase(Boolean.FALSE.toString())) return Deny;
            for (BasicPermissionConstants iterate : values()) {
                if (iterate.name().equalsIgnoreCase(source.trim())) {
                    return iterate;
                }
            }
            return null;
        }
    }

    private BasicPermissionConstants AboveSeaLevel;
    private BasicPermissionConstants BelowSeaLevel;

    /**
     * returns whether this placement rule allows Action above sea level.
     *
     * @return
     */
    public BasicPermissionConstants getAboveSeaLevel() {
        return AboveSeaLevel;
    }

    /**
     * returns whether this placement rule allows Action below sea level.
     *
     * @return
     */
    public BasicPermissionConstants getBelowSeaLevel() {
        return BelowSeaLevel;
    }

    public static PlacementRules AboveOnly = new PlacementRules(true, false);
    public static PlacementRules BelowOnly = new PlacementRules(false, true);
    public static PlacementRules Both = new PlacementRules(true, true);
    public static PlacementRules Neither = new PlacementRules(false, false);

    @Override
    public String toString() {
        if (AboveSeaLevel.allowed() && BelowSeaLevel.denied()) return "Only Above Sea Level";
        if (AboveSeaLevel.denied() && BelowSeaLevel.allowed()) return "Only Below Sea Level";
        if (AboveSeaLevel.allowed() && BelowSeaLevel.allowed()) return "Anywhere";
        return "Nowhere";
    }

    public PlacementRules(boolean above, boolean below) {
        AboveSeaLevel = BasicPermissionConstants.fromBoolean(above);
        BelowSeaLevel = BasicPermissionConstants.fromBoolean(below);
    }

    public PlacementRules(BasicPermissionConstants above, BasicPermissionConstants below) {
        AboveSeaLevel = above;
        BelowSeaLevel = below;
    }

    /**
     * constructs a new PlacementRules based on the settings in the given configuration file at the given Node, using specific defaults and
     * a target Configuration to save the elements too.
     *
     * @param source   Source Configuration.
     * @param target   Target Configuration to save to.
     * @param nodePath Path to the Configuration Node to read from.
     * @param defaults instance containing Default settings to default to.
     */
    public PlacementRules(FileConfiguration source, FileConfiguration target, String nodePath, PlacementRules defaults) {

        String sAboveSeaLevel = source.getString(nodePath + ".AboveSeaLevel", defaults.AboveSeaLevel.name());
        String sBelowSeaLevel = source.getString(nodePath + ".BelowSeaLevel", defaults.BelowSeaLevel.name());
        AboveSeaLevel = BasicPermissionConstants.fromString(sAboveSeaLevel);
        BelowSeaLevel = BasicPermissionConstants.fromString(sBelowSeaLevel);
        if (AboveSeaLevel == null) AboveSeaLevel = defaults.AboveSeaLevel;
        if (BelowSeaLevel == null) BelowSeaLevel = defaults.BelowSeaLevel;
        target.set(nodePath + ".AboveSeaLevel", AboveSeaLevel.name());
        target.set(nodePath + ".BelowSeaLevel", BelowSeaLevel.name());
    }

    /**
     * determines if this Placementrule allows for the given location.
     *
     * @param target
     * @return
     */
    public boolean allow(Location target) {
        int seaLevelofWorld = GriefPrevention.instance.getSeaLevel(target.getWorld());
        boolean result = (AboveSeaLevel.allowed() && (target.getBlockY() >= seaLevelofWorld)) ||
                (BelowSeaLevel.allowed() && (target.getBlockY() < seaLevelofWorld));
        return result;
    }
}
