/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention.configuration;

import org.bukkit.Material;

import java.util.regex.Pattern;

//represents a material or collection of materials
public class MaterialInfo {
    int typeId;
    byte data;
    boolean allDataValues;
    String description;
    private Pattern re;

    public int getTypeId() {
        return typeId;
    }

    public byte getData() {
        return data;
    }

    public boolean getallDataValues() {
        return allDataValues;
    }

    public String getDescription() {
        return description;
    }

    public MaterialInfo(int typeId, byte data, String description) {
        this.typeId = typeId;
        this.data = data;
        this.allDataValues = false;
        this.description = description;
    }

    public MaterialInfo(int typeId, String description) {
        this.typeId = typeId;
        this.data = 0;
        this.allDataValues = true;
        if (description == null || description.length() == 0) {
            description = Material.getMaterial(typeId).name();
        }
        this.description = description;
    }

    private MaterialInfo(int typeId, byte data, boolean allDataValues, String description) {
        this.typeId = typeId;
        this.data = data;
        this.allDataValues = allDataValues;
        if (description.startsWith("//")) {
            re = Pattern.compile(description.substring(1));
        }
        this.description = description;
    }

    @Override
    public String toString() {
        String returnValue = String.valueOf(this.typeId) + ":" + (this.allDataValues ? "*" : String.valueOf(this.data));
        if (this.description != null) returnValue += ":" + this.description;
        return returnValue;
    }

    @Override
    public int hashCode() {
        return (typeId * data) / (typeId + data) ^ data;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MaterialInfo) {
            MaterialInfo castedelement = (MaterialInfo) other;
            if (this.typeId == castedelement.typeId &&
                    ((this.allDataValues || castedelement.allDataValues) ||
                            this.data == castedelement.data)) {
                if (re != null) {
                    return re.matcher(castedelement.getDescription()).matches();
                }
                return true;
            }
        }
        return super.equals(other);
    }

    public static MaterialInfo fromString(String string) {
        if (string == null || string.isEmpty()) return null;
        String[] parts = string.split(":");
        if (parts.length < 3) return null;
        try {
            int typeID = Integer.parseInt(parts[0]);

            byte data;
            boolean allDataValues;
            if (parts[1].equals("*")) {
                return new MaterialInfo(typeID, parts[2]);
            } else {
                data = Byte.parseByte(parts[1]);
                return new MaterialInfo(typeID, data, parts[2]);
            }
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}