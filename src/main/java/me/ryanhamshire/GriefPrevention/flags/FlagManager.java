package me.ryanhamshire.GriefPrevention.flags;

import me.ryanhamshire.GriefPrevention.GriefPrevention;

import java.util.Collection;
import java.util.LinkedHashMap;

public class FlagManager {

    private GriefPrevention plugin;
    private LinkedHashMap<String, BaseFlag> registeredClaimFlags = new LinkedHashMap<String, BaseFlag>();

    public FlagManager(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    public boolean registerFlag(BaseFlag flag) {
        if (registeredClaimFlags.containsKey(flag.getKey().toLowerCase())) {
            return false;
        }
        registeredClaimFlags.put(flag.getKey().toLowerCase(), flag);
        return true;
    }

    public boolean unregisterFlag(BaseFlag flag) {
        if (registeredClaimFlags.containsKey(flag.getKey().toLowerCase())) {
            return false;
        }
        registeredClaimFlags.remove(flag.getKey().toLowerCase());
        return true;
    }

    public boolean isFlagRegistered(BaseFlag flag) {
        return registeredClaimFlags.containsKey(flag.getKey().toLowerCase());
    }

    public BaseFlag getFlag(String key) {
        return registeredClaimFlags.get(key.toLowerCase());
    }

    public Collection<BaseFlag> getAllFlags() {
        return registeredClaimFlags.values();
    }
}
