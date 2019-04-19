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

package de.myftb.launcher.cef.schemes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerHeadScheme extends CefResourceHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(PlayerHeadScheme.class);
    private static final Map<String, byte[]> skinCache = new HashMap<>();
    private static final int targetSize = 256;
    private byte[] data;
    private int offset;

    private byte[] getSkin(String uuid) {
        try {
            HttpResponse response = Request.Get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)
                    .connectTimeout(3000)
                    .socketTimeout(3000)
                    .execute()
                    .returnResponse();

            if (response.getStatusLine().getStatusCode() != 200) {
                return new byte[0];
            }

            Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
            JsonObject jsonObject = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonElement.class)
                    .getAsJsonObject();

            JsonArray properties = jsonObject.getAsJsonArray("properties");
            for (int i = 0; i < properties.size(); i++) {
                JsonObject property = properties.get(i).getAsJsonObject();
                if (!"textures".equals(property.get("name").getAsString())) {
                    continue;
                }

                String json = new String(Base64.decodeBase64(property.get("value").getAsString()), Charsets.UTF_8);
                MinecraftTexturesPayload textures = gson.fromJson(json, MinecraftTexturesPayload.class);
                if (textures.getTextures() != null && textures.getTextures().containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    MinecraftProfileTexture skin = textures.getTextures().get(MinecraftProfileTexture.Type.SKIN);
                    URI skinUri = new URI(skin.getUrl());
                    if (skinUri.getHost().endsWith(".mojang.com") || skinUri.getHost().endsWith(".minecraft.net")) {
                        HttpResponse skinResponse = Request.Get(skinUri)
                                .connectTimeout(3000)
                                .socketTimeout(3000)
                                .execute()
                                .returnResponse();

                        if (skinResponse.getStatusLine().getStatusCode() != 200) {
                            return new byte[0];
                        }

                        BufferedImage wholeSkin = ImageIO.read(skinResponse.getEntity().getContent());
                        BufferedImage head = wholeSkin.getSubimage(8, 8, 8, 8);
                        AffineTransform transform = new AffineTransform();
                        transform.scale(PlayerHeadScheme.targetSize / 8.0, PlayerHeadScheme.targetSize / 8.0);
                        AffineTransformOp scaleOperation = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                        BufferedImage scaledHead = new BufferedImage(PlayerHeadScheme.targetSize, PlayerHeadScheme.targetSize,
                                BufferedImage.TYPE_INT_RGB);
                        scaledHead = scaleOperation.filter(head, scaledHead);
                        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                            ImageIO.write(scaledHead, "png", byteArrayOutputStream);
                            return byteArrayOutputStream.toByteArray();
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            PlayerHeadScheme.log.warn("Fehler bei der Skinabfrage", e);
        }

        return new byte[0];
    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String url = request.getURL().substring(13);
        if (url.endsWith("/")) {
            url = url.substring(0, url.indexOf('/'));
        }

        url = url.replace("-", "");

        if (url.length() == 32) {
            this.data = PlayerHeadScheme.skinCache.computeIfAbsent(url, this::getSkin);
        } else {
            this.data = new byte[0];
        }

        callback.Continue();
        return true;
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
        response.setMimeType("image/png");
        response.setStatus(this.data.length > 0 ? 200 : 404);
        responseLength.set(this.data.length);
    }

    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
        boolean dataAvailable = false;

        if (this.offset < this.data.length) {
            final int min = Math.min(bytesToRead, this.data.length - this.offset);
            System.arraycopy(this.data, this.offset, dataOut, 0, min);
            this.offset += min;
            bytesRead.set(min);
            dataAvailable = true;
        } else {
            bytesRead.set(this.offset = 0);
        }

        return dataAvailable;
    }

}
