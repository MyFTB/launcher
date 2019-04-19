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

import com.google.gson.JsonObject;
import com.mojang.authlib.exceptions.AuthenticationException;

import de.myftb.launcher.cef.AuthRequestHandler;
import de.myftb.launcher.cef.BlockExternalRequestHandler;
import de.myftb.launcher.cef.DevToolsContextMenuHandler;
import de.myftb.launcher.cef.SeqRequestHandler;
import de.myftb.launcher.cef.ipc.TopicMessageHandler;
import de.myftb.launcher.gui.CefFrame;
import de.myftb.launcher.models.launcher.LauncherConfig;

import io.javalin.Javalin;
import io.javalin.UnauthorizedResponse;
import io.javalin.core.util.JettyServerUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import javax.swing.JFrame;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    private static boolean development = false;
    private static Launcher instance;

    private final Javalin webServer;

    private final CefApp cefApp;
    private final CefBrowser cefBrowser;
    private final JFrame window;
    private final TopicMessageHandler ipcHandler;
    private final IpcTopics ipcTopics;

    private LauncherConfig config;

    public Launcher() {
        String uuid = UUID.randomUUID().toString();
        if (!Launcher.development) {
            this.webServer = Javalin.create()
                    .server(() -> {
                        Server server = JettyServerUtil.defaultServer();
                        ((QueuedThreadPool) server.getThreadPool()).setDaemon(true);

                        ServerConnector connector = new ServerConnector(server);
                        connector.setHost("127.0.0.1"); //NOPMD
                        server.setConnectors(new Connector[]{ connector });

                        return server;
                    })
                    .before(ctx -> {
                        if (!uuid.equals(ctx.header("ClientUID"))) {
                            throw new UnauthorizedResponse();
                        }
                    })
                    .disableStartupBanner()
                    .enableStaticFiles("/webroot")
                    .port(0)
                    .start();
        } else {
            this.webServer = null;
        }

        try {
            this.config = new LauncherConfig();


            this.config = this.config.readConfig(this.getExecutableDirectory()); // Workaround damit Profile korrekt gelesen werden.
            this.config = this.config.readConfig(this.getExecutableDirectory()); // Zukünftig vielleicht einen Custom (De)Serializer?
            this.saveConfig();

            this.cefApp = CefFrame.getApp();
            CefApp.CefVersion version = cefApp.getVersion();
            Launcher.log.info("JCef Version: {}", version.getJcefVersion());
            Launcher.log.info("Cef Version: {}", version.getCefVersion());
            Launcher.log.info("Chrome Version: {}", version.getChromeVersion());

            CefClient client = this.cefApp.createClient();
            client.addRequestHandler(new SeqRequestHandler(new BlockExternalRequestHandler(), new AuthRequestHandler(uuid)));
            client.addContextMenuHandler(new DevToolsContextMenuHandler());

            CefMessageRouter.CefMessageRouterConfig routerConfig = new CefMessageRouter.CefMessageRouterConfig();
            routerConfig.jsQueryFunction = "ipcQuery";
            routerConfig.jsCancelFunction = "cancelIpcQuery";
            CefMessageRouter ipcRouter = CefMessageRouter.create(routerConfig);
            ipcRouter.addHandler(this.ipcHandler = new TopicMessageHandler(), true);
            client.addMessageRouter(ipcRouter);
            this.ipcTopics = new IpcTopics(this, this.ipcHandler);
            this.setupIpcCommunication();

            this.cefBrowser = client.createBrowser("http://127.0.0.1:" + (Launcher.development ? 8080 : this.webServer.port()),
                    OS.isLinux(), false);

            this.window = new CefFrame(this.cefBrowser);
            this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.window.setTitle("MyFTB Launcher v" + this.getVersion());
            this.window.setSize(960, 576);
            this.window.setMinimumSize(this.window.getSize());
            this.window.setVisible(true);
        } catch (Exception e) {
            if (this.webServer != null) {
                this.webServer.stop();
            }
            throw new RuntimeException(e);
        }
    }

    private void setupIpcCommunication() {
        this.ipcHandler.listenAsync("renderer_arrived", this.ipcTopics::onRendererArrived);
        this.ipcHandler.listenAsync("mc_login", this.ipcTopics::onMcLogin);
        this.ipcHandler.listen("request_settings", this.ipcTopics::onRequestSettings);
        this.ipcHandler.listen("submit_settings", this.ipcTopics::onSubmitSettings);
        this.ipcHandler.listenAsync("open_url", this.ipcTopics::onOpenUrl);
        this.ipcHandler.listenAsync("open_directory_browser", this.ipcTopics::onOpenDirectoryBrowser);
        this.ipcHandler.listenAsync("request_installable_modpacks", this.ipcTopics::onRequestInstallableModpacks);
        this.ipcHandler.listenAsync("request_installed_modpacks", this.ipcTopics::onRequestInstalledModpacks);
        this.ipcHandler.listenAsync("install_modpack", this.ipcTopics::onInstallModpack);
        this.ipcHandler.listenAsync("launch_modpack", this.ipcTopics::onLaunchModpack);
    }

    /**
     * Meldet das in der Konfiguration angegebene Minecraft-Profil an.
     * Eine UI-Benachrichtigung über das Topic "logged_in" wird ebenfalls versendet
     *
     * @throws AuthenticationException Fehler bei der Anmeldung
     */
    void login() throws AuthenticationException {
        this.config.getProfile().logIn();
        this.saveConfig();
        Launcher.log.info("Minecraft Account angemeldet: " + this.config.getProfile().getSelectedProfile());
        this.ipcHandler.send("logged_in", this.config.getProfile().getSelectedProfile());
    }

    /**
     * Speichert die aktuelle Konfiguration in die zugehörige Datei.
     */
    void saveConfig() {
        try {
            this.config.save(this.getExecutableDirectory());
        } catch (IOException e) {
            Launcher.log.warn("Fehler beim Speichern der Konfiguration", e);
        }
    }

    /**
     * Setzt geänderte Konfigurationswerte aus dem übergebenen Objekt und speichert die resultierende Konfiguration.
     * @param newValues Neue Konfigurationswerte
     */
    void mergeConfig(JsonObject newValues) {
        JsonObject current = this.config.toJson();
        newValues.entrySet().forEach(entry -> current.add(entry.getKey(), entry.getValue()));
        this.config = this.config.readConfig(current);
        this.saveConfig();
        Launcher.log.info("Einstellungen gespeichert");
    }

    /**
     * Gibt das Installationsverzeichnis des Launchers zurück.
     * In Entwicklungsumgebungen ist dies das aktuelle Arbeitsverzeichnis.
     *
     * <p>Hier lässt sich z.B. die aktuelle Java Runtime oder CEF finden.
     *
     * <p>Dies ist nicht zwingend der Speicherort für Instanzen, Bibliotheken, etc.
     * Diese werden in {@link Launcher#getSaveDirectory()} abgelegt.
     *
     * @return Installationsverzeichnis des Launchers
     */
    public File getExecutableDirectory() {
        if (Launcher.development) {
            return new File(".");
        }

        try {
            return new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (URISyntaxException e) {
            return new File(".");
        }
    }

    /**
     * Gibt das Speicherverzeichnis für Instanzen, Bibliotheken, etc. zurück.
     * Ist in der Konfiguration ein gültiger Speicherpfad angegeben, wird dieser verwendet. ({@link LauncherConfig#getInstallationDir()})
     *
     * <p>Andernfalls ist dies das selbe Verzeichnis wie {@link Launcher#getExecutableDirectory()}.
     *
     * @return Speicherverzeichnis für Spielinhalte
     */
    public File getSaveDirectory() {
        String installationDir = this.config.getInstallationDir();
        if (!installationDir.isEmpty()) {
            File dir = new File(installationDir);
            if (dir.isDirectory()) {
                return dir;
            }
        }

        return this.getExecutableDirectory();
    }

    /**
     * Gibt ein benanntes Unterverzeichnis aus {@link Launcher#getSaveDirectory()} zurück.
     * Existiert dieses noch nicht, wird es erstellt.
     *
     * @param name Name des Unterverzeichnisses
     * @return Unterverzeichnis aus {@link Launcher#getSaveDirectory()} mit angegebenem Namen
     */
    public File getSaveSubDirectory(String name) {
        File subDir = new File(this.getSaveDirectory(), name);
        subDir.mkdirs();
        return subDir;
    }

    public LauncherConfig getConfig() {
        return this.config;
    }

    public String getVersion() {
        return "@VERSION@"; //TODO
    }

    public static void main(String[] args) {
        if ("dev".equals(System.getProperty("environment", "production"))) {
            Launcher.development = true;
        }

        Launcher.instance = new Launcher();
    }

    public static Launcher getInstance() {
        return Launcher.instance;
    }

}
