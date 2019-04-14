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

import java.util.HashMap;
import java.util.Map;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;

public class AuthRequestHandler extends CefRequestHandlerAdapter {
    private final String uid;

    public AuthRequestHandler(String uid) {
        this.uid = uid;
    }

    private void addHeader(CefRequest request) {
        Map<String, String> headerMap = new HashMap<>();
        request.getHeaderMap(headerMap);
        headerMap.put("ClientUID", this.uid);
        request.setHeaderMap(headerMap);
    }

    @Override
    public boolean onBeforeBrowse(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest, boolean b, boolean b1) {
        this.addHeader(cefRequest);
        return false;
    }

    @Override
    public boolean onBeforeResourceLoad(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest) {
        this.addHeader(cefRequest);
        return false;
    }

}
