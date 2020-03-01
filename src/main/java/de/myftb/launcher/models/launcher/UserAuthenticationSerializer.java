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

package de.myftb.launcher.models.launcher;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;

import java.io.IOException;
import java.util.Map;

public class UserAuthenticationSerializer extends TypeAdapter<UserAuthentication> {
    private final LauncherConfig config;

    public UserAuthenticationSerializer(LauncherConfig config) {
        this.config = config;
    }

    @Override
    public void write(JsonWriter out, UserAuthentication value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        new Gson().toJson(value.saveForStorage(), Map.class, out);
    }

    @Override
    public UserAuthentication read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        Map<String, Object> data = new Gson().fromJson(in, Map.class);
        UserAuthentication authentication = this.config.getAuthenticationService().createUserAuthentication(Agent.MINECRAFT);
        authentication.loadFromStorage(data);
        return authentication;
    }

}
