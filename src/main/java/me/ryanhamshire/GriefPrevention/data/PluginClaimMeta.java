package me.ryanhamshire.GriefPrevention.data;

import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SerializableAs("ClaimMeta")
public class PluginClaimMeta implements ConfigurationSerializable {
    String pluginName = null;
    HashMap<String, Object> data = new HashMap<String, Object>();

    public void set(String key, Object value) {
        this.data.put(key, value);
    }

    public Object get(String key) {
        return this.data.get(key);
    }

    public Integer getInt(String key, Integer defaultValue) {
        if (!data.containsKey(key)) {
            return defaultValue;
        }

        try {
            return (Integer) this.get(key);
        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }

    public String getString(String key, String defaultValue) {
        if (!data.containsKey(key)) {
            return defaultValue;
        }

        try {
            return (String) this.get(key);
        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }

    public Double getDouble(String key, Double defaultValue) {
        if (!data.containsKey(key)) {
            return defaultValue;
        }

        try {
            return (Double) this.get(key);
        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }

    public Float getFloat(String key, Float defaultValue) {
        if (!data.containsKey(key)) {
            return defaultValue;
        }

        try {
            return (Float) this.get(key);
        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }

    public Map<String, Object> getMap(String key, Map<String, Object> defaultValue) {
        if (!data.containsKey(key)) {
            return defaultValue;
        }

        try {
            return (Map<String, Object>) get(key);
        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }

    public List<String> getStringList(String key, List<String> defaultValue) {
        if (!data.containsKey(key)) {
            return defaultValue;
        }

        try {
            return (List<String>) data.get(key);
        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }

    public PluginClaimMeta(Plugin plugin) {
        if (plugin != null) {
            pluginName = plugin.getName();
        }
    }

    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>(this.data);
    }

    public static PluginClaimMeta deserialize(Map<String, Object> data) {
        PluginClaimMeta meta = new PluginClaimMeta(null);
        meta.data = new HashMap<String, Object>(data);
        return meta;
    }
}
