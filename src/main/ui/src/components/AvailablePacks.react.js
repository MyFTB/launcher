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

export default class AvailablePacks extends React.Component {

    constructor(props) {
        super(props);
        this.state = { 
            packages: false, dialog: false, changeToInstalled: false 
        };

        this.acceptInstallationDialog = this.acceptInstallationDialog.bind(this);
    }

    componentDidMount() {
        window.launcher.loading(true);
        window.launcher.sendIpc('request_installable_modpacks', false, (err, data) => {
            window.launcher.loading(false);
            if (err) {
                return window.launcher.showDialog(true, <p>{err}</p>);
            }
            this.setState({packages: data.packages.sort((a, b) => a.title.localeCompare(b.title))});
        });
    }

    onModpackClick(index) {
        this.installPack(index);
    }

    acceptInstallationDialog() {
        window.launcher.resetDialog();

        if (this.state.changeToInstalled) {   
            this.props.history.push('/modpacks');
        }
    }

    installPack(index, features) {
        let pack = {};
        Object.assign(pack, this.state.packages[index]);
        if (features) {
            pack.selected_features = features;
        }

        window.launcher.loading(true);
        window.launcher.sendIpc('install_modpack', pack, window.launcher.generalFeatureCallback('install_modpack', data => {
            if (data.installing) {
                window.launcher.setState({installationStatus: {progress: data.installing, pack: this.state.packages[index]}});
            } else if (data.installed) {
                let success = data.success;
                window.launcher.setState({installationStatus: false});
                this.setState({status: false, changeToInstalled: success});
                window.launcher.showDialog(false, [
                    <p>{success ? 'Das Modpack ' + this.state.packages[index].title + ' wurde erfolgreich installiert' : 'Bei der Installation von ' + this.state.packages[index].title + ' sind Fehler aufgetreten'}</p>,
                    <button className="btn" onClick={this.acceptInstallationDialog}>OK</button>
                ]);
            }
        }));
    }

    render() {
        return (
            <div>
                <div className="packs">
                    {this.state.packages && (
                        this.state.packages.map((pack, i) => {
                            return <Modpack key={i} pack={pack} onClick={this.onModpackClick.bind(this, i)}></Modpack>
                        })
                    )}
                </div>
            </div>
        )
    }

}