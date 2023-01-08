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

package de.myftb.launcher.login.mojang;

import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import de.myftb.launcher.Launcher;
import de.myftb.launcher.login.LoginException;
import de.myftb.launcher.login.LoginService;
import de.myftb.launcher.models.launcher.LauncherProfile;

import java.net.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class MojangLogin implements LoginService {
    public static final String MOJANG_PROFILE_PROVIDER = "mojang";

    private static YggdrasilAuthenticationService getAuthService() {
        return new YggdrasilAuthenticationService(Proxy.NO_PROXY, Launcher.getInstance().getConfig().getClientToken());
    }

    public static Collection<LauncherProfile> login(String username, String password) throws LoginException {
        try {
            UserAuthentication userAuthentication = MojangLogin.getAuthService().createUserAuthentication(Agent.MINECRAFT);
            userAuthentication.setUsername(username);
            userAuthentication.setPassword(password);
            userAuthentication.logIn();

            return Arrays.stream(userAuthentication.getAvailableProfiles())
                    .map(gameProfile -> {
                        LauncherProfile launcherProfile = new LauncherProfile();
                        launcherProfile.setProvider(MojangLogin.MOJANG_PROFILE_PROVIDER);
                        launcherProfile.setUuid(gameProfile.getId());
                        launcherProfile.setLastKnownUsername(gameProfile.getName());
                        launcherProfile.setProperties(userAuthentication.saveForStorage());
                        return launcherProfile;
                    })
                    .collect(Collectors.toList());
        } catch (AuthenticationException e) {
            throw new LoginException("Fehler bei der Mojang-Anmeldung", e);
        }
    }

    @Override
    public void refreshLogin(LauncherProfile launcherProfile) throws LoginException {
        try {
            UserAuthentication userAuthentication = MojangLogin.getAuthService().createUserAuthentication(Agent.MINECRAFT);
            userAuthentication.loadFromStorage(launcherProfile.getProperties());
            userAuthentication.logIn();
            launcherProfile.setProperties(userAuthentication.saveForStorage());
            //TODO lastKnownUsername aktualisieren
        } catch (AuthenticationException e) {
            throw new LoginException("Fehler bei der Mojang-Anmeldung", e);
        }
    }

    @Override
    public String getAuthToken(LauncherProfile launcherProfile) {
        return (String) launcherProfile.getProperties().get("accessToken");
    }

}
