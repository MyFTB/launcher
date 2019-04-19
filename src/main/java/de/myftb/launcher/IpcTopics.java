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

package de.myftb.launcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;

import de.myftb.launcher.cef.ipc.TopicMessageHandler;
import de.myftb.launcher.launch.LaunchHelper;
import de.myftb.launcher.launch.LaunchMinecraft;
import de.myftb.launcher.launch.ManifestHelper;
import de.myftb.launcher.models.modpacks.ModpackManifest;
import de.myftb.launcher.models.modpacks.ModpackManifestList;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.swing.JFileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpcTopics {
    private static final Logger log = LoggerFactory.getLogger(IpcTopics.class);
    private final Launcher launcher;
    private final TopicMessageHandler ipcHandler;

    private ModpackManifestList modpackList;

    public IpcTopics(Launcher launcher, TopicMessageHandler ipcHandler) {
        this.launcher = launcher;
        this.ipcHandler = ipcHandler;
    }

    void onRendererArrived(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        JsonObject cbResponse = new JsonObject();

        if (this.launcher.getConfig().getProfile() == null) {
            cbResponse.addProperty("login_needed", true);
        } else {
            Map<String, Object> profileData = this.launcher.getConfig().getProfile().saveForStorage();
            if (profileData.containsKey("username")) {
                cbResponse.addProperty("login_username", (String) profileData.get("username"));
            }


            if (!this.launcher.getConfig().getProfile().isLoggedIn() && !this.launcher.getConfig().getProfile().canLogIn()) {
                cbResponse.addProperty("login_needed", true);
            } else {
                try {
                    this.launcher.login();
                } catch (AuthenticationException e) {
                    IpcTopics.log.warn("Fehler beim Login", e);
                    cbResponse.addProperty("login_needed", true);
                }
            }
        }

        callback.success(cbResponse);
    }

    void onMcLogin(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        this.launcher.getConfig().setProfile(this.launcher.getConfig().getAuthenticationService().createUserAuthentication(Agent.MINECRAFT));
        this.launcher.getConfig().getProfile().setUsername(data.get("username").getAsString());
        this.launcher.getConfig().getProfile().setPassword(data.get("password").getAsString());
        try {
            this.launcher.login();
            callback.success(new JsonObject());
        } catch (Exception e) {
            IpcTopics.log.warn("Fehler beim Login", e);
            callback.failure(e.getLocalizedMessage());
        }
    }

    void onRequestSettings(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        callback.success(this.launcher.getConfig().toJson());
    }

    void onSubmitSettings(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        this.launcher.mergeConfig(data);
    }

    void onOpenUrl(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        try {
            URL url = new URL(data.get("url").getAsString());
            if (url.getHost().endsWith("myftb.de") && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(url.toURI());
            }
        } catch (Exception e) {
            callback.failure("URL kann nicht gelesen werden");
        }
    }

    void onOpenDirectoryBrowser(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setDialogTitle("Bitte wähle den Speicherort für installierte Modpacks");
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setAcceptAllFileFilterUsed(false);
        if (dirChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("directory", dirChooser.getSelectedFile().toString());
            callback.success(jsonObject);
        }
    }

    void onRequestInstallableModpacks(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        try {
            if (this.modpackList == null) {
                this.modpackList = ManifestHelper.getManifests();
            }

            List<ModpackManifest> installedPacks = ManifestHelper.getInstalledModpacks();

            ModpackManifestList manifestList = new ModpackManifestList();
            manifestList.setPackages(this.modpackList.getPackages().stream()
                    .filter(manifestRef -> installedPacks.stream()
                            .noneMatch(manifest -> manifest.getName().equals(manifestRef.getName())))
                    .collect(Collectors.toList())
            );

            callback.success(manifestList);
        } catch (Exception e) {
            callback.failure(e.getLocalizedMessage());
            IpcTopics.log.warn("Fehler beim Abfragen der Packliste", e);
        }
    }

    void onRequestInstalledModpacks(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("packages", LaunchHelper.mapper.writeValueAsString(ManifestHelper.getInstalledModpacks()));
            callback.success(jsonObject);
        } catch (JsonProcessingException e) {
            callback.failure("Die Modpacks konnten nicht angezeigt werden");
            IpcTopics.log.warn("Fehler bei der Modpack-Serialisierung", e);
        }
    }

    void onInstallModpack(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        try {
            ModpackManifestList.ModpackManifestReference reference = new Gson().fromJson(data, ModpackManifestList.ModpackManifestReference.class);
            ModpackManifest manifest = ManifestHelper.getManifest(reference);
            if ((manifest.getFeatures() != null && !manifest.getFeatures().isEmpty()) && !data.has("selected_features")) {

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("features", LaunchHelper.mapper.writeValueAsString(manifest.getFeatures()));
                callback.success(jsonObject);

            } else {
                List<String> selectedFeatures = (manifest.getFeatures() == null || manifest.getFeatures().isEmpty())
                        ? Collections.emptyList()
                        : StreamSupport.stream(data.getAsJsonArray("selected_features").spliterator(), false)
                        .map(JsonElement::getAsString)
                        .collect(Collectors.toList());
                IpcTopics.log.info("Installiere " + manifest.getTitle() + " mit Features: " + selectedFeatures);

                LaunchMinecraft.install(manifest, selectedFeatures, (total, finished, failed) -> {
                    JsonObject jsonObject = new JsonObject();
                    JsonObject status = new JsonObject();
                    status.addProperty("total", total);
                    status.addProperty("finished", finished);
                    status.addProperty("failed", failed);
                    jsonObject.add("installing", status);
                    callback.success(jsonObject);
                });

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("installed", true);
                callback.success(jsonObject);
            }
        } catch (Exception e) {
            callback.failure("Modpack konnte nicht installiert werden");
            IpcTopics.log.warn("Fehler bei der Modpack-Installation", e);
        }
    }

    void onLaunchModpack(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        Optional<ModpackManifest> modpack = ManifestHelper.getInstalledModpacks().stream()
                .filter(manifest -> manifest.getName().equals(data.get("modpack").getAsString()))
                .findFirst();

        if (!modpack.isPresent()) {
            callback.failure("Das Modpack konnte nicht gefunden werden");
            return;
        }

        if (this.launcher.getConfig().getProfile() == null) {
            this.ipcHandler.send("show_login_form", new JsonObject());
            return;
        }

        boolean loggedIn = false;
        if (this.launcher.getConfig().getProfile().isLoggedIn() || this.launcher.getConfig().getProfile().canLogIn()) {
            try {
                this.launcher.login();
                loggedIn = true;
            } catch (AuthenticationException e) {
                e.printStackTrace();
            }
        }

        if (!loggedIn) {
            JsonObject jsonObject = new JsonObject();
            Map<String, Object> profileData = this.launcher.getConfig().getProfile().saveForStorage();
            if (profileData.containsKey("username")) {
                jsonObject.addProperty("username", (String) profileData.get("username"));
            }
            this.ipcHandler.send("show_login_form", jsonObject);
            return;
        }

        try {
            JsonObject launching = new JsonObject();
            launching.addProperty("launching", true);
            callback.success(launching);

            LaunchMinecraft.launch(modpack.get(), this.launcher.getConfig().getProfile());

            JsonObject closed = new JsonObject();
            closed.addProperty("closed", true);
            callback.success(closed);
        } catch (IOException | InterruptedException e) {
            callback.failure("Das Modpack konnte auf Grund eines Fehlers nicht gestartet werden");
            IpcTopics.log.warn("Das Modpack " + modpack.get().getName() + " konnte nicht gestartet werden", e);
        }
    }

}
