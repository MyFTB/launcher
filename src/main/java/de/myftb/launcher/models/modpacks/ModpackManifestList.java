/*
 * MyFTBLauncher
 * Copyright (C) 2019 MyFTB <https://myftb.de>
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

package de.myftb.launcher.models.modpacks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModpackManifestList {
    private List<ModpackManifestReference> packages;

    public void setPackages(List<ModpackManifestReference> packages) {
        this.packages = packages;
    }

    public List<ModpackManifestReference> getPackages() {
        return this.packages;
    }

    public Optional<ModpackManifestReference> getPackByName(String name) {
        return this.getPackages().stream()
                .filter(reference -> reference.getName().equals(name))
                .findFirst();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModpackManifestReference {
        private String name;
        private String title;
        private String version;
        private String location;

        public String getName() {
            return this.name;
        }

        public String getTitle() {
            return this.title;
        }

        public String getVersion() {
            return this.version;
        }

        public String getLocation() {
            return this.location;
        }
    }

}
