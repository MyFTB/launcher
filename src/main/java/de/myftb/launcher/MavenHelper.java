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
package de.myftb.launcher;

public class MavenHelper {

    public static class MavenArtifact {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String classifier;

        public MavenArtifact(String groupId, String artifactId, String version, String classifier) {
            this.groupId = groupId.replace(".", "/");
            this.artifactId = artifactId;
            this.version = version;
            this.classifier = classifier;
        }

        public MavenArtifact(String artifactPath) {
            String[] args = artifactPath.split("[:]");
            this.groupId = args[0].replace(".", "/");
            this.artifactId = args[1];
            this.version = args[2];
            if (args.length > 3) {
                this.classifier = args[3];
            } else {
                this.classifier = null;
            }
        }

        public String getFilePath(String classifier) {
            return String.format("%s/%s/%s/%s-%s%s.jar",
                    this.groupId,
                    this.artifactId,
                    this.version,
                    this.artifactId,
                    this.version,
                    classifier != null ? "-" + classifier : ""
            );
        }

        public String getFilePath() {
            return this.getFilePath(this.classifier);
        }

        public String getArtifactPath(String classifier) {
            return String.format("%s:%s:%s%s", this.groupId, this.artifactId, this.version, classifier != null ? ":" + classifier : "");
        }

        public String getArtifactPath() {
            return this.getArtifactPath(this.classifier);
        }

        @Override
        public String toString() {
            return this.getArtifactPath();
        }

    }

}
