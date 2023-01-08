/*
 * MyFTBLauncher
 * Copyright (C) 2023 MyFTB <https://myftb.de>
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

package de.myftb.launcher.login.microsoft;

import java.io.IOException;
import java.util.function.Consumer;

import fi.iki.elonen.NanoHTTPD;

class OauthCallbackServer extends NanoHTTPD {
    private final String expectedState;
    private final Consumer<String> codeCallback;

    OauthCallbackServer(int port, String expectedState, Consumer<String> codeCallback) throws IOException {
        super("localhost", port);
        this.expectedState = expectedState;
        this.codeCallback = codeCallback;
        this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getMethod() != Method.GET || !"/login_callback".equals(session.getUri())) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Die angeforderte Seite konnte nicht gefunden werden");
        }

        if (!session.getParameters().containsKey("state") || !this.expectedState.equals(session.getParameters().get("state").get(0))) {
            this.codeCallback.accept(null);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "Die Anmeldung war nicht erfolgreich. Bitte starte den Anmeldeprozess über den Launcher neu.");
        }

        if (!session.getParameters().containsKey("code") || session.getParameters().get("code").isEmpty()) {
            this.codeCallback.accept(null);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "Die Anmeldung war nicht erfolgreich. Bitte starte den Anmeldeprozess über den Launcher neu.");
        }

        this.codeCallback.accept(session.getParameters().get("code").get(0));
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain", "Login erfolgreich, die Seite kann nun geschlossen werden.");
    }

}