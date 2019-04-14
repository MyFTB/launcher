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

import de.myftb.launcher.cef.AuthRequestHandler;
import de.myftb.launcher.cef.BlockExternalRequestHandler;
import de.myftb.launcher.cef.DevToolsContextMenuHandler;
import de.myftb.launcher.cef.SeqRequestHandler;
import de.myftb.launcher.gui.CefFrame;

import java.util.UUID;
import javax.swing.JFrame;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;
import io.javalin.UnauthorizedResponse;
import io.javalin.core.util.JettyServerUtil;

public class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    private static Launcher instance;

    private final Javalin webServer;

    private final CefApp cefApp;
    private final CefBrowser cefBrowser;
    private final JFrame window;

    public Launcher() {
        String uuid = UUID.randomUUID().toString();
        this.webServer = Javalin.create()
                .server(() -> {
                    Server server = JettyServerUtil.defaultServer();
                    ((QueuedThreadPool) server.getThreadPool()).setDaemon(true);

                    ServerConnector connector = new ServerConnector(server);
                    connector.setHost("127.0.0.1");
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

        try {
            this.cefApp = CefFrame.getApp();
            CefApp.CefVersion version = cefApp.getVersion();
            Launcher.log.info("JCef Version: {}", version.getJcefVersion());
            Launcher.log.info("Cef Version: {}", version.getCefVersion());
            Launcher.log.info("Chrome Version: {}", version.getChromeVersion());

            CefClient client = this.cefApp.createClient();
            client.addRequestHandler(new SeqRequestHandler(new BlockExternalRequestHandler(), new AuthRequestHandler(uuid)));
            client.addContextMenuHandler(new DevToolsContextMenuHandler());
            this.cefBrowser = client.createBrowser( "http://127.0.0.1:" + this.webServer.port(), OS.isLinux(), false);

            this.window = new CefFrame(this.cefBrowser);
            this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.window.setTitle("MyFTB Launcher");
            this.window.setSize(960, 576);
            this.window.setVisible(true);
        } catch (Exception e) {
            this.webServer.stop();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Launcher.instance = new Launcher();
    }

}
