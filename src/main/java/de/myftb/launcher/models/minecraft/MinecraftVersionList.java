/*
 * MyFTBLauncher
 * Copyright (C) 2023 MyFTB <https://myftb.de>
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftVersionList {
    private List<MinecraftVersion> versions;

    public Optional<MinecraftVersion> getVersion(String id) {
        return this.versions.stream()
                .filter(version -> version.getId().equals(id))
                .findFirst();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MinecraftVersion {
        private String id;
        private String url;

        public String getId() {
            return this.id;
        }

        public String getUrl() {
            return this.url;
        }
    }

}