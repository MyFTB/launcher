/*
 * MyFTBLauncher
 * Copyright (C) 2022 MyFTB <https://myftb.de>
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

package de.myftb.launcher.login.microsoft.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftProfile {
    private String id;
    private String name;

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public UUID getUuid() {
        return UUID.fromString(this.id.substring(0, 8) + "-" + this.id.substring(8, 12) + "-" + this.id.substring(12, 16) + "-"
                + this.id.substring(16, 20) + "-" + this.id.substring(20, 32));
    }
}
