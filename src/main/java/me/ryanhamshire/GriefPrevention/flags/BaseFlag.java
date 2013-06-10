package me.ryanhamshire.GriefPrevention.flags;

import me.ryanhamshire.GriefPrevention.configuration.ClaimBehaviourData;
import me.ryanhamshire.GriefPrevention.configuration.ClaimBehaviourData.ClaimBehaviourMode;
import me.ryanhamshire.GriefPrevention.data.Claim;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public abstract class BaseFlag {

    protected final String key;
    protected String displayName;
    protected String description;
    protected String defaultValue;
    protected List<String> validOptions = new ArrayList<String>();
    protected String requiredPermission = null;

    protected BaseFlag(String key) {
        this.key = key;
    }

    public abstract String getRequiredPermission();
    public abstract void onSet(Player player, Claim claim, String value);

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<String> getValidOptions() {
        return validOptions;
    }

    public void setValidOptions(List<String> validOptions) {
        this.validOptions = validOptions;
    }

    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
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
