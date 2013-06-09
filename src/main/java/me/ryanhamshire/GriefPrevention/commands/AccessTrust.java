package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.data.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class AccessTrust extends BaseTrustCommand {
    public AccessTrust(GriefPrevention plugin) {
        super(plugin, "accesstrust", ClaimPermission.ACCESS);
    }
}
