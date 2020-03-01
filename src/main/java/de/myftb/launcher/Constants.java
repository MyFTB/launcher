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

public class Constants {
    public static final String versionManifestListUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String launcherObjects = "http://packs.myftb.de/packs/objects/%s";
    public static final String minecraftResources = "http://resources.download.minecraft.net/%s";

    public static final String packList = "http://packs.myftb.de/packs/packages.php?key=%s";
    public static final String packManifest = "http://packs.myftb.de/packs/%s";
    public static final String pasteTarget = "https://paste.myftb.de";
    public static final String postsApi = "https://myftb.de/api/posts";

    public static final int connectTimeout = 30000;
    public static final int socketTimeout = 90000;

    public static final String[] repositories = new String[] {
            "https://libraries.minecraft.net/",
            "https://repo1.maven.org/maven2/",
            "http://maven.apache.org/",
            "http://packs.myftb.de/packs/libraries/"
    };
}
