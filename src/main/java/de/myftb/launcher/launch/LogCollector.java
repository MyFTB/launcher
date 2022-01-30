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
package de.myftb.launcher.launch;

import de.myftb.launcher.Launcher;

import java.nio.charset.StandardCharsets;

public class LogCollector {
    private static final int maxSize = 2000000;

    private final byte[] log = new byte[LogCollector.maxSize];
    private final byte[] logBuffer = new byte[LogCollector.maxSize];
    private int length = 0;

    public void log(String str) {
        byte[] logBytes = str.getBytes(StandardCharsets.UTF_8);
        if ((this.length + logBytes.length) >= LogCollector.maxSize) {
            System.arraycopy(this.log, logBytes.length, this.logBuffer, 0, LogCollector.maxSize - logBytes.length);
            System.arraycopy(this.logBuffer, 0, this.log, 0, LogCollector.maxSize - logBytes.length);
            System.arraycopy(logBytes, 0, this.log, LogCollector.maxSize - logBytes.length, logBytes.length);
        } else {
            System.arraycopy(logBytes, 0, this.log, this.length, logBytes.length);
        }

        this.length = Math.min(LogCollector.maxSize, this.length + logBytes.length);
        Launcher.getInstance().getIpcHandler().sendString("console_data", str);
    }

    public String getLog() {
        return new String(this.log, 0, this.length, StandardCharsets.UTF_8);
    }

    public void clear() {
        this.length = 0;
    }

}
