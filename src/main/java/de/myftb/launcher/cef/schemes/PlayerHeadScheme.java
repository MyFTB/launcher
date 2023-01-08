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
package de.myftb.launcher.cef.schemes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;

import de.myftb.launcher.HttpRequest;
import de.myftb.launcher.Launcher;
import de.myftb.launcher.cef.DataResourceHandler;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerHeadScheme extends DataResourceHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerHeadScheme.class);
    private static final Map<String, byte[]> skinCache = new HashMap<>();
    private static final int targetSize = 256;

    @Override
    protected void fillDataForRequest(String path, Consumer<String> mimeTypeSetter, Consumer<byte[]> dataSetter) {
        path = path.replace("-", "");
        mimeTypeSetter.accept("image/png");

        if (path.length() == 32) {
            dataSetter.accept(PlayerHeadScheme.skinCache.computeIfAbsent(path, this::getSkin));
        } else {
            dataSetter.accept(new byte[0]);
        }
    }

    private byte[] getSkin(String uuid) {
        try {
            File cacheFile = new File(Launcher.getInstance().getSaveSubDirectory("cache"), uuid + ".png");
            if (!cacheFile.isFile() || (System.currentTimeMillis() - cacheFile.lastModified()) >= TimeUnit.DAYS.toMillis(1)) {
                byte[] skin = this.getRemoteSkin(uuid);
                if (skin.length > 0) {
                    Files.write(cacheFile.toPath(), skin);
                }
                return skin;
            }

            if (cacheFile.isFile()) {
                return Files.readAllBytes(cacheFile.toPath());
            }

            return new byte[0];
        } catch (IOException e) {
            PlayerHeadScheme.log.warn("Fehler bei der Skinabfrage", e);
            return new byte[0];
        }
    }

    private byte[] getRemoteSkin(String uuid) {
        try {
            HttpResponse response = HttpRequest.get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)
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
                        HttpResponse skinResponse = HttpRequest.get(skinUri)
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

}
