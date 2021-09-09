/*
 * MyFTBLauncher
 * Copyright (C) 2021 MyFTB <https://myftb.de>
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

package de.myftb.launcher.models.launcher;

import com.google.gson.annotations.Expose;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class LauncherProfile {
    @Expose private String provider;
    @Expose private UUID uuid;
    @Expose private String lastKnownUsername;
    @Expose private Map<String, Object> properties;

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getLastKnownUsername() {
        return this.lastKnownUsername;
    }

    public void setLastKnownUsername(String lastKnownUsername) {
        this.lastKnownUsername = lastKnownUsername;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        LauncherProfile that = (LauncherProfile) o;
        return this.provider.equals(that.provider) && this.uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.provider, this.uuid);
    }

}
