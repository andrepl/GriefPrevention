package com.norcode.bukkit.griefprevention.data;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SerializableAs("ClaimMeta")
@SuppressWarnings("unused")
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

    public Long getLong(String key, Long defaultValue) {
        if (!data.containsKey(key)) {
            return defaultValue;
        }
        try {
            return (Long) this.get(key);
        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        if (!data.containsKey(key)) {
            return defaultValue;
        }
        try {
            return (Boolean) this.get(key);
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
        HashMap<String, Object> clone = new HashMap <String, Object>();
        for (Map.Entry<String, Object> e: this.data.entrySet()) {
            if (e.getValue() instanceof ConfigurationSerializable) {
                clone.put(e.getKey(), ((ConfigurationSerializable) e.getValue()).serialize());
            } else {
                clone.put(e.getKey(), e.getValue());
            }
        }
        return clone;
    }

    public static PluginClaimMeta deserialize(Map<String, Object> data) {
        PluginClaimMeta meta = new PluginClaimMeta(null);
        meta.data = new HashMap<String, Object>(data);
        return meta;
    }
}
