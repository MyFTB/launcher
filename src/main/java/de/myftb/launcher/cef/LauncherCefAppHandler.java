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
package de.myftb.launcher.cef;

import de.myftb.launcher.cef.schemes.ModpackImageScheme;
import de.myftb.launcher.cef.schemes.PlayerHeadScheme;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefCommandLine;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

public class LauncherCefAppHandler extends CefAppHandlerAdapter {

    public LauncherCefAppHandler() {
        super(new String[0]);
    }

    @Override
    public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        registrar.addCustomScheme("playerhead", true, false, false, false, true, false, false);
        registrar.addCustomScheme("modpackimage", true, false, false, false, true, false, false);
    }

    @Override
    public void onContextInitialized() {
        CefApp cefApp = CefApp.getInstance();
        SchemeHandlerFactory factory = new SchemeHandlerFactory();
        cefApp.registerSchemeHandlerFactory("playerhead", "", factory);
        cefApp.registerSchemeHandlerFactory("modpackimage", "", factory);
    }

    @Override
    public void onBeforeCommandLineProcessing(String s, CefCommandLine cefCommandLine) {
        cefCommandLine.appendSwitchWithValue("enable-features", "OverlayScrollbar");
        cefCommandLine.appendArgument("enable-smooth-scrolling");
    }

    private static class SchemeHandlerFactory implements CefSchemeHandlerFactory {
        @Override
        public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
            if ("playerhead".equals(schemeName)) {
                return new PlayerHeadScheme();
            } else if ("modpackimage".equals(schemeName)) {
                return new ModpackImageScheme();
            }

            return null;
        }
    }

}
