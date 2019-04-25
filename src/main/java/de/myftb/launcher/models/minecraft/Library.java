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

package de.myftb.launcher.models.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.myftb.launcher.Launcher;
import de.myftb.launcher.launch.DownloadCallable;
import de.myftb.launcher.models.launcher.Platform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Library {
    private String name;
    private Downloads downloads;
    private Map<String, String> natives;
    private Map<String, List<String>> extract;
    private List<Rule> rules;

    public String getName() {
        return this.name;
    }

    public Downloads getDownloads() {
        return this.downloads;
    }

    public Map<String, String> getNatives() {
        return this.natives;
    }

    public Map<String, List<String>> getExtract() {
        return this.extract;
    }

    public List<Rule> getRules() {
        return this.rules;
    }

    public List<DownloadCallable.Downloadable> getDownloadables() {
        List<DownloadCallable.Downloadable> downloadables = new ArrayList<>();

        if (this.downloads != null) {
            if (this.downloads.artifact != null) {
                downloadables.add(new DownloadCallable.Downloadable(this.downloads.artifact.url,
                        this.downloads.artifact.sha1,
                        new File(Launcher.getInstance().getSaveSubDirectory("libraries"),
                                this.downloads.artifact.path)));
            }

            if (this.downloads.classifiers != null && this.natives != null && this.natives.containsKey(Platform.getPlatform().name().toLowerCase())) {
                String classifier = this.natives.get(Platform.getPlatform().name().toLowerCase())
                        .replace("${arch}", System.getProperty("os.arch").contains("64") ? "64" : "32");
                if (this.downloads.classifiers.containsKey(classifier)) {
                    DownloadInfo classifierDownload = this.downloads.classifiers.get(classifier);
                    downloadables.add(new DownloadCallable.Downloadable(classifierDownload.url,
                            classifierDownload.sha1,
                            new File(Launcher.getInstance().getSaveSubDirectory("libraries"),
                                    classifierDownload.path)));
                }
            }
        }

        return downloadables;
    }

    public String getPath(String classifier) {
        String[] args = this.name.split("[:]");

        return String.format("%s/%s/%s/%s-%s.jar",
                args[0].replace(".", "/"), // Classifier
                args[1], // Artifact Id
                args[2], // Version
                args[1], // Artifact Id
                args[2] + (classifier != null ? "-" + classifier : "")
        );
    }

    public boolean isExtractionAllowed(String name) {
        if (this.extract != null && this.extract.containsKey("exclude")) {
            List<String> excludes = this.extract.get("exclude");
            for (String exclude : excludes) {
                if (name.startsWith(exclude)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static class Downloads {
        private DownloadInfo artifact;
        private Map<String, DownloadInfo> classifiers;

        public DownloadInfo getArtifact() {
            return this.artifact;
        }

        public Map<String, DownloadInfo> getClassifiers() {
            return this.classifiers;
        }
    }

    public static class DownloadInfo {
        private String path;
        private String sha1;
        private int size;
        private String url;

        public String getPath() {
            return this.path;
        }

        public String getSha1() {
            return this.sha1;
        }

        public int getSize() {
            return this.size;
        }

        public String getUrl() {
            return this.url;
        }
    }

}
