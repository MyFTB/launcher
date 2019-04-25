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

package de.myftb.launcher.cef;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockExternalRequestHandler extends CefRequestHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(BlockExternalRequestHandler.class);
    private static final List<String> allowedSchemes = Arrays.asList("playerhead", "chrome-devtools", "modpackimage");

    private boolean checkRequest(CefRequest request) {
        try {
            URI uri = new URI(request.getURL());
            if (BlockExternalRequestHandler.allowedSchemes.contains(uri.getScheme())) {
                return false;
            }

            if (!"127.0.0.1".equals(uri.getHost()) && !"localhost".equals(uri.getHost())) { //NOPMD
                BlockExternalRequestHandler.log.info("Externe URL blockiert: " + request.getURL());
                return true;
            }
        } catch (Exception e) {
            BlockExternalRequestHandler.log.info("Externe URL blockiert: " + request.getURL(), e);
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

}
