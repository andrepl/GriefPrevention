package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class ContainerTrust extends BaseTrustCommand {
    public ContainerTrust(GriefPrevention plugin) {
        super(plugin, "containertrust", ClaimPermission.Inventory);
    }
}
