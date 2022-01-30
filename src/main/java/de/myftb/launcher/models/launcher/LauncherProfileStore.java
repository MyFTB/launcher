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

package de.myftb.launcher.models.launcher;

import com.google.gson.annotations.Expose;

import de.myftb.launcher.Launcher;
import de.myftb.launcher.login.LoginService;
import de.myftb.launcher.login.microsoft.MicrosoftLogin;
import de.myftb.launcher.login.mojang.MojangLogin;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class LauncherProfileStore {
    @Expose(deserialize = false, serialize = false)
    private static Map<String, LoginService> loginServices = new HashMap<>();

    @Expose private UUID selectedProfile;
    @Expose private Set<LauncherProfile> launcherProfiles;

    public LauncherProfileStore() {
        this.launcherProfiles = new HashSet<>();
    }

    public Set<LauncherProfile> getLauncherProfiles() {
        return this.launcherProfiles;
    }

    public Optional<LauncherProfile> getSelectedProfile() {
        return Optional.ofNullable(this.selectedProfile)
                .flatMap(uuid -> this.launcherProfiles.stream()
                        .filter(profile -> profile.getUuid().equals(uuid))
                        .findFirst());
    }

    public void setSelectedProfile(LauncherProfile launcherProfile) {
        this.selectedProfile = launcherProfile.getUuid();
    }

    public void commit(Collection<LauncherProfile> launcherProfiles) {
        this.launcherProfiles.removeIf(profile -> launcherProfiles.stream().anyMatch(newProfile -> profile.getUuid().equals(newProfile.getUuid())));
        this.launcherProfiles.addAll(launcherProfiles);

        if (!this.getSelectedProfile().isPresent()) {
            this.setSelectedProfile(this.launcherProfiles.iterator().next());
        }

        this.syncUiProfiles();
    }

    public LoginService getLoginService(LauncherProfile launcherProfile) {
        return LauncherProfileStore.loginServices.get(launcherProfile.getProvider());
    }

    public void logoutSelected() {
        if (this.selectedProfile == null) {
            return;
        }

        this.launcherProfiles.removeIf(profile -> profile.getUuid().equals(this.selectedProfile));

        if (!this.launcherProfiles.isEmpty()) {
            this.selectedProfile = this.launcherProfiles.iterator().next().getUuid();
        }

        this.syncUiProfiles();
    }

    public void syncUiProfiles() {
        List<LauncherProfile> sortedProfiles = this.launcherProfiles.stream()
                .sorted(Comparator.comparing(profile -> !profile.getUuid().equals(this.selectedProfile)))
                .collect(Collectors.toList());

        Launcher.getInstance().getIpcHandler().send("update_profiles", sortedProfiles);
    }

    static {
        LauncherProfileStore.loginServices.put(MojangLogin.MOJANG_PROFILE_PROVIDER, new MojangLogin());
        LauncherProfileStore.loginServices.put(MicrosoftLogin.MICROSOFT_PROFILE_PROVIDER, new MicrosoftLogin());
    }

}
