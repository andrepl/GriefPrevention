package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.configuration.ClaimPermission;

public class ContainerTrust extends BaseTrustCommand {
    public ContainerTrust(GriefPreventionTNG plugin) {
        super(plugin, "containertrust", ClaimPermission.INVENTORY);
    }
}
