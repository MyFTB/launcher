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
package de.myftb.launcher.models.modpacks;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Condition {
    @JsonProperty("if")
    private String condition;
    private List<String> features;

    public String getCondition() {
        return this.condition;
    }

    public List<String> getFeatures() {
        return this.features;
    }

    public boolean matches(List<String> selectedFeatures) {
        if (condition.equals("requireAll")) {
            for (String feature : this.features) {
                if (!selectedFeatures.contains(feature)) {
                    return false;
                }
            }

            return true;
        } else if (condition.equals("requireAny")) {
            for (String feature : this.features) {
                if (selectedFeatures.contains(feature)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

}
