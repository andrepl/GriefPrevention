package me.ryanhamshire.GriefPrevention.configuration;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * represents the placement rules for a particular Claim Behaviour 'packet'. This is designed to allow for unneeded
 * flexibility later, or something.
 *
 * @author BC_Programming
 */
public class PlacementRules {

    public static final PlacementRules ABOVE_ONLY = new PlacementRules(true, false);
    public static final PlacementRules BELOW_ONLY = new PlacementRules(false, true);
    public static final PlacementRules BOTH = new PlacementRules(true, true);
    public static final PlacementRules NEITHER = new PlacementRules(false, false);

    // above and below placement rules.
    public enum BasicPermissionConstants {
        DENY,
        ALLOW;

        public boolean allowed() {
            return this == ALLOW;
        }

        public boolean denied() {
            return !allowed();
        }

        public static BasicPermissionConstants fromBoolean(boolean value) {
            return value ? ALLOW : DENY;
        }

        public static BasicPermissionConstants fromString(String source) {
            if (source.equalsIgnoreCase(Boolean.TRUE.toString())) return ALLOW;
            if (source.equalsIgnoreCase(Boolean.FALSE.toString())) return DENY;
            for (BasicPermissionConstants iterate : values()) {
                if (iterate.name().equalsIgnoreCase(source.trim())) {
                    return iterate;
                }
            }
            return null;
        }
    }

    private BasicPermissionConstants aboveSeaLevel;
    private BasicPermissionConstants belowSeaLevel;

    /**
     * returns whether this placement rule allows Action above sea level.
     *
     * @return
     */
    public BasicPermissionConstants getAboveSeaLevel() {
        return aboveSeaLevel;
    }

    /**
     * returns whether this placement rule allows Action below sea level.
     *
     * @return
     */
    public BasicPermissionConstants getBelowSeaLevel() {
        return belowSeaLevel;
    }

    @Override
    public String toString() {
        if (aboveSeaLevel.allowed() && belowSeaLevel.denied()) return "Only Above Sea Level";
        if (aboveSeaLevel.denied() && belowSeaLevel.allowed()) return "Only Below Sea Level";
        if (aboveSeaLevel.allowed() && belowSeaLevel.allowed()) return "Anywhere";
        return "Nowhere";
    }

    public PlacementRules(PlacementRules copySource) {
        this.aboveSeaLevel = copySource.aboveSeaLevel;
        this.belowSeaLevel = copySource.belowSeaLevel;
    }

    public PlacementRules(boolean above, boolean below) {
        aboveSeaLevel = BasicPermissionConstants.fromBoolean(above);
        belowSeaLevel = BasicPermissionConstants.fromBoolean(below);
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

        String sAboveSeaLevel = source.getString(nodePath + ".AboveSeaLevel", defaults.aboveSeaLevel.name());
        String sBelowSeaLevel = source.getString(nodePath + ".BelowSeaLevel", defaults.belowSeaLevel.name());
        aboveSeaLevel = BasicPermissionConstants.fromString(sAboveSeaLevel);
        belowSeaLevel = BasicPermissionConstants.fromString(sBelowSeaLevel);
        if (aboveSeaLevel == null) aboveSeaLevel = defaults.aboveSeaLevel;
        if (belowSeaLevel == null) belowSeaLevel = defaults.belowSeaLevel;
        target.set(nodePath + ".AboveSeaLevel", aboveSeaLevel.name());
        target.set(nodePath + ".BelowSeaLevel", belowSeaLevel.name());
    }

    /**
     * determines if this PlacementRule allows for the given location.
     *
     * @param target
     * @return
     */
    public boolean allow(GriefPrevention plugin, Location target, Player player, boolean showMessages) {
        // TODO player and showMessages args were added upstream but do nothing. investigate
        int seaLevelofWorld = plugin.getWorldCfg(target.getWorld()).getSeaLevelOverride();
        boolean result = (aboveSeaLevel.allowed() && (target.getBlockY() >= seaLevelofWorld)) ||
                (belowSeaLevel.allowed() && (target.getBlockY() < seaLevelofWorld));
        return result;
    }
}
