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

export default class InstalledPacks extends React.Component {

    constructor(props) {
        super(props);
        this.state = {packages: false};
    }

    componentDidMount() {
        window.launcher.loading(true);
        window.launcher.sendIpc('request_installed_modpacks', false, (err, data) => {
            window.launcher.loading(false);
            if (err) {
                return window.launcher.showDialog(true, <p>{err}</p>);
            }
            this.setState({ packages: JSON.parse(data.packages).sort((a, b) => a.title.localeCompare(b.title)) });
        });
    }

    onModpackClick(index) {
        window.launcher.launchModpack(this.state.packages[index].name);
    }

    render() {
        return (
            <div>
                <div className="packs">
                    {this.state.packages && (
                        this.state.packages.map((pack, i) => {
                            return <Modpack key={i} pack={pack} packinstalled="true" onClick={this.onModpackClick.bind(this, i)}></Modpack>
                        })
                    )}
                </div>
            </div>
        )
    }

}