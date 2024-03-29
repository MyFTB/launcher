/*
 * MyFTBLauncher
 * Copyright (C) 2024 MyFTB <https://myftb.de>
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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.sun.management.OperatingSystemMXBean;

import de.myftb.launcher.launch.ManifestHelper;
import de.myftb.launcher.models.modpacks.ModpackManifest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class LauncherConfig {

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    @Expose private String clientToken = UUID.randomUUID().toString();
    @Expose private String jvmArgs = "";

    @Expose private int maxMemory = LauncherConfig.getDefaultMemory();
    @Expose private int minMemory = Math.max(1024, this.maxMemory / 2);

    @Expose private int gameWidth = 854;
    @Expose private int gameHeight = 480;

    @Expose private String packKey = "";
    @Expose private String installationDir = "";
    @Expose private boolean allowWebstart = true;
    @Expose private List<String> lastPlayedPacks = new LinkedList<>();
    @Expose private Map<String, String> autoConfigs = new HashMap<>();

    @Expose private LauncherProfileStore launcherProfileStore = new LauncherProfileStore();

    public String getClientToken() {
        return this.clientToken;
    }

    public String getJvmArgs() {
        return this.jvmArgs;
    }

    public int getMinMemory() {
        return this.minMemory;
    }

    public int getMaxMemory() {
        return this.maxMemory;
    }

    public int getGameWidth() {
        return this.gameWidth;
    }

    public int getGameHeight() {
        return this.gameHeight;
    }

    public String getPackKey() {
        return this.packKey;
    }

    public String getInstallationDir() {
        return this.installationDir;
    }

    public boolean isAllowWebstart() {
        return this.allowWebstart;
    }

    public List<String> getLastPlayedPacks() {
        List<String> installed = ManifestHelper.getInstalledModpacks().stream().map(ModpackManifest::getName).collect(Collectors.toList());
        this.lastPlayedPacks.removeIf(pack -> !installed.contains(pack));

        return this.lastPlayedPacks;
    }

    public void addLastPlayedPack(String name) {
        this.lastPlayedPacks = new LinkedList<>(this.lastPlayedPacks);
        this.lastPlayedPacks.removeIf(str -> str.equals(name));
        ((LinkedList<String>) this.lastPlayedPacks).addFirst(name);

        if (this.lastPlayedPacks.size() > 3) {
            ((LinkedList<String>) this.lastPlayedPacks).removeLast();
        }
    }

    public Map<String, String> getAutoConfigs() {
        return this.autoConfigs;
    }

    public LauncherProfileStore getLauncherProfileStore() {
        return this.launcherProfileStore;
    }

    /* ======================================== Serialisierung ======================================== */

    public void save(File dir) throws IOException {
        File configFile = new File(dir, "config.json");
        Files.write(configFile.toPath(), this.gson.toJson(this, LauncherConfig.class).getBytes(StandardCharsets.UTF_8));
    }

    public JsonObject toJson() {
        return this.gson.toJsonTree(this).getAsJsonObject();
    }

    public LauncherConfig readConfig(JsonObject jsonObject) {
        return this.gson.fromJson(jsonObject, LauncherConfig.class);
    }

    public LauncherConfig readConfig(File dir) throws IOException {
        File configFile = new File(dir, "config.json");
        if (!configFile.isFile()) {
            return this;
        }

        LauncherConfig config = this.gson.fromJson(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8), LauncherConfig.class);
        return config;
    }

    private static int getDefaultMemory() {
        try {
            long memorySize = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize() / 1048576;
            if (memorySize >= 16000) {
                return 8192;
            } else if (memorySize >= 12000) {
                return 6144;
            } else if (memorySize >= 8000) {
                return 4096;
            }
        } catch (Exception e) {
            // ignore
        }

        return 1024;
    }

}
