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

package com.norcode.bukkit.griefprevention.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//ordered list of material info objects, for fast searching
public class MaterialCollection {
    HashMap<Integer, List<MaterialInfo>> materials = new HashMap<Integer, List<MaterialInfo>>();

    public void add(MaterialInfo material) {
        if (!materials.containsKey(material.getTypeId())) {
            materials.put(material.getTypeId(), new ArrayList<MaterialInfo>());
        }
        this.materials.get(material.typeId).add(material);

    }

    public boolean contains(MaterialInfo material) {
        if (this.materials.containsKey(material.typeId)) {
            return this.materials.get(material.typeId).contains(material);
        }


        return false;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (List<MaterialInfo> matInfoList : materials.values()) {
            for (MaterialInfo materialInfo : matInfoList) {
                stringBuilder.append(materialInfo.toString());
                stringBuilder.append(" ");
            }
        }
        return stringBuilder.toString().trim();
    }

    public int size() {
        return this.materials.size();
    }

    public void clear() {
        this.materials.clear();
    }
}