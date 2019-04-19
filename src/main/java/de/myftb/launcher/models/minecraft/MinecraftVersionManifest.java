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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftVersionManifest {
    private String id;
    private String mainClass;
    private String type;

    private Map<String, Download> downloads;
    private Map<String, Logging> logging;
    private List<Library> libraries;

    private AssetIndexReference assetIndex;

    @JsonAlias({"minecraftArguments", "arguments"})
    private Arguments arguments;

    public String getId() {
        return this.id;
    }

    public String getMainClass() {
        return this.mainClass;
    }

    public String getType() {
        return this.type;
    }

    public Map<String, Download> getDownloads() {
        return this.downloads;
    }

    public Map<String, Logging> getLogging() {
        return this.logging;
    }

    public List<Library> getLibraries() {
        return this.libraries;
    }

    public AssetIndexReference getAssetIndex() {
        return this.assetIndex;
    }

    public Arguments getArguments() {
        return this.arguments;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Download {
        private String url;
        private int size;
        private String sha1;

        public String getUrl() {
            return this.url;
        }

        public int getSize() {
            return this.size;
        }

        public String getSha1() {
            return this.sha1;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Logging {
        private String argument;
        private LoggingFile file;

        public String getArgument() {
            return this.argument;
        }

        public LoggingFile getFile() {
            return this.file;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoggingFile {
        private String id;
        private String sha1;
        private int size;
        private String url;

        public String getId() {
            return this.id;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssetIndexReference {
        private String id;
        private String sha1;
        private int size;
        private String url;

        public String getId() {
            return this.id;
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