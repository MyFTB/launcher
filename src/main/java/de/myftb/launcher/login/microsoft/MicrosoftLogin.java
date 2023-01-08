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

package de.myftb.launcher.login.microsoft;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.myftb.launcher.Constants;
import de.myftb.launcher.HttpRequest;
import de.myftb.launcher.Launcher;
import de.myftb.launcher.login.LoginException;
import de.myftb.launcher.login.LoginService;
import de.myftb.launcher.login.microsoft.models.MinecraftAuthentication;
import de.myftb.launcher.login.microsoft.models.MinecraftProfile;
import de.myftb.launcher.login.microsoft.models.OauthResponse;
import de.myftb.launcher.login.microsoft.models.XboxAuthentication;
import de.myftb.launcher.models.launcher.LauncherProfile;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MicrosoftLogin implements LoginService {
    private static final Logger log = LoggerFactory.getLogger(MicrosoftLogin.class);
    private static final Gson gson = new Gson();
    private static final ObjectMapper mapper = new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final String MICROSOFT_PROFILE_PROVIDER = "microsoft";

    private static OauthCallbackServer oauthCallbackServer = null;
    private static String expectedState = null;

    private static void startOauthCallbackServer() throws IOException {
        int redirectUriPort = 45535 + new Random().nextInt(10000);
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            redirectUriPort = serverSocket.getLocalPort();
        } catch (IOException e) {
        }

        MicrosoftLogin.expectedState = UUID.randomUUID().toString();
        MicrosoftLogin.oauthCallbackServer = new OauthCallbackServer(redirectUriPort, MicrosoftLogin.expectedState, MicrosoftLogin::handleAuthCallback);
    }

    private static LauncherProfile loginFlow(String code, boolean refresh) throws LoginException {
        if (code == null) {
            throw new LoginException("Die Micrsoft-Anmeldung war nicht erfolgreich");
        }

        try {
            OauthResponse oauthResponse = getAccessToken(code, refresh);
            XboxAuthentication xblAuthentication = doXblAuthenticate(oauthResponse);
            XboxAuthentication xstsAuthentication = doXstsAuthenticate(xblAuthentication);
            MinecraftAuthentication minecraftAuthentication = doMinecraftXboxAuthenticate(xstsAuthentication);

            MinecraftProfile minecraftProfile = getMinecraftProfile(minecraftAuthentication);
            if (minecraftProfile.getId().isEmpty() || minecraftProfile.getName().isEmpty()) {
                throw new LoginException("Dieser Microsoft-Account besitzt kein gültiges Minecraft-Profil");
            }

            LauncherProfile launcherProfile = new LauncherProfile();
            launcherProfile.setProvider(MicrosoftLogin.MICROSOFT_PROFILE_PROVIDER);
            launcherProfile.setUuid(minecraftProfile.getUuid());
            launcherProfile.setLastKnownUsername(minecraftProfile.getName());
            launcherProfile.setProperties(new HashMap<>());
            launcherProfile.getProperties().put("minecraftAccessToken", minecraftAuthentication.getAccessToken());
            launcherProfile.getProperties().put("oauthRefreshToken", oauthResponse.getRefreshToken());
            return launcherProfile;
        } catch (IOException e) {
            throw new LoginException("Die Microsoft-Anmeldung war nicht erfolgreich", e);
        }
    }

    private static void handleAuthCallback(String code) {
        try {
            Launcher.getInstance().getConfig().getLauncherProfileStore().commit(Collections.singleton(MicrosoftLogin.loginFlow(code, false)));
            Launcher.getInstance().saveConfig();

            Launcher.getInstance().getIpcHandler().send("close_microsoft_login", new JsonObject());
        } catch (LoginException e) {
            MicrosoftLogin.log.warn("Fehler beim Microsoft Login", e);

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("error", e.getLocalizedMessage());
            Launcher.getInstance().getIpcHandler().send("microsoft_login_error", jsonObject);
        }
    }

    private static String getRedirectUri() {
        return "http://localhost:" + MicrosoftLogin.oauthCallbackServer.getListeningPort() + "/login_callback";
    }

    public static void requestLogin() throws LoginException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new LoginException("Auf diesem System kann kein Browserfenster erzeugt werden");
        }

        try {
            URI loginUrl = new URIBuilder("https://login.live.com/oauth20_authorize.srf")
                    .addParameter("response_type", "code")
                    .addParameter("client_id", Constants.microsoftLoginClientId)
                    .addParameter("state", MicrosoftLogin.expectedState)
                    .addParameter("redirect_uri", MicrosoftLogin.getRedirectUri())
                    .addParameter("scope", "XboxLive.signin offline_access")
                    .build();

            Desktop.getDesktop().browse(loginUrl);
        } catch (IOException | URISyntaxException e) {
            throw new LoginException("Fehler bei der Abfrage des Authorisierungscodes", e);
        }
    }

    private static IllegalStateException getInvalidStatusException(HttpResponse response) throws IOException {
        return new IllegalStateException(String.format("Ungültiger Statuscode %d: %s", response.getStatusLine().getStatusCode(),
                EntityUtils.toString(response.getEntity())));
    }

    private static OauthResponse getAccessToken(String code, boolean refresh) throws IOException, LoginException {
        HttpResponse response = HttpRequest.post("https://login.live.com/oauth20_token.srf")
                .bodyForm(new BasicNameValuePair("client_id", Constants.microsoftLoginClientId),
                        new BasicNameValuePair(refresh ? "refresh_token" : "code", code),
                        new BasicNameValuePair("grant_type", refresh ? "refresh_token" : "authorization_code"),
                        new BasicNameValuePair("redirect_uri", refresh ? "" : MicrosoftLogin.getRedirectUri()),
                        new BasicNameValuePair("scope", "XboxLive.signin offline_access"))
                .execute()
                .returnResponse();

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new LoginException("Fehler bei der Abfrage des Microsoft Zugriffstoken", MicrosoftLogin.getInvalidStatusException(response));
        }

        try (InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)) {
            return MicrosoftLogin.mapper.readValue(inputStreamReader, OauthResponse.class);
        }
    }

    private static XboxAuthentication doXblAuthenticate(OauthResponse oauthResponse) throws IOException, LoginException {
        JsonObject payload = new JsonObject();
        payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        payload.addProperty("TokenType", "JWT");

        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + oauthResponse.getAccessToken());

        payload.add("Properties", properties);

        HttpResponse response = HttpRequest.post("https://user.auth.xboxlive.com/user/authenticate")
                .bodyString(MicrosoftLogin.gson.toJson(payload), ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new LoginException("Fehler bei der XboxLive Authentifizierung", MicrosoftLogin.getInvalidStatusException(response));
        }

        try (InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)) {
            return MicrosoftLogin.mapper.readValue(inputStreamReader, XboxAuthentication.class);
        }
    }

    private static XboxAuthentication doXstsAuthenticate(XboxAuthentication xblAuthentication) throws IOException, LoginException {
        JsonObject payload = new JsonObject();
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");

        JsonArray userTokens = new JsonArray();
        userTokens.add(xblAuthentication.getToken());

        properties.add("UserTokens", userTokens);

        payload.add("Properties", properties);

        HttpResponse response = HttpRequest.post("https://xsts.auth.xboxlive.com/xsts/authorize")
                .bodyString(MicrosoftLogin.gson.toJson(payload), ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new LoginException("Fehler bei der XSTS Authentifizierung", MicrosoftLogin.getInvalidStatusException(response));
        }

        try (InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)) {
            return MicrosoftLogin.mapper.readValue(inputStreamReader, XboxAuthentication.class);
        }
    }

    private static MinecraftAuthentication doMinecraftXboxAuthenticate(XboxAuthentication xstsAuthentication) throws IOException, LoginException {
        JsonObject payload = new JsonObject();
        payload.addProperty("identityToken", "XBL3.0 x=" + xstsAuthentication.getDisplayClaims().get("xui").get(0).getUserHash()
                + ";" + xstsAuthentication.getToken());

        HttpResponse response = HttpRequest.post("https://api.minecraftservices.com/authentication/login_with_xbox")
                .bodyString(MicrosoftLogin.gson.toJson(payload), ContentType.APPLICATION_JSON)
                .execute()
                .returnResponse();

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new LoginException("Fehler bei der Minecraft-Xbox Authentifizierung", MicrosoftLogin.getInvalidStatusException(response));
        }

        try (InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)) {
            return MicrosoftLogin.mapper.readValue(inputStreamReader, MinecraftAuthentication.class);
        }
    }

    private static MinecraftProfile getMinecraftProfile(MinecraftAuthentication minecraftAuthentication) throws IOException, LoginException {
        HttpResponse response = HttpRequest.get("https://api.minecraftservices.com/minecraft/profile")
                .addHeader("Authorization", "Bearer " + minecraftAuthentication.getAccessToken())
                .execute()
                .returnResponse();

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new LoginException("Fehler bei der Abfrage des Minecraft-Profils", MicrosoftLogin.getInvalidStatusException(response));
        }

        try (InputStreamReader inputStreamReader = new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)) {
            return MicrosoftLogin.mapper.readValue(inputStreamReader, MinecraftProfile.class);
        }
    }

    @Override
    public void refreshLogin(LauncherProfile launcherProfile) throws LoginException {
        launcherProfile.setProperties(MicrosoftLogin.loginFlow((String) launcherProfile.getProperties().get("oauthRefreshToken"), true).getProperties());
    }

    @Override
    public String getAuthToken(LauncherProfile launcherProfile) {
        return (String) launcherProfile.getProperties().get("minecraftAccessToken");
    }

    static {
        try {
            MicrosoftLogin.startOauthCallbackServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
