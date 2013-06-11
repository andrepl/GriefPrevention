package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.configuration.ClaimPermission;

public class ContainerTrust extends BaseTrustCommand {
    public ContainerTrust(GriefPrevention plugin) {
        super(plugin, "containertrust", ClaimPermission.INVENTORY);
    }
}
