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

package de.myftb.launcher.integration;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.myftb.launcher.Launcher;
import de.myftb.launcher.launch.LaunchHelper;
import de.myftb.launcher.models.modpacks.ModpackManifestList;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModpackWebstart extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(ModpackWebstart.class);
    private static final Gson gson = new Gson();
    private static final int portRangeStart = 52820;
    private static final int portRangeLength = 10;

    private static final byte ERR_NO_TYPE = 1;
    private static final byte ERR_PACK_NOT_FOUND = 2;

    public ModpackWebstart() {
        super(new InetSocketAddress("127.0.0.1",
                ModpackWebstart.getFreePortInRange().orElseThrow(() -> new RuntimeException("Keinen freien Port gefunden"))));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        try {
            String origin = handshake.getFieldValue("Origin");
            if (origin == null || !new URI(origin).getHost().endsWith("myftb.de")) {
                conn.close();
                return;
            }
        } catch (URISyntaxException e) {
            conn.close();
            return;
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject jsonObject = ModpackWebstart.gson.fromJson(message, JsonElement.class).getAsJsonObject();
            if (!jsonObject.has("type")) {
                conn.send(ModpackWebstart.getError(ModpackWebstart.ERR_NO_TYPE).toString());
                return;
            }

            String type = jsonObject.get("type").getAsString();
            if ("ping".equals(type)) {
                JsonObject response = new JsonObject();
                response.addProperty("type", "pong");
                response.addProperty("version", Launcher.getVersion());
                conn.send(response.toString());
            } else if ("launch".equals(type)) {
                if (!jsonObject.has("pack")) {
                    conn.send(ModpackWebstart.getError(ModpackWebstart.ERR_PACK_NOT_FOUND).toString());
                    return;
                }

                Optional<ModpackManifestList.ModpackManifestReference> pack = Launcher.getInstance().getRemotePacks()
                        .getPackByName(jsonObject.get("pack").getAsString());

                if (!pack.isPresent()) {
                    conn.send(ModpackWebstart.getError(ModpackWebstart.ERR_PACK_NOT_FOUND).toString());
                    return;
                }

                Launcher.getInstance().bringToFront();
                Launcher.getInstance().getIpcHandler().send("launch_pack", pack.get().getWebstart());
            }
        } catch (Exception e) {
            ModpackWebstart.log.warn("Fehler in Websocket-Handler", e);
        }
    }

    private static JsonObject getError(byte errorType) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "error");
        jsonObject.addProperty("code", errorType);
        return jsonObject;
    }

    private static boolean isPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Optional<Integer> getFreePortInRange() {
        for (int i = 0; i < ModpackWebstart.portRangeLength; i++) {
            if (ModpackWebstart.isPortFree(ModpackWebstart.portRangeStart + i)) {
                return Optional.of(ModpackWebstart.portRangeStart + i);
            }
        }

        return Optional.empty();
    }

}
