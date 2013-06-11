package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.ClaimPermission;

public class Trust extends BaseTrustCommand {
    public Trust(GriefPrevention plugin) {
        super(plugin, "trust", ClaimPermission.BUILD);
    }
}
