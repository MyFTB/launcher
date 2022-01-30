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
package de.myftb.launcher.models.modpacks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.io.Files;

import de.myftb.launcher.HttpRequest;
import de.myftb.launcher.Launcher;
import de.myftb.launcher.models.minecraft.MinecraftVersionManifest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import net.sf.image4j.codec.ico.ICOEncoder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModpackManifest {
    private String title;
    private String name;
    private String version;
    private String gameVersion;
    private MinecraftVersionManifest versionManifest; // TODO inheritsFrom korrekt beachten
    private Map<String, List<String>> launch;
    private String runtime;

    private List<Feature> features;
    private List<FileTask> tasks;

    public String getTitle() {
        return this.title;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getGameVersion() {
        return this.gameVersion;
    }

    public MinecraftVersionManifest getVersionManifest() {
        return this.versionManifest;
    }

    public Map<String, List<String>> getLaunch() {
        return this.launch;
    }

    public String getRuntime() {
        return this.runtime;
    }

    public List<Feature> getFeatures() {
        return this.features;
    }

    public List<FileTask> getTasks() {
        return this.tasks;
    }

    public File getInstanceDir() {
        File instanceDir = new File(Launcher.getInstance().getSaveSubDirectory("instances"), this.getName());
        instanceDir.mkdirs();
        return instanceDir;
    }

    public boolean saveModpackLogo(File target) throws IOException {
        Optional<String> imageLocation = Launcher.getInstance().getRemotePacks().getPackByName(this.name)
                .map(ModpackManifestList.ModpackManifestReference::getLocation)
                .map(location -> location.substring(0, location.lastIndexOf('.')) + ".png");

        if (!imageLocation.isPresent()) {
            return false;
        }

        if (!target.isFile() || (System.currentTimeMillis() - target.lastModified()) >= TimeUnit.DAYS.toMillis(3)) {
            File icoTarget = null;

            if (target.getName().endsWith(".ico")) {
                icoTarget = target;
                target = new File(target.getParentFile(), Files.getNameWithoutExtension(target.getName()) + ".png");
            }

            HttpRequest.get("https://launcher.myftb.de/images/" + imageLocation.get())
                    .execute()
                    .saveContent(target);

            if (icoTarget != null) {
                ICOEncoder.write(ImageIO.read(target), 32, icoTarget);
            }
        }

        return true;
    }

}