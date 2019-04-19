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

import ToggleSwitch from './base/ToggleSwitch.react';
import RangeInput from './base/RangeInput.react';

export default class Settings extends React.Component {

    constructor(props) {
        super(props);
        this.state = {minMemory: 0, maxMemory: 0, gameWidth: 0, gameHeight: 0, jvmArgs: '', packKey: '', installationDir: '', metricsEnabled: false, loaded: false};
        this.doInstallDirSelection = this.doInstallDirSelection.bind(this);
    }

    componentDidMount() {
        window.launcher.sendIpc('request_settings', false, (err, data) => {
            for (let key in data) {
                if (!(key in this.state)) {
                    delete data[key];
                }
            }
            data.loaded = true;
            this.setState(data);
        });
    }

    componentWillUnmount() {
        let newSettings = {};
        let inputs = document.querySelectorAll('.settings input');
        for (let i = 0; i < inputs.length; i++) {
            let input = inputs[i];
            if (input.type === 'range') {
                newSettings[input.id] = parseInt(input.value);
            } else if (input.type === 'checkbox') {
                newSettings[input.id] = input.checked;
            } else {
                newSettings[input.id] = input.value;
            }
        }
        window.launcher.sendIpc('submit_settings', newSettings);
    }

    doInstallDirSelection() {
        window.launcher.sendIpc('open_directory_browser', false, (err, data) => {
            this.refs.installationDir.value = data.directory;
        });
    }

    render() { // key auf jedem Element um eine Neuinitialisierung nach Stateänderung zu erhalten. Siehe https://stackoverflow.com/a/41717743/6431694
        return (
            <div className="settings">
                <div className="form-group">
                    <p>Minimaler RAM in MB</p>
                    <RangeInput id="minMemory" min="1024" max="16384" key={this.state.loaded ? 'loaded': 'notLoaded'} defaultValue={this.state.minMemory} readOnly={!this.state.loaded}></RangeInput>
                </div>
                <div className="form-group">
                    <p>Maximaler RAM in MB</p>
                    <RangeInput id="maxMemory" min="1024" max="16384" key={this.state.loaded ? 'loaded': 'notLoaded'} defaultValue={this.state.maxMemory} readOnly={!this.state.loaded}></RangeInput>
                </div>
                <div className="form-group">
                    <p>Java Argumente</p>
                    <input id="jvmArgs" type="text" key={this.state.loaded ? 'loaded': 'notLoaded'} defaultValue={this.state.jvmArgs} readOnly={!this.state.loaded}></input>
                </div>
                <div className="form-group">
                    <p>Breite des Spielfensters in Pixel</p>
                    <RangeInput id="gameWidth" min="854" max="3840" key={this.state.loaded ? 'loaded': 'notLoaded'} defaultValue={this.state.gameWidth} readOnly={!this.state.loaded}></RangeInput>
                </div>
                <div className="form-group">
                    <p>Höhe des Spielfensters in Pixel</p>
                    <RangeInput id="gameHeight" min="480" max="2160" key={this.state.loaded ? 'loaded': 'notLoaded'} defaultValue={this.state.gameHeight} readOnly={!this.state.loaded}></RangeInput>
                </div>
                <div className="form-group">
                    <p>Modpackschlüssel</p>
                    <input id="packKey" type="text" key={this.state.loaded ? 'loaded': 'notLoaded'} defaultValue={this.state.packKey} readOnly={!this.state.loaded}></input>
                </div>
                <div className="form-group">
                    <p>Speicherverzeichnis</p>
                    <input id="installationDir" ref="installationDir" type="text" className="dir-chooser" key={this.state.loaded ? 'loaded': 'notLoaded'} defaultValue={this.state.installationDir} readOnly={!this.state.loaded}></input>
                    <button onClick={this.doInstallDirSelection}>...</button>
                </div>
                <div className="form-group">
                    <p>Anonyme Nutzungsdaten</p>
                    <ToggleSwitch id="metricsEnabled" key={this.state.loaded ? 'loaded': 'notLoaded'} defaultChecked={this.state.metricsEnabled} readOnly={!this.state.loaded}></ToggleSwitch>
                </div>
            </div>
        )
    }

}