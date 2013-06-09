package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.data.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class Trust extends BaseTrustCommand {
    public Trust(GriefPrevention plugin) {
        super(plugin, "trust", ClaimPermission.Build);
    }
}
