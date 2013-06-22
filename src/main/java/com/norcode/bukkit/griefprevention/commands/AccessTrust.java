package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.configuration.ClaimPermission;

public class AccessTrust extends BaseTrustCommand {
    public AccessTrust(GriefPreventionTNG plugin) {
        super(plugin, "accesstrust", ClaimPermission.ACCESS);
    }
}
