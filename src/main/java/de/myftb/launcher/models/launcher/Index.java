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

package de.myftb.launcher.models.launcher;

import java.util.List;

public class Index {
    private String name;
    private List<IndexObject> objects;

    public String getName() {
        return this.name;
    }

    public List<IndexObject> getObjects() {
        return this.objects;
    }

    public static class IndexObject {
        private String path;
        private String hash;
        private String url;

        public String getPath() {
            return this.path;
        }

        public String getHash() {
            return this.hash;
        }

        public String getUrl() {
            return this.url;
        }

    }

}
