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
package de.myftb.launcher.launch;

import de.myftb.launcher.HttpRequest;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadCallable implements Callable<File> {
    private static final Logger log = LoggerFactory.getLogger(DownloadCallable.class);
    protected final Downloadable downloadable;
    private final boolean onlyCheckExistance;

    public DownloadCallable(Downloadable downloadable, boolean onlyCheckExistance) {
        this.downloadable = downloadable;
        this.onlyCheckExistance = onlyCheckExistance;
    }

    public DownloadCallable(Downloadable downloadable) {
        this(downloadable, false);
    }

    @Override
    public File call() throws Exception {
        if (this.downloadable.targetFile.isFile()
                && (this.onlyCheckExistance || this.downloadable.hashFn.getFileHash(this.downloadable.targetFile).equals(this.downloadable.hash))) {
            DownloadCallable.log.trace("Überspringe Download von " + this.downloadable.url + ", Datei ist bereits aktuell");
            return this.downloadable.targetFile;
        }

        DownloadCallable.log.trace("Lade Datei " + this.downloadable.url + " herunter");

        this.downloadable.targetFile.getParentFile().mkdirs();

        HttpRequest.get(this.downloadable.url)
                .execute()
                .saveContent(this.downloadable.targetFile);

        String hash = this.downloadable.hashFn.getFileHash(this.downloadable.targetFile);
        if (!this.onlyCheckExistance && !hash.equals(this.downloadable.hash)) {
            throw new IOException("Ungültige Prüfsumme beim Download von " + this.downloadable.url + ": " + hash + " erwartet: "
                    + this.downloadable.hash);
        }

        DownloadCallable.log.info("Datei " + this.downloadable.url + " nach " + this.downloadable.targetFile.getAbsolutePath() + " heruntergeladen");

        return this.downloadable.targetFile;
    }

    public static class Downloadable {
        protected final String url;
        protected final LaunchHelper.FileHashFunction hashFn;
        protected final String hash;
        protected final File targetFile;

        public Downloadable(String url, LaunchHelper.FileHashFunction hashFn, String hash, File targetFile) {
            this.url = url;
            this.hashFn = hashFn;
            this.hash = hash;
            this.targetFile = targetFile;
        }

        public Downloadable(String url, String hash, File targetFile) {
            this(url, LaunchHelper::getSha1, hash, targetFile);
        }

    }

}