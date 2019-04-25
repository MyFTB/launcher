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
import ProgressBar from './base/ProgressBar.react';

export default class AvailablePacks extends React.Component {

    constructor(props) {
        super(props);
        this.acceptDialog = this.acceptDialog.bind(this);
        this.closeDialog = this.closeDialog.bind(this);
        this.state = { 
            packages: false, dialog: false, status: false, 
            features: false, featuresFor: false, 
            changeToInstalled: false 
        };
    }

    acceptDialog() {
        if (this.state.features) {
            let features = [];
            let featureChecks = document.querySelectorAll('.feature-dialog input[type=checkbox]');
            for (let i = 0; i < featureChecks.length; i++) {
                if (featureChecks[i].checked) {
                    features.push(featureChecks[i].getAttribute('data-feature'));
                }
            }
            this.installPack(this.state.featuresFor, features);
            this.setState({features: false, featuresFor: false});
        } else if (this.state.changeToInstalled) {
            window.launcher.resetDialog();
            this.props.history.push('/modpacks');
        }
    }

    closeDialog() {
        this.setState({features: false, featuresFor: false});
    }

    componentDidMount() {
        window.launcher.loading(true);
        window.launcher.sendIpc('request_installable_modpacks', false, (err, data) => {
            window.launcher.loading(false);
            if (err) {
                return window.launcher.showDialog(true, <p>{err}</p>);
            }
            this.setState({ packages: data.packages.sort((a, b) => a.title.localeCompare(b.title)) });
        });
    }

    onModpackClick(index) {
        this.installPack(index);
    }

    installPack(index, features) {
        let pack = {};
        Object.assign(pack, this.state.packages[index]);
        if (features) {
            pack.selected_features = features;
        }

        window.launcher.loading(true);
        window.launcher.sendIpc('install_modpack', pack, (err, data) => {
            window.launcher.loading(false);
            if (err) {
                return window.launcher.showDialog(true, <p>{err}</p>);
            }

            if (data.features) {
                this.setState({ featuresFor: index, features: JSON.parse(data.features) });
            } else if (data.installing) {
                this.setState({ status: { progress: data.installing, pack: this.state.packages[index] } });
            } else if (data.installed) {
                this.setState({ status: false, changeToInstalled: true });
                window.launcher.showDialog(false, [
                    <p>Das Modpack {this.state.packages[index].title} wurde erfolgreich installiert</p>,
                    <button className="btn" onClick={this.acceptDialog}>OK</button>
                ]);
            }
        });
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

                {this.state.status && (
                    <div className="overlayed-dark">
                        <div className="dialog">
                            <h3>Installiere Modpack: {this.state.status.pack.title}</h3>
                            <p>{this.state.status.progress.total} Aufgaben gesamt, {this.state.status.progress.finished} abgeschlossen, {this.state.status.progress.failed} fehlgeschlagen</p>
                            <ProgressBar data-progress={Math.round(this.state.status.progress.finished / this.state.status.progress.total * 100)}></ProgressBar>
                        </div>
                    </div>
                )}

                {this.state.features && (
                    <div className="overlayed-dark">
                        <div className="dialog feature-dialog">
                            <h3>Optionale Features</h3>
                            <p>Wähle optionale Features aus der unten aufgeführten Liste aus, welche zusätzlich installiert werden sollen</p>
                            {this.state.features.map((feature, i) => {
                                return (
                                    <div key={i} className="feature">
                                        <b>
                                            <label className="toggle">
                                                <input type="checkbox" data-feature={feature.name}></input>
                                                <span></span>
                                            </label>
                                            {feature.name}
                                        </b>
                                        <p>{feature.description}</p>
                                    </div>
                                )
                            })}
                            <button className="btn" onClick={this.acceptDialog}>OK</button>
                            <button className="btn" onClick={this.closeDialog}>Abbrechen</button>
                        </div>
                    </div>
                )}
            </div>
        )
    }

}