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

export default class AvailablePacks extends React.Component {

    constructor(props) {
        super(props);
        this.state = { 
            packages: false, dialog: false, changeToInstalled: false, versions: [],
            nameSearch: false, versionSearch: false
        };

        this.acceptInstallationDialog = this.acceptInstallationDialog.bind(this);
        this.onSearch = this.onSearch.bind(this);
    }

    componentDidMount() {
        window.launcher.loading(true);
        window.launcher.sendIpc('request_installable_modpacks', false, (err, data) => {
            window.launcher.loading(false);
            if (err) {
                return window.launcher.showDialog(true, <p>{err}</p>);
            }
            
            this.setState({
                packages: data.packages.sort((a, b) => a.title.localeCompare(b.title)),
                versions: data.packages.map(pack => pack.gameVersion).filter(PackSearch.distinct).sort()
            });
        });
    }

    onModpackClick(pack) {
        this.installPack(pack);
    }

    acceptInstallationDialog() {
        window.launcher.resetDialog();

        if (this.state.changeToInstalled) {   
            this.props.history.push('/modpacks');
        }
    }

    installPack(pack, features) {
        if (features) {
            pack.selected_features = features;
        }

        window.launcher.loading(true);
        window.launcher.sendIpc('install_modpack', pack, window.launcher.generalFeatureCallback('install_modpack', data => {
            if (data.installing) {
                window.launcher.setState({installationStatus: {progress: data.installing, pack: pack}});
            } else if (data.installed) {
                let success = data.success;
                window.launcher.setState({installationStatus: false});
                this.setState({changeToInstalled: success});
                window.launcher.showDialog(false, [
                    <p>{success ? 'Das Modpack ' + pack.title + ' wurde erfolgreich installiert' : 'Bei der Installation von ' + pack.title + ' sind Fehler aufgetreten'}</p>,
                    <button className="btn" onClick={this.acceptInstallationDialog}>OK</button>
                ]);
            }
        }));
    }

    onSearch(name, version) {
        this.setState({nameSearch: name, versionSearch: version});
    }

    packFilter(pack) {
        return (!this.state.nameSearch || pack.title.toLowerCase().includes(this.state.nameSearch)) && (!this.state.versionSearch || pack.gameVersion === this.state.versionSearch);
    }

    render() {
        return (
            <div>
                <PackSearch versions={this.state.versions} searchCallback={this.onSearch}/>

                <div className="packs">
                    {this.state.packages && (
                        this.state.packages.filter(PackSearch.packFilter(this.state)).map((pack, i) => {
                            return <Modpack key={i} pack={pack} onClick={this.onModpackClick.bind(this, pack)}></Modpack>
                        })
                    )}
                </div>
            </div>
        )
    }

}