/*
 * MyFTBLauncher
 * Copyright (C) 2021 MyFTB <https://myftb.de>
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
import de.myftb.launcher.MavenHelper;
import de.myftb.launcher.launch.DownloadCallable;
import de.myftb.launcher.launch.MavenDownloadCallable;
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
    private String url;

    public String getName() {
        return this.name;
    }

    public String getArtifactGroup() {
        return this.name.split("[:]")[0];
    }

    public String getArtifactName() {
        return this.name.split("[:]")[1];
    }

    public String getArtifactVersion() {
        return this.name.split("[:]")[2];
    }

    public void setDownloads(Downloads downloads) {
        this.downloads = downloads;
    }

    public Downloads getDownloads() {
        return this.downloads;
    }

    public Map<String, String> getNatives() {
        return this.natives;
    }

    public void setNatives(Map<String, String> natives) {
        this.natives = natives;
    }

    public Map<String, List<String>> getExtract() {
        return this.extract;
    }

    public List<Rule> getRules() {
        return this.rules;
    }

    public boolean isAllowed() {
        if (this.rules == null) {
            return true;
        }

        return this.rules.stream().anyMatch(Rule::matches);
    }

    public List<DownloadCallable> getLibraryDownloads() {
        List<DownloadCallable> downloads = new ArrayList<>();

        if (this.downloads != null) {
            if (this.downloads.artifact != null && (this.downloads.artifact.url != null && !this.downloads.artifact.url.isEmpty())) {
                downloads.add(new DownloadCallable(new DownloadCallable.Downloadable(this.downloads.artifact.url,
                        this.downloads.artifact.sha1,
                        new File(Launcher.getInstance().getSaveSubDirectory("libraries"),
                                this.downloads.artifact.path))));
            }

            if (this.downloads.classifiers != null && this.natives != null && this.natives.containsKey(Platform.getPlatform().name().toLowerCase())) {
                String classifier = this.natives.get(Platform.getPlatform().name().toLowerCase())
                        .replace("${arch}", System.getProperty("os.arch").contains("64") ? "64" : "32");
                if (this.downloads.classifiers.containsKey(classifier)) {
                    DownloadInfo classifierDownload = this.downloads.classifiers.get(classifier);
                    downloads.add(new DownloadCallable(new DownloadCallable.Downloadable(classifierDownload.url,
                            classifierDownload.sha1,
                            new File(Launcher.getInstance().getSaveSubDirectory("libraries"),
                                    classifierDownload.path))));
                }
            }

        } else if (this.url != null) {
            downloads.add(new MavenDownloadCallable(this.url, this.name, new File(Launcher.getInstance().getSaveSubDirectory("libraries"),
                    this.getPath(null))));
        } else {
            downloads.add(new MavenDownloadCallable(this.name, new File(Launcher.getInstance().getSaveSubDirectory("libraries"),
                    this.getPath(null))));
        }

        return downloads;
    }

    public String getPath(String classifier) {
        return new MavenHelper.MavenArtifact(this.name).getFilePath(classifier);
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

        public void setArtifact(DownloadInfo artifact) {
            this.artifact = artifact;
        }

        public Map<String, DownloadInfo> getClassifiers() {
            return this.classifiers;
        }

        public void setClassifiers(Map<String, DownloadInfo> classifiers) {
            this.classifiers = classifiers;
        }

        @Override
        public String toString() {
            return "Downloads{"
                    + "artifact=" + artifact
                    + ", classifiers=" + classifiers
                    + '}';
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

        @Override
        public String toString() {
            return "DownloadInfo{"
                    + "path='" + path + '\''
                    + ", sha1='" + sha1 + '\''
                    + ", size=" + size
                    + ", url='" + url + '\''
                    + '}';
        }

    }

}
