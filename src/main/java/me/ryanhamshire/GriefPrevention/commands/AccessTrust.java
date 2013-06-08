package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class AccessTrust extends BaseTrustCommand {
    public AccessTrust(GriefPrevention plugin) {
        super(plugin, "accesstrust", ClaimPermission.Access);
    }
}
