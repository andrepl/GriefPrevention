package com.norcode.bukkit.griefprevention.commands;

import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.configuration.ClaimPermission;

public class Trust extends BaseTrustCommand {
    public Trust(GriefPreventionTNG plugin) {
        super(plugin, "trust", ClaimPermission.BUILD);
    }
}
