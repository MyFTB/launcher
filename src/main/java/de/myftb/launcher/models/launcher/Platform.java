/*
 * MyFTBLauncher
 * Copyright (C) 2024 MyFTB <https://myftb.de>
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

public enum Platform {
    WINDOWS,
    OSX,
    LINUX,
    UNKNOWN;

    public static Platform getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return Platform.WINDOWS;
        }
        if (osName.contains("mac")) {
            return Platform.OSX;
        }
        if (osName.contains("linux")) {
            return Platform.LINUX;
        }

        return UNKNOWN;
    }

}