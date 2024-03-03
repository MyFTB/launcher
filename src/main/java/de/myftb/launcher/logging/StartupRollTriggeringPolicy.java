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
package de.myftb.launcher.logging;

import java.io.File;

import ch.qos.logback.core.rolling.TriggeringPolicyBase;

public class StartupRollTriggeringPolicy<T> extends TriggeringPolicyBase<T> {
    private static boolean rolled = false;

    @Override
    public boolean isTriggeringEvent(File activeFile, T event) {
        if (!StartupRollTriggeringPolicy.rolled) {
            StartupRollTriggeringPolicy.rolled = true;
            if (activeFile.length() == 0) {
                return false;
            }

            return true;
        }

        return false;
    }

}
