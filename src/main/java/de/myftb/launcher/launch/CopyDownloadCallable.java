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

package de.myftb.launcher.launch;

import java.io.File;
import java.nio.file.Files;

public class CopyDownloadCallable extends DownloadCallable {
    private final File copy;

    public CopyDownloadCallable(Downloadable downloadable, File copy) {
        super(downloadable);
        this.copy = copy;
    }

    @Override
    public File call() throws Exception {
        File targetFile = super.call();

        Files.copy(targetFile.toPath(), this.copy.toPath());
        Files.write(new File(this.copy.getAbsolutePath() + ".sha1").toPath(),
                this.downloadable.sha1.getBytes());

        return targetFile;
    }

}