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

package de.myftb.launcher;

import org.apache.http.client.fluent.Request;

import java.net.URI;

public class HttpRequest {

    private static Request configure(Request request) {
        return request
                .connectTimeout(Constants.connectTimeout)
                .socketTimeout(Constants.socketTimeout)
                .addHeader("User-Agent", "MyFTBLauncher v" + Launcher.getVersion());
    }

    public static Request get(String url) {
        return HttpRequest.configure(Request.Get(url));
    }

    public static Request get(URI url) {
        return HttpRequest.configure(Request.Get(url));
    }

    public static Request post(String url) {
        return HttpRequest.configure(Request.Post(url));
    }

}
