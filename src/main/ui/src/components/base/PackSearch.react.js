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

import React from 'react';

export default class PackSearch extends React.Component {

    constructor(props) {
        super(props);
        this.onSearchChanged = this.onSearchChanged.bind(this);
    }

    onSearchChanged() {
        let search = this.refs.search.value.trim().toLowerCase();
        if (search === '') {
            search = false;
        }

        let version = this.refs.version.value;
        if (version === 'all') {
            version = false;
        }

        this.props.searchCallback(search, version);
    }

    static distinct(value, index, self) {
        return self.indexOf(value) === index;
    }

    static packFilter(state) {
        return pack => (!state.nameSearch || pack.title.toLowerCase().includes(state.nameSearch)) && (!state.versionSearch || pack.gameVersion === state.versionSearch);
    }

    render() {
        return (
            <div className="clearfix pack-search-bar">
                <div className="pack-search">
                    <input type="text" placeholder="Suche" ref="search" onChange={this.onSearchChanged} spellCheck="false"></input>
                    <select ref="version" onChange={this.onSearchChanged}>
                        <option value="all">Alle</option>
                        {this.props.versions.map((version, i) => {
                            return <option key={i}>{version}</option>
                        })}
                    </select>
                </div>
            </div>
        )
    }

}