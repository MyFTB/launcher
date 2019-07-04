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

import Modpack from './base/Modpack.react';
import PackSearch from './base/PackSearch.react';

export default class InstalledPacks extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            packages: false, versions: [], 
            nameSearch: false, versionSearch: false
        };

        this.onSearch = this.onSearch.bind(this);
    }

    componentDidMount() {
        window.installed_packs = this;

        window.launcher.loading(true);
        window.launcher.sendIpc('request_installed_modpacks', false, (err, data) => {
            window.launcher.loading(false);
            if (err) {
                return window.launcher.showDialog(true, <p>{err}</p>);
            }
            let packs = JSON.parse(data.packages);
            this.setState({
                packages: packs.sort((a, b) => a.title.localeCompare(b.title)), 
                versions: packs.map(pack => pack.gameVersion).filter(PackSearch.distinct).sort()
            });
        });
    }

    componentWillUnmount() {
        window.installed_packs = null;
    }

    onModpackClick(index) {
        window.launcher.launchModpack(this.state.packages[index]);
    }

    onSearch(name, version) {
        this.setState({nameSearch: name, versionSearch: version});
    }

    render() {
        return (
            <div>
                <PackSearch versions={this.state.versions} searchCallback={this.onSearch}/>

                <div className="packs">
                    {this.state.packages && (
                        this.state.packages.filter(PackSearch.packFilter(this.state)).map((pack, i) => {
                            return <Modpack key={i} pack={pack} packinstalled="true" onClick={this.onModpackClick.bind(this, i)}></Modpack>
                        })
                    )}
                </div>
            </div>
        )
    }

}