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

package de.myftb.launcher.login.microsoft.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class XboxAuthentication {
    @JsonProperty("IssueInstant")
    private String issueInstant;
    @JsonProperty("NotAfter")
    private String notAfter;
    @JsonProperty("Token")
    private String token;
    @JsonProperty("DisplayClaims")
    private Map<String, List<XblDisplayClaim>> displayClaims;

    public String getIssueInstant() {
        return this.issueInstant;
    }

    public String getNotAfter() {
        return this.notAfter;
    }

    public String getToken() {
        return this.token;
    }

    public Map<String, List<XblDisplayClaim>> getDisplayClaims() {
        return this.displayClaims;
    }

    public static class XblDisplayClaim {
        @JsonProperty("uhs")
        private String userHash;

        public String getUserHash() {
            return this.userHash;
        }
    }
}
