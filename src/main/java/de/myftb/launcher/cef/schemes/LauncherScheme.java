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
package de.myftb.launcher.cef.schemes;

import de.myftb.launcher.cef.DataResourceHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LauncherScheme extends DataResourceHandler {
    private static final Logger log = LoggerFactory.getLogger(LauncherScheme.class);
    private static final Map<String, String> mimeTypeMap = new HashMap<>();

    @Override
    protected void fillDataForRequest(String path, Consumer<String> mimeTypeSetter, Consumer<byte[]> dataSetter) {
        if (path.isEmpty()) {
            path = "index.html";
        }

        mimeTypeSetter.accept(LauncherScheme.mimeTypeMap.getOrDefault(path.substring(path.lastIndexOf('.') + 1), "text/plain"));

        try (InputStream inputStream = LauncherScheme.class.getResourceAsStream("/webroot/" + path)) {
            if (inputStream == null) {
                dataSetter.accept(new byte[0]);
                return;
            }

            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                int read;
                byte[] buffer = new byte[1024];
                while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, read);
                }

                dataSetter.accept(byteArrayOutputStream.toByteArray());
            }
        } catch (IOException e) {
            LauncherScheme.log.warn("Fehler beim Laden von Launcher-Content", e);
        }
    }

    static {
        mimeTypeMap.put("html", "text/html");
        mimeTypeMap.put("js", "text/javascript");
        mimeTypeMap.put("png", "image/png");
    }

}
