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
package de.myftb.launcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;

import de.myftb.launcher.cef.ipc.TopicMessageHandler;
import de.myftb.launcher.launch.LaunchHelper;
import de.myftb.launcher.launch.LaunchMinecraft;
import de.myftb.launcher.launch.ManifestHelper;
import de.myftb.launcher.models.launcher.Platform;
import de.myftb.launcher.models.modpacks.ModpackManifest;
import de.myftb.launcher.models.modpacks.ModpackManifestList;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.swing.JFileChooser;

import mslinks.ShellLink;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpcTopics {
    private static final Logger log = LoggerFactory.getLogger(IpcTopics.class);
    private final Launcher launcher;
    private final TopicMessageHandler ipcHandler;
    private JsonArray posts = null;

    public IpcTopics(Launcher launcher, TopicMessageHandler ipcHandler) {
        this.launcher = launcher;
        this.ipcHandler = ipcHandler;
    }

    void onRendererArrived(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        JsonObject cbResponse = new JsonObject();

        this.ipcHandler.send("update_profiles", this.launcher.getConfig().getGameProfiles());

        if (this.launcher.getConfig().getSelectedProfile() == null) {
            cbResponse.addProperty("login_needed", true);
            cbResponse.addProperty("new_profile", true);
        } else {
            Map<String, Object> profileData = this.launcher.getConfig().getSelectedProfile().saveForStorage();
            if (profileData.containsKey("username")) {
                cbResponse.addProperty("login_username", (String) profileData.get("username"));
            }

            if (!this.launcher.getConfig().getSelectedProfile().isLoggedIn() && !this.launcher.getConfig().getSelectedProfile().canLogIn()) {
                cbResponse.addProperty("login_needed", true);
                cbResponse.addProperty("new_profile", false);
            } else {
                try {
                    this.launcher.login();
                } catch (AuthenticationException e) {
                    IpcTopics.log.warn("Fehler beim Login", e);
                    cbResponse.addProperty("login_needed", true);
                    cbResponse.addProperty("new_profile", false);
                }
            }
        }

        callback.success(cbResponse);
    }

    void onMcLogin(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        if (data.get("new_profile").getAsBoolean()) {
            this.launcher.getConfig().addProfile(this.launcher.getConfig().getAuthenticationService().createUserAuthentication(Agent.MINECRAFT));
        } else {
            this.launcher.getConfig().setProfile(0, this.launcher.getConfig().getAuthenticationService().createUserAuthentication(Agent.MINECRAFT));
        }
        this.launcher.getConfig().getSelectedProfile().setUsername(data.get("username").getAsString());
        this.launcher.getConfig().getSelectedProfile().setPassword(data.get("password").getAsString());
        try {
            this.launcher.login();

            List<UserAuthentication> profiles = this.launcher.getConfig().getProfiles();
            for (int i = 0; i < profiles.size(); i++) {
                if (i > 0 && this.launcher.getConfig().getSelectedProfile().getUserID().equals(profiles.get(i).getUserID())) {
                    this.launcher.getConfig().removeProfile(i);
                    break;
                }
            }

            this.ipcHandler.send("update_profiles", this.launcher.getConfig().getGameProfiles());
            callback.success(new JsonObject());
        } catch (Exception e) {
            this.launcher.getConfig().removeProfile(0);
            IpcTopics.log.warn("Fehler beim Login", e);
            callback.failure(e.getLocalizedMessage());
        }
    }

    void onSwitchProfile(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        this.launcher.getConfig().setSelectedProfile(data.get("index").getAsInt());

        if (!this.launcher.getConfig().getSelectedProfile().isLoggedIn() && !this.launcher.getConfig().getSelectedProfile().canLogIn()) {
            JsonObject jsonObject = new JsonObject();
            Map<String, Object> profileData = this.launcher.getConfig().getSelectedProfile().saveForStorage();
            if (profileData.containsKey("username")) {
                jsonObject.addProperty("username", (String) profileData.get("username"));
                jsonObject.addProperty("new_profile", false);
            }
            this.ipcHandler.send("show_login_form", jsonObject);
        }

        this.launcher.saveConfig();
        this.launcher.getIpcHandler().send("update_profiles", this.launcher.getConfig().getGameProfiles());
    }

    void onRequestSettings(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        callback.success(this.launcher.getConfig().toJson());
    }

    void onSubmitSettings(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        this.launcher.mergeConfig(data);
    }

    void onOpenUrl(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        try {
            URI url = new URI(data.get("url").getAsString());
            if (url.getHost().endsWith("myftb.de") && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(url);
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
            List<ModpackManifest> installedPacks = ManifestHelper.getInstalledModpacks();

            ModpackManifestList manifestList = new ModpackManifestList();
            manifestList.setPackages(this.launcher.getRemotePacks().getPackages().stream()
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

    void onRequestRecentPacks(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        try {
            JsonObject jsonObject = new JsonObject();
            List<ModpackManifest> installedPacks = ManifestHelper.getInstalledModpacks();
            List<ModpackManifest> recentPacks = Launcher.getInstance().getConfig().getLastPlayedPacks().stream()
                    .map(name -> installedPacks.stream().filter(manifest -> name.equals(manifest.getName())).findFirst().orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            jsonObject.addProperty("packages", LaunchHelper.mapper.writeValueAsString(recentPacks));
            callback.success(jsonObject);
        } catch (JsonProcessingException e) {
            callback.failure("Die Modpacks konnten nicht angezeigt werden");
            IpcTopics.log.warn("Fehler bei der Modpack-Serialisierung", e);
        }
    }

    Optional<Boolean> manifestInstallHelper(ModpackManifest manifest, JsonObject data, TopicMessageHandler.JsonQueryCallback callback)
            throws IOException {
        if ((manifest.getFeatures() != null && !manifest.getFeatures().isEmpty()) && !data.has("selected_features")) {
            data.addProperty("features", LaunchHelper.mapper.writeValueAsString(manifest.getFeatures()));
            callback.success(data);
        } else {
            List<String> selectedFeatures = (manifest.getFeatures() == null || manifest.getFeatures().isEmpty())
                    ? Collections.emptyList()
                    : StreamSupport.stream(data.getAsJsonArray("selected_features").spliterator(), false)
                    .map(JsonElement::getAsString)
                    .collect(Collectors.toList());
            IpcTopics.log.info("Installiere " + manifest.getTitle() + " mit Features: " + selectedFeatures);

            boolean success = LaunchMinecraft.install(manifest, selectedFeatures, (total, finished, failed) -> {
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
            jsonObject.addProperty("success", success);
            callback.success(jsonObject);
            return Optional.of(success);
        }

        return Optional.empty();
    }

    void onInstallModpack(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        try {
            ModpackManifestList.ModpackManifestReference reference = new Gson().fromJson(data, ModpackManifestList.ModpackManifestReference.class);
            ModpackManifest manifest = reference.getLocation() == null || reference.getLocation().isEmpty()
                    ? ManifestHelper.getManifestByName(reference.getName())
                    : ManifestHelper.getManifestByReference(reference);
            this.manifestInstallHelper(manifest, data, callback);
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

        try {
            this.launcher.launchModpack(modpack.get(), () -> {
                JsonObject launching = new JsonObject();
                launching.addProperty("launching", true);
                callback.success(launching);
            });

            JsonObject closed = new JsonObject();
            closed.addProperty("closed", true);
            callback.success(closed);
        } catch (LaunchMinecraft.ModpackOutdatedException outdatedEx) {
            try {
                ModpackManifest manifest = ManifestHelper.getManifestByReference(outdatedEx.getModpack());
                this.manifestInstallHelper(manifest, data, callback).ifPresent(success -> {
                    if (success) {
                        this.onLaunchModpack(data, callback);
                    } else {
                        callback.failure("Das Modpack konnte nicht aktualisiert werden");
                    }
                });
            } catch (IOException e) {
                callback.failure("Das Modpack konnte nicht gestartet werden");
                IpcTopics.log.warn("Das Modpack " + modpack.get().getName() + " konnte nicht gestartet werden", e);
            }
        } catch (IllegalStateException e) {
            callback.failure(e.getLocalizedMessage());
        } catch (IOException | InterruptedException e) {
            callback.failure("Das Modpack konnte auf Grund eines Fehlers nicht gestartet werden");
            IpcTopics.log.warn("Das Modpack " + modpack.get().getName() + " konnte nicht gestartet werden", e);
        }
    }

    void onModpackContextMenuClick(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        int index = data.get("index").getAsInt();
        Optional<ModpackManifest> modpack = ManifestHelper.getInstalledModpacks().stream()
                .filter(manifest -> manifest.getName().equals(data.get("pack").getAsString()))
                .findFirst();

        if (!modpack.isPresent()) {
            callback.failure("Das Modpack konnte nicht gefunden werden");
            return;
        }

        if (index == 1) { // Ordner
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(modpack.get().getInstanceDir());
                    callback.success(new JsonObject());
                } catch (IOException e) {
                    callback.failure("Der Ordner konnte nicht geöffnet werden");
                    IpcTopics.log.warn("Der Ordner für " + modpack.get().getName() + " konnte nicht geöffnet werden", e);
                }
            } else {
                callback.failure("Diese Aktion ist aktuell nicht ausführbar");
            }
        } else if (index == 2) { // Löschen
            try {
                Files.walk(modpack.get().getInstanceDir().toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                callback.success(new JsonObject());
            } catch (IOException e) {
                callback.failure("Das Modpack " + modpack.get().getName() + " konnte nicht gelöscht werden");
            }
        } else if (index == 4) { // Crashreport
            File crashReportsDir = new File(modpack.get().getInstanceDir(), "crash-reports");
            if (!crashReportsDir.isDirectory() || crashReportsDir.listFiles().length == 0) {
                callback.failure("Es wurden keine Crash-Reports gefunden");
                return;
            }

            Arrays.stream(crashReportsDir.listFiles(file -> file.getName().endsWith(".txt")))
                    .max(Comparator.comparing(File::lastModified))
                    .map(File::toPath)
                    .ifPresent(crashReport -> {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            try {
                                String pasteUrl = HastebinHelper.post(Files.readAllBytes(crashReport));
                                Desktop.getDesktop().browse(new URI(pasteUrl));
                                callback.success(new JsonObject());
                            } catch (IOException | URISyntaxException e) {
                                callback.failure("Der Crashreport konnte nicht hochgeladen werden");
                                IpcTopics.log.warn("Crashreport konnte nicht hochgeladen werden", e);
                            }
                        } else {
                            callback.failure("Diese Aktion ist aktuell nicht ausführbar");
                        }
                    });
        } else if (index == 5) { // Desktop-Verknüpfung
            String executablePath = System.getProperty("launcher.app.path");
            if (executablePath == null) {
                callback.failure("Der Pfad zum Launcher konnte nicht erkannt werden");
                return;
            }

            File desktop = new File(System.getProperty("user.home"), "Desktop");
            if (!desktop.isDirectory()) {
                callback.failure("Der Desktop konnte nicht gefunden werden");
                return;
            }

            try {
                Platform platform = Platform.getPlatform();
                boolean needsIco = platform == Platform.WINDOWS;
                File modpackImage = new File(this.launcher.getSaveSubDirectory("cache"),
                        UUID.randomUUID().toString() + (needsIco ? ".ico" : ".png"));
                modpack.get().saveModpackLogo(modpackImage);

                if (platform == Platform.WINDOWS) {
                    ShellLink link = ShellLink.createLink(executablePath)
                            .setWorkingDir(new File(executablePath).getParentFile().getAbsolutePath())
                            .setName(modpack.get().getTitle())
                            .setCMDArgs("--pack \"" + modpack.get().getName() + "\"")
                            .setIconLocation(modpackImage.isFile() ? modpackImage.getAbsolutePath() : null);
                    link.getHeader().setIconIndex(0);
                    link.saveTo(new File(desktop, modpack.get().getTitle() + ".lnk").getAbsolutePath());
                    callback.success(new JsonObject());
                } else if (platform == Platform.LINUX) {
                    File desktopEntry = new File(desktop, modpack.get().getTitle() + ".desktop");
                    Files.write(desktopEntry.toPath(), Arrays.asList(
                            "[Desktop Entry]",
                            "Name=" + modpack.get().getTitle(),
                            "Exec=" + executablePath + " --pack \"" + modpack.get().getName() + "\"",
                            "Icon=" + modpackImage.getAbsolutePath(),
                            "Categories=Game",
                            "Path=" + new File(executablePath).getParentFile().getAbsolutePath(),
                            "Terminal=false"
                    ), StandardCharsets.UTF_8);
                } else {
                    callback.failure("Verknüpfungen werden auf dieser Platform nicht unterstützt");
                }
            } catch (IOException e) {
                callback.failure("Die Desktop-Verknüpfung konnte nicht angelegt werden");
                IpcTopics.log.warn("Desktop-Verknüpfung konnte nicht angelegt werden", e);
            }
        }
    }

    void onLogout(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        this.launcher.getConfig().removeProfile(0);
        this.launcher.saveConfig();
        this.ipcHandler.send("update_profiles", this.launcher.getConfig().getGameProfiles());

        if (this.launcher.getConfig().getGameProfiles().size() == 0) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("new_profile", true);

            this.ipcHandler.send("show_login_form", jsonObject);
        }
    }

    void onRequestPosts(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        if (this.posts == null) {
            try {
                HttpResponse response = HttpRequest.get(Constants.postsApi)
                        .execute()
                        .returnResponse();

                this.posts = new Gson().fromJson(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8), JsonElement.class)
                        .getAsJsonArray();
            } catch (IOException e) {
                callback.failure("Die Beiträge konnten nicht abgerufen werdne");
                IpcTopics.log.warn("Fehler beim Abrufen der Website Posts", e);
                return;
            }
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("posts", this.posts);
        callback.success(jsonObject);
    }

    void onRequestConsole(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        this.ipcHandler.sendString("console_data", LaunchMinecraft.getLog());
    }

    void onUploadLog(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        String log = LaunchMinecraft.getLog();

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                String pasteUrl = HastebinHelper.post(log.getBytes(StandardCharsets.UTF_8));
                Desktop.getDesktop().browse(new URI(pasteUrl));
                callback.success(new JsonObject());
            } catch (Exception e) {
                callback.failure("Der Log konnte nicht hochgeladen werden");
                IpcTopics.log.warn("Log konnte nicht hochgeladen werden", e);
            }
        } else {
            callback.failure("Diese Aktion ist aktuell nicht ausführbar");
        }
    }

    void onKillMinecraft(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        LaunchMinecraft.killMinecraft();
    }

    void onCancelDownload(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        LaunchMinecraft.cancelDownload = true;
    }

    void onRequestAutoconfigs(JsonObject data, TopicMessageHandler.JsonQueryCallback callback) {
        JsonArray configs = new JsonArray();

        this.launcher.getAutoConfigManager().getKnownOptions().forEach(option -> {
            JsonObject optionObject = new JsonObject();
            optionObject.addProperty("id", option);
            optionObject.addProperty("name", this.launcher.getAutoConfigManager().getTranslation(option).orElse(option));
            optionObject.addProperty("type", this.launcher.getAutoConfigManager().getType(option).map(Class::getSimpleName).orElse("").toLowerCase());
            configs.add(optionObject);
        });

        JsonObject response = new JsonObject();
        response.add("configs", configs);
        response.add("types", this.launcher.getAutoConfigManager().getTypes());
        response.add("constraints", this.launcher.getAutoConfigManager().getConstraints());
        callback.success(response);
    }

}
