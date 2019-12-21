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

import RangeInput from './base/RangeInput.react';
import ToggleSwitch from './base/ToggleSwitch.react';
import AutoConfig from './base/AutoConfig.react';

export default class Settings extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            minMemory: 0, maxMemory: 0, gameWidth: 0, gameHeight: 0, jvmArgs: '', packKey: '', installationDir: '', metricsEnabled: false, allowWebstart: false, loaded: false, 
            autoConfigOptions: {configs:[], types: [], constraints: []}
        };
        this.doInstallDirSelection = this.doInstallDirSelection.bind(this);

        this.onMinMemoryChange = this.onMinMemoryChange.bind(this);
        this.onMaxMemoryChange = this.onMaxMemoryChange.bind(this);
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

            window.launcher.sendIpc('request_autoconfigs', false, (err, data) => {
                this.setState({
                    autoConfigOptions: {
                        configs: data.configs.sort((a, b) => a.name.localeCompare(b.name)),
                        types: data.types,
                        constraints: data.constraints
                    }
                });
            });
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

    onMinMemoryChange() {
        let min = this.refs.minMemory;
        let max = this.refs.maxMemory;
        if (min.getValue() > max.getValue()) {
            max.setValue(min.getValue());
        }
    }

    onMaxMemoryChange() {
        let min = this.refs.minMemory;
        let max = this.refs.maxMemory;
        if (max.getValue() < min.getValue()) {
            min.setValue(max.getValue());
        }
    }

    doInstallDirSelection() {
        window.launcher.sendIpc('open_directory_browser', false, (err, data) => {
            this.refs.installationDir.value = data.directory;
        });
    }

    getOptionAttributes(name) {
        return {
            id: name,
            key: this.state.loaded ? 'loaded' : 'notLoaded',
            defaultValue: this.state[name],
            readOnly: !this.state.loaded
        }
    }

    render() { // key auf jedem Element um eine Neuinitialisierung nach Stateänderung zu erhalten. Siehe https://stackoverflow.com/a/41717743/6431694
        return (
            <div className="settings">
                <div className="form-group">
                    <p>Minimaler RAM in MB</p>
                    <RangeInput {...this.getOptionAttributes('minMemory')} min="1024" max="16384" ref="minMemory" onChange={this.onMinMemoryChange}></RangeInput>
                </div>
                <div className="form-group">
                    <p>Maximaler RAM in MB</p>
                    <RangeInput {...this.getOptionAttributes('maxMemory')} min="1024" max="16384" ref="maxMemory" onChange={this.onMaxMemoryChange}></RangeInput>
                </div>
                <div className="form-group">
                    <p>Java Argumente</p>
                    <input {...this.getOptionAttributes('jvmArgs')} type="text"></input>
                </div>
                <div className="form-group">
                    <p>Breite des Spielfensters in Pixel</p>
                    <RangeInput {...this.getOptionAttributes('gameWidth')} min="854" max="3840"></RangeInput>
                </div>
                <div className="form-group">
                    <p>Höhe des Spielfensters in Pixel</p>
                    <RangeInput {...this.getOptionAttributes('gameHeight')} min="480" max="2160"></RangeInput>
                </div>
                <div className="form-group">
                    <p>Modpackschlüssel</p>
                    <input {...this.getOptionAttributes('packKey')} type="text"></input>
                </div>
                <div className="form-group">
                    <p>Benutzerdefiniertes Speicherverzeichnis</p>
                    <input {...this.getOptionAttributes('installationDir')} ref="installationDir" type="text" className="dir-chooser"></input>
                    <button onClick={this.doInstallDirSelection}>...</button>
                </div>
                <div className="form-group">
                    <p>Webstart aktivieren</p>
                    <ToggleSwitch {...this.getOptionAttributes('allowWebstart')} defaultChecked={this.state.allowWebstart}></ToggleSwitch>
                </div>
            </div>
        )
    }

}