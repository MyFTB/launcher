/*
 * MyFTBLauncher
 * Copyright (C) 2020 MyFTB <https://myftb.de>
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

import de.myftb.launcher.cef.schemes.LauncherScheme;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LauncherRequestHandler extends CefRequestHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(LauncherRequestHandler.class);
    private static final List<String> allowedSchemes = Arrays.asList("playerhead", "chrome-devtools", "modpackimage", "launcher");
    private static final List<String> allowedHosts = Arrays.asList("127.0.0.1", "localhost", "launcher.myftb.local"); //NOPMD
    private static final LauncherScheme launcherScheme = new LauncherScheme();

    private boolean checkRequest(CefRequest request) {
        try {
            URI uri = new URI(request.getURL());
            if (LauncherRequestHandler.allowedSchemes.contains(uri.getScheme())) {
                return false;
            }

            if (!LauncherRequestHandler.allowedHosts.contains(uri.getHost())) {
                LauncherRequestHandler.log.info("Externe URL blockiert: " + request.getURL());
                return true;
            }
        } catch (Exception e) {
            LauncherRequestHandler.log.info("Externe URL blockiert: " + request.getURL(), e);
            return true;
        }

        return false;
    }

    @Override
    public boolean onBeforeBrowse(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest, boolean b, boolean b1) {
        return this.checkRequest(cefRequest);
    }

    @Override
    public boolean onBeforeResourceLoad(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest) {
        return this.checkRequest(cefRequest);
    }

    @Override
    public CefResourceHandler getResourceHandler(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest) {
        try {
            URI uri = new URI(cefRequest.getURL());
            if (uri.getHost().equals("launcher.myftb.local")) {
                return LauncherRequestHandler.launcherScheme;
            }
        } catch (URISyntaxException e) {
            // Ignore
        }

        return super.getResourceHandler(cefBrowser, cefFrame, cefRequest);
    }
}
