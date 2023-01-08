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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import de.myftb.launcher.models.launcher.Platform;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Rule {

    private enum Action {
        ALLOW,
        DISALLOW;

        @JsonCreator
        public static Action fromJson(String text) {
            return valueOf(text.toUpperCase());
        }

        @JsonValue
        public String toJson() {
            return name().toLowerCase();
        }
    }


    private Action action;
    private Map<String, String> os;
    private Map<String, Boolean> features;

    public Action getAction() {
        return this.action;
    }

    public Map<String, String> getOs() {
        return this.os;
    }

    public Map<String, Boolean> getFeatures() {
        return this.features;
    }

    public boolean matches() {
        if (this.os != null) {
            if (this.os.containsKey("name") && !Platform.getPlatform().name().equalsIgnoreCase(this.os.get("name"))) {
                return false;
            }
            if (this.os.containsKey("version") && !Pattern.compile(this.os.get("version")).matcher(System.getProperty("os.version")).find()) {
                return false;
            }
            if (this.os.containsKey("arch") && !System.getProperty("os.arch").equalsIgnoreCase(this.os.get("arch"))) {
                return false;
            }
        }
        if (this.features != null) {
            //TODO Implement at some point (if necessary)
            return false;
        }

        return true;
    }

    public static boolean isValid(List<Rule> ruleSet) { //TODO WTH.
        return ruleSet.stream()
                .filter(Rule::matches)
                .anyMatch(rule -> rule.action == Action.ALLOW);
    }

}
