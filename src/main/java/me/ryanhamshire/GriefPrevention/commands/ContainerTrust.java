package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.configuration.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class ContainerTrust extends BaseTrustCommand {
    public ContainerTrust(GriefPrevention plugin) {
        super(plugin, "containertrust", ClaimPermission.INVENTORY);
    }
}
