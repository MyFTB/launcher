/*
 * MyFTBLauncher
 * Copyright (C) 2020 MyFTB <https://myftb.de>
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

@JsonIgnoreProperties(ignoreUnknown = true)
public class FileTask {
    private String hash;
    private String location;
    private String to;
    private boolean userFile;
    private Condition when;

    public String getHash() {
        return this.hash;
    }

    public String getLocation() {
        return this.location;
    }

    public String getTo() {
        return this.to;
    }

    public boolean isUserFile() {
        return this.userFile;
    }

    public Condition getCondition() {
        return this.when;
    }

}