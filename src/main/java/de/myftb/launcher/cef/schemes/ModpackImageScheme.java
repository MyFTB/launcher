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

import de.myftb.launcher.Constants;
import de.myftb.launcher.Launcher;
import de.myftb.launcher.cef.DataResourceHandler;
import de.myftb.launcher.models.modpacks.ModpackManifestList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModpackImageScheme extends DataResourceHandler {
    private static final Logger log = LoggerFactory.getLogger(ModpackImageScheme.class);

    @Override
    protected void fillDataForRequest(String path, Consumer<String> mimeTypeSetter, Consumer<byte[]> dataSetter) {
        mimeTypeSetter.accept("image/png");

        try {
            Optional<String> imageLocation = Launcher.getInstance().getRemotePacks().getPackages().stream()
                    .filter(reference -> reference.getName().equals(path))
                    .findFirst()
                    .map(ModpackManifestList.ModpackManifestReference::getLocation)
                    .map(location -> location.substring(0, location.lastIndexOf('.')) + ".png");

            if (!imageLocation.isPresent()) {
                dataSetter.accept(new byte[0]);
                return;
            }

            File cacheFile = new File(Launcher.getInstance().getSaveSubDirectory("cache"), imageLocation.get());
            if (!cacheFile.isFile() || (System.currentTimeMillis() - cacheFile.lastModified()) >= 259200000) {
                System.out.println("Request " + "https://launcher.myftb.de/images/" + imageLocation.get());
                Request.Get("https://launcher.myftb.de/images/" + imageLocation.get())
                        .connectTimeout(Constants.connectTimeout)
                        .socketTimeout(Constants.socketTimeout)
                        .execute()
                        .saveContent(cacheFile);
            }

            if (cacheFile.isFile()) {
                dataSetter.accept(Files.readAllBytes(cacheFile.toPath()));
            }
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != 404) {
                ModpackImageScheme.log.warn("Fehler beim Abrufen von Modpacklogo für " + path, e);
            }
        } catch (IOException e) {
            ModpackImageScheme.log.warn("Fehler beim Abrufen von Modpacklogo für " + path, e);
        }
    }

}
