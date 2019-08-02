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

package de.myftb.launcher.launch;

import de.myftb.launcher.Constants;
import de.myftb.launcher.Launcher;
import de.myftb.launcher.models.minecraft.MinecraftVersionList;
import de.myftb.launcher.models.minecraft.MinecraftVersionManifest;
import de.myftb.launcher.models.modpacks.ModpackManifest;
import de.myftb.launcher.models.modpacks.ModpackManifestList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManifestHelper {
    private static final Logger log = LoggerFactory.getLogger(ManifestHelper.class);
    private static final Map<String, MinecraftVersionManifest> versionManifestCache = new HashMap<>();
    private static final Map<String, ModpackManifest> modpackManifestCache = new HashMap<>();

    public static void clearModpackCache() {
        ManifestHelper.modpackManifestCache.clear();
    }

    public static ModpackManifestList getManifests() throws IOException {
        return LaunchHelper.mapper.readValue(LaunchHelper.download(
                String.format(Constants.packList, Launcher.getInstance().getConfig().getPackKey()), null), ModpackManifestList.class);
    }

    public static MinecraftVersionManifest getManifest(String version) throws IOException {
        if (ManifestHelper.versionManifestCache.containsKey(version)) {
            return ManifestHelper.versionManifestCache.get(version);
        }

        String versionManifestString = LaunchHelper.download(Constants.versionManifestListUrl, null);
        MinecraftVersionList versionList = LaunchHelper.mapper.readValue(versionManifestString, MinecraftVersionList.class);

        MinecraftVersionList.MinecraftVersion mcVersion = versionList.getVersion(version)
                .orElseThrow(() -> new IllegalStateException("Ungültige Spielversion: " + version));

        MinecraftVersionManifest manifest = LaunchHelper.mapper.readValue(LaunchHelper.download(mcVersion.getUrl(), null),
                MinecraftVersionManifest.class);
        ManifestHelper.versionManifestCache.put(version, manifest);
        return manifest;
    }

    public static ModpackManifest getManifestByReference(ModpackManifestList.ModpackManifestReference reference) throws IOException {
        if (ManifestHelper.modpackManifestCache.containsKey(reference.getLocation())) {
            return ManifestHelper.modpackManifestCache.get(reference.getLocation());
        }

        ModpackManifest manifest = LaunchHelper.mapper.readValue(
                LaunchHelper.download(String.format(Constants.packManifest, reference.getLocation()), null),
                ModpackManifest.class
        );

        ManifestHelper.modpackManifestCache.put(reference.getLocation(), manifest);
        return manifest;
    }

    public static ModpackManifest getManifestByName(String name) throws IOException {
        Optional<ModpackManifestList.ModpackManifestReference> reference = Launcher.getInstance().getRemotePacks().getPackByName(name);
        if (reference.isPresent()) {
            return ManifestHelper.getManifestByReference(reference.get());
        }

        return null;
    }

    public static List<ModpackManifest> getInstalledModpacks() {
        File instancesDir = Launcher.getInstance().getSaveSubDirectory("instances");
        File[] instanceDirs = instancesDir.listFiles(File::isDirectory);
        if (instanceDirs == null) {
            return Collections.emptyList();
        }

        List<ModpackManifest> instances = new ArrayList<>();
        for (File instanceDir : instanceDirs) {
            File manifestFile = new File(instanceDir, "manifest.json");
            File successFile = new File(instanceDir, ".success");
            if (!successFile.isFile()) {
                continue;
            }

            try {
                byte[] successBytes = Files.readAllBytes(successFile.toPath());
                if (successBytes.length == 0 || successBytes[0] == 0) {
                    continue;
                }
            } catch (IOException e) {
                ManifestHelper.log.warn("Fehler beim Lesen von Modpack-Installationsstatus " + successFile.getAbsolutePath(), e);
                continue;
            }

            if (manifestFile.isFile()) {
                try {
                    instances.add(LaunchHelper.mapper.readValue(manifestFile, ModpackManifest.class));
                } catch (IOException e) {
                    ManifestHelper.log.warn("Fehler beim Lesen von Modpack-Manifest " + manifestFile.getAbsolutePath(), e);
                }
            }
        }

        return instances;
    }

}
