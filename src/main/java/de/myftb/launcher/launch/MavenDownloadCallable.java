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

import de.myftb.launcher.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import de.myftb.launcher.HttpRequest;
import de.myftb.launcher.MavenHelper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenDownloadCallable extends DownloadCallable {
    private static final Logger log = LoggerFactory.getLogger(MavenDownloadCallable.class);

    private final String repoUrl;

    public MavenDownloadCallable(String repoUrl, String artifact, File targetFile) {
        super(new Downloadable(artifact, null, targetFile));
        this.repoUrl = repoUrl;
    }

    public MavenDownloadCallable(String artifact, File targetFile) {
        this(null, artifact, targetFile);
    }

    @Override
    public File call() throws Exception {
        String path = new MavenHelper.MavenArtifact(this.downloadable.url).getFilePath();

        if (this.repoUrl != null) {
            try {
                return this.tryRepository(this.repoUrl, path);
            } catch (IOException e) {
                if (!(e instanceof HttpResponseException) || ((HttpResponseException) e).getStatusCode() != 404) {
                    MavenDownloadCallable.log.warn("Fehler beim Herunterladen von Maven-Artefakt: " + this.downloadable.url, e);
                }
            }
        }

        for (String repository : Constants.repositories) {
            try {
                return this.tryRepository(repository, path);
            } catch (IOException e) {
                if (!(e instanceof HttpResponseException) || ((HttpResponseException) e).getStatusCode() != 404) {
                    MavenDownloadCallable.log.warn("Fehler beim Herunterladen von Maven-Artefakt: " + this.downloadable.url, e);
                }
            }
        }

        throw new FileNotFoundException("Maven-Artifact " + this.downloadable.url + " nicht gefunden");
    }

    private File tryRepository(String repository, String path) throws IOException {
        HttpResponse sha1SumResponse = HttpRequest.get(repository + path + ".sha1")
                .execute()
                .returnResponse();

        if (sha1SumResponse.getStatusLine().getStatusCode() != 200) {
            throw new HttpResponseException(sha1SumResponse.getStatusLine().getStatusCode(), sha1SumResponse.getStatusLine().getReasonPhrase());
        }

        String sha1Sum = EntityUtils.toString(sha1SumResponse.getEntity(), StandardCharsets.UTF_8).trim();

        if (this.downloadable.targetFile.isFile() && LaunchHelper.getSha1(this.downloadable.targetFile).equals(sha1Sum)) {
            MavenDownloadCallable.log.trace("Überspringe Download von " + this.downloadable.url + ", Datei ist bereits aktuell");
            return this.downloadable.targetFile;
        }

        MavenDownloadCallable.log.trace("Lade Maven-Artifact " + this.downloadable.url + " herunter");

        this.downloadable.targetFile.getParentFile().mkdirs();

        HttpRequest.get(repository + path)
                .execute()
                .saveContent(this.downloadable.targetFile);

        String fileSum = LaunchHelper.getSha1(this.downloadable.targetFile);
        if (!sha1Sum.equals(fileSum)) {
            throw new IOException("Ungültige Prüfsumme beim Download von " + this.downloadable.url + ": " + fileSum + " erwartet: " + sha1Sum);
        }

        Files.write(new File(this.downloadable.targetFile.getAbsolutePath() + ".sha1").toPath(), sha1Sum.getBytes());
        MavenDownloadCallable.log.info("Datei " + this.downloadable.url + " nach " + this.downloadable.targetFile.getAbsolutePath()
                + " heruntergeladen");

        return this.downloadable.targetFile;
    }

}