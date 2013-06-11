package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.ClaimPermission;

public class AccessTrust extends BaseTrustCommand {
    public AccessTrust(GriefPrevention plugin) {
        super(plugin, "accesstrust", ClaimPermission.ACCESS);
    }
}
