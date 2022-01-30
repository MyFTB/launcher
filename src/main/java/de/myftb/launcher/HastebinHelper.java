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
package de.myftb.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

public class HastebinHelper {

    public static String post(byte[] content) throws IOException {
        HttpResponse response = HttpRequest.post(Constants.pasteTarget + "/documents")
                .bodyByteArray(content)
                .execute()
                .returnResponse();

        JsonObject jsonObject = new Gson().fromJson(new InputStreamReader(response.getEntity().getContent()), JsonElement.class).getAsJsonObject();
        if (!jsonObject.has("key")) {
            throw new IOException(jsonObject.toString());
        }

        return Constants.pasteTarget + "/" + jsonObject.get("key").getAsString();
    }

}
