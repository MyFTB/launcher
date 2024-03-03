/*
 * MyFTBLauncher
 * Copyright (C) 2024 MyFTB <https://myftb.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.myftb.launcher.models.minecraft;

import de.myftb.launcher.Constants;

import java.util.Map;

public class AssetIndex {
    private Map<String, Asset> objects;
    private boolean virtual;

    public Map<String, Asset> getObjects() {
        return this.objects;
    }

    public boolean isVirtual() {
        return this.virtual;
    }

    public static class Asset {
        private String hash;
        private int size;

        public String getHash() {
            return this.hash;
        }

        public int getSize() {
            return this.size;
        }

        public String getSubPath() {
            return this.hash.substring(0, 2) + "/" + this.hash;
        }

        public String getDownloadUrl() {
            return String.format(Constants.minecraftResources, this.getSubPath());
        }
    }
}