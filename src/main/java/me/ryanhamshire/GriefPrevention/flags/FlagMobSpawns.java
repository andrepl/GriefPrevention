package me.ryanhamshire.GriefPrevention.flags;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.ClaimBehaviourData.ClaimBehaviourMode;
import me.ryanhamshire.GriefPrevention.data.Claim;
import org.bukkit.entity.Player;

import java.util.EnumSet;

public class FlagMobSpawns extends BaseFlag {

    GriefPrevention plugin;

    public FlagMobSpawns(GriefPrevention plugin) {
        super("MOB_SPAWNS");
        this.plugin = plugin;
        this.validOptions.add("Allow");
        this.validOptions.add("Deny");
        this.setDescription("Allow or Deny hostile mob spawns");
        this.setDisplayName("Mob Spawning");
        this.setDefaultValue("Allow");
    }

    @Override
    public String getRequiredPermission() {
        return "griefprevention.togglemobs";
    }

    @Override
    public void onSet(Player player, Claim claim, String value) {
        plugin.getLogger().info("Mob Spawns flag set to " + value);
    }

}
