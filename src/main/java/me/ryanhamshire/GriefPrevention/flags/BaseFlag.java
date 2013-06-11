package me.ryanhamshire.GriefPrevention.flags;

import me.ryanhamshire.GriefPrevention.data.Claim;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * A base class to be extended by plugins to provide custom flags.
 */
@SuppressWarnings("unused")
public abstract class BaseFlag {

    protected final String key;
    protected String displayName;
    protected String description;
    protected String defaultValue;
    protected List<String> validOptions = new ArrayList<String>();
    protected String requiredPermission = null;

    /**
     * Construct a BaseFlag instance
     *
     * All properties MUST be set before registering the flag.
     *
     * @param key a unique name for the key and used in the command to set it
     * @param displayName a name used to display the flag
     * @param description a brief description of what this flag does
     * @param defaultValue the default value for this flag.
     * @param requiredPermission a permission node required to set this flag.
     * @param validOptions a list of valid values this flag can be set to.
     */
    protected BaseFlag(String key, String displayName, String description, String defaultValue, String requiredPermission, List<String> validOptions) {
        this(key, displayName, description, defaultValue, requiredPermission);
        this.validOptions = new ArrayList<String>(validOptions);
    }

    /**
     * Construct a BaseFlag instance
     *
     * All properties MUST be set before registering the flag.
     *
     * @param key a unique name for the key and used in the command to set it
     * @param displayName a name used to display the flag
     * @param description a brief description of what this flag does
     * @param defaultValue the default value for this flag.
     * @param requiredPermission a permission node required to set this flag.
     */
    protected BaseFlag(String key, String displayName, String description, String defaultValue, String requiredPermission) {
        this(key, displayName, description, defaultValue);
        this.requiredPermission = requiredPermission;
    }

    /**
     * Construct a BaseFlag instance
     *
     * All properties MUST be set before registering the flag.
     *
     * @param key a unique name for the key and used in the command to set it
     * @param displayName a name used to display the flag
     * @param description a brief description of what this flag does
     * @param defaultValue the default value for this flag.
     */
    protected BaseFlag(String key, String displayName, String description, String defaultValue) {
        this(key, displayName, description);
        this.defaultValue = defaultValue;
    }

    /**
     * Construct a BaseFlag instance
     *
     * All properties MUST be set before registering the flag.
     *
     * @param key a unique name for the key and used in the command to set it
     * @param displayName a name used to display the flag
     * @param description a brief description of what this flag does
     */
    protected BaseFlag(String key, String displayName, String description) {
        this(key, displayName);
        this.description = description;
    }

    /**
     * Construct a BaseFlag instance
     *
     * All properties MUST be set before registering the flag.
     *
     * @param key a unique name for the key and used in the command to set it
     * @param displayName a name used to display the flag
     */
    protected BaseFlag(String key, String displayName) {
        this(key);
        this.displayName = displayName;
    }

    /**
     * Construct a BaseFlag instance
     *
     * Note: All properties MUST be set before registering the flag.
     *
     * @param key a unique name for the key and used in the command to set it
     */
    protected BaseFlag(String key) {
        this.key = key;
    }

    /**
     * get the permission node required to change this flag.
     *
     * @return a permission node (as a string)
     */
    public String getRequiredPermission() {
        return requiredPermission == null ? "griefprevention.claims" : requiredPermission;
    }

    /**
     * get the unique name of this flag
     *
     * @return the unique flag name/key
     */
    public String getKey() {
        return key;
    }

    /**
     * get the display-name of this flag.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * set the display name for this flag
     *
     * @param displayName a name to be used when displaying the value of this flag to the user.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * get a brief description of this flag
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * sets the flag's description
     *
     * @param description a brief description of the flag.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * get the default value of this flag
     *
     * @return the flags default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * set the flag's default value
     *
     * @param defaultValue the default value of this flag.
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * get a List of String's containing all the valid values that can be assigned to this flag
     *
     * @return a list of valid flag values.
     */
    public List<String> getValidOptions() {
        return validOptions;
    }

    /**
     * sets the list of valid values for this flag.
     *
     * @param validOptions a new list of valid values.
     */
    public void setValidOptions(List<String> validOptions) {
        this.validOptions = validOptions;
    }

    /**
     * set the required permission node for changing this flag.
     *
     * @param requiredPermission the permission node required.
     */
    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    /**
     * Called when a player changes this flags value on a claim.
     *
     * @param player the player who executed the command
     * @param claim the claim affected by the flag change
     * @param value the new value of the flag
     */
    public void onSet(Player player, Claim claim, String value) {

    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof BaseFlag)) {
            return false;
        }
        return ((BaseFlag) o).getKey().toLowerCase().equals(this.getKey().toLowerCase());
    }

    @Override
    public int hashCode() {
        return getKey().toLowerCase().hashCode();
    }
}
