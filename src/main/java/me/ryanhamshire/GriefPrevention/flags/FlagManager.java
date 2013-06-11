package me.ryanhamshire.GriefPrevention.flags;

import me.ryanhamshire.GriefPrevention.GriefPrevention;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Manages all 3rd party claim 'flags'
 */
public class FlagManager {

    private GriefPrevention plugin;
    private LinkedHashMap<String, BaseFlag> registeredClaimFlags = new LinkedHashMap<String, BaseFlag>();

    public FlagManager(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a custom claim flag
     *
     * A flag registered here will be available to players via the /gp flag
     * command and will have its values automatically persisted.
     * @param flag a subclass of BaseFlag to be registered
     * @return true if the flag was successfully registered, false if a flag by that name is already registered.
     */
    public boolean registerFlag(BaseFlag flag) {
        if (registeredClaimFlags.containsKey(flag.getKey().toLowerCase())) {
            return false;
        }
        registeredClaimFlags.put(flag.getKey().toLowerCase(), flag);
        return true;
    }

    /**
     * Unregister a custom flag
     *
     * Remove a previously registered flag from the system.
     *
     * @param flag the flag to remove.
     * @return false if the flag wasn't previously registered, otherwise true.
     */
    public boolean unregisterFlag(BaseFlag flag) {
        if (!registeredClaimFlags.containsKey(flag.getKey().toLowerCase())) {
            return false;
        }
        registeredClaimFlags.remove(flag.getKey().toLowerCase());
        return true;
    }

    /**
     * Check if a flag with the same name as this one has been registered.
     *
     * @param flag the flag to check for
     * @return true if the flag is already registered, false if it is not.
     */
    public boolean isFlagRegistered(BaseFlag flag) {
        return registeredClaimFlags.containsKey(flag.getKey().toLowerCase());
    }

    /**
     * get a the registered flag with the given name;
     * @param key the name of the flag to get
     * @return the flag if it exists, or null otherwise.
     */
    public BaseFlag getFlag(String key) {
        return registeredClaimFlags.get(key.toLowerCase());
    }

    /**
     * get all registered flags
     *
     * @return a Collection of all registered BaseFlag instances.
     */
    public Collection<BaseFlag> getAllFlags() {
        return registeredClaimFlags.values();
    }
}
