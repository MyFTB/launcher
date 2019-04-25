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

package de.myftb.launcher.models.modpacks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.myftb.launcher.Launcher;
import de.myftb.launcher.models.minecraft.MinecraftVersionManifest;

import java.io.File;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModpackManifest {
    private String title;
    private String name;
    private String version;
    private String gameVersion;
    private MinecraftVersionManifest versionManifest;
    private Map<String, List<String>> launch;

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

}