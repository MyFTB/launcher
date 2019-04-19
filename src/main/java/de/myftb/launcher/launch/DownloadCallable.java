/*
 * MyFTBLauncher
 * Copyright (C) 2019 MyFTB <https://myftb.de>
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
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadCallable implements Callable<File> {
    private static final Logger log = LoggerFactory.getLogger(DownloadCallable.class);
    protected final Downloadable downloadable;

    public DownloadCallable(Downloadable downloadable) {
        this.downloadable = downloadable;
    }

    @Override
    public File call() throws Exception {
        if (this.downloadable.targetFile.isFile() && LaunchHelper.getSha1(this.downloadable.targetFile).equals(this.downloadable.sha1)) {
            DownloadCallable.log.trace("Überspringe Download von " + this.downloadable.url + ", Datei ist bereits aktuell");
            return this.downloadable.targetFile;
        }

        DownloadCallable.log.trace("Lade Datei " + this.downloadable.url + " herunter");

        this.downloadable.targetFile.getParentFile().mkdirs();

        Request.Get(this.downloadable.url)
                .socketTimeout(3000)
                .connectTimeout(3000)
                .execute()
                .saveContent(this.downloadable.targetFile);

        String sha1Sum = LaunchHelper.getSha1(this.downloadable.targetFile);
        if (!sha1Sum.equals(this.downloadable.sha1)) {
            throw new IOException("Ungültige Prüfsumme beim Download von " + this.downloadable.url + ": " + sha1Sum + " erwartet: "
                    + this.downloadable.sha1);
        }

        DownloadCallable.log.info("Datei " + this.downloadable.url + " nach " + this.downloadable.targetFile.getAbsolutePath() + " heruntergeladen");

        return this.downloadable.targetFile;
    }

    public static class Downloadable {
        protected final String url;
        protected final String sha1;
        protected final File targetFile;

        public Downloadable(String url, String sha1, File targetFile) {
            this.url = url;
            this.sha1 = sha1;
            this.targetFile = targetFile;
        }
    }

}