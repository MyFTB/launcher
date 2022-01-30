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
package de.myftb.launcher.cef.gui;

import de.myftb.launcher.Launcher;
import de.myftb.launcher.cef.LauncherCefAppHandler;

import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CefFrame extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(CefFrame.class);
    public static final long serialVersionUID = -8523171301600433735L;
    private static int browserCount = 0;

    public CefFrame(CefBrowser browser) {
        this.getContentPane().add(browser.getUIComponent());

        try {
            this.setIconImage(ImageIO.read(Launcher.class.getResourceAsStream("/icon.png")));
        } catch (IOException e) {
            CefFrame.log.warn("Fehler beim Setzen des Fenstericons", e);
        }

        browser.getClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser cefBrowser) {
                CefFrame.browserCount++;
            }

            @Override
            public void onBeforeClose(CefBrowser cefBrowser) {
                if (--CefFrame.browserCount == 0) {
                    if (DevToolsDialog.devTools != null) {
                        DevToolsDialog.devTools.dispose();
                    }

                    CefApp.getInstance().dispose();
                }
            }
        });
    }

    public static CefApp getApp() {
        if (!CefApp.startup()) {
            return null;
        }

        CefSettings settings = new CefSettings();
        settings.user_agent = "MyFTB Launcher";
        settings.windowless_rendering_enabled = false;
        CefApp cefApp = CefApp.getInstance(settings);
        CefApp.addAppHandler(new LauncherCefAppHandler());
        return cefApp;
    }

}
