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
import { NavLink, withRouter } from "react-router-dom";

import Loading from './components/Loading.react';
import ProgressBar from './components/base/ProgressBar.react';

import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTerminal, faUpload, faSkull } from '@fortawesome/free-solid-svg-icons'

library.add(faTerminal);
library.add(faUpload);
library.add(faSkull);

class Launcher extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            loading: true, loginForm: false, loginFormPrefill: false, loginDisabled: true, loginError: '', 
            profile: false, dialog: false, dialogCloseable: false, loginListeners: [], modpackRunning: false,
            featureMessage: false, featureCallback: false, installationStatus: false,
            welcomeMessage: false
        };
        
        window.launcher = this;

        this.handleLoginInput = this.handleLoginInput.bind(this);
        this.handleLogin = this.handleLogin.bind(this);
        this.handlePasswordKeyDown = this.handlePasswordKeyDown.bind(this);
        this.resetDialog = this.resetDialog.bind(this);

        this.acceptFeatureDialog = this.acceptFeatureDialog.bind(this);
        this.closeFeatureDialog = this.closeFeatureDialog.bind(this);

        this.uploadLog = this.uploadLog.bind(this);
        this.killMinecraft = this.killMinecraft.bind(this);

        this.listenIpc('logged_in', (err, data) => {
            if (!err) {
                this.setState({profile: data});
                this.state.loginListeners.forEach(comp => comp.onLogin(data));
            }
        });

        this.listenIpc('show_login_form', (err, data) => {
            this.setState({loginForm: true, loginFormPrefill: data.username});
        });

        this.listenIpc('launch_pack', (err, data) => {
            this.launchModpack(data);
        });

        this.listenIpc('welcome_message', (err, data) => {
            this.showDialog(false, [
                <h3>Herzlich Willkommen!</h3>,
                <p>Es scheint so, als hättest du den Launcher gerade zum ersten Mal gestartet. </p>,
                <p>Zu Beginn solltest du in den <b>Einstellungen</b> einige Dinge konfigurieren.</p>,
                <br></br>,
                <p>Dort kannst du unter anderem auch den Installationsort für Modpacks festlegen. Dieser ist aktuell auf <b>{data.installation_dir}</b> festgelegt.</p>,
                <NavLink to="/settings" className="btn" onClick={this.resetDialog}>Zu den Einstellungen</NavLink>
            ]);
        });

        this.sendIpc('renderer_arrived', false, (err, data) => {
            if (err) {
                throw err;
            }
            this.loading(false);
            if (data.login_needed) {
                this.setState({loginForm: true, loginFormPrefill: data.login_username});
            }
        });
    }

    loading(state) {
        this.setState({loading: state});
    }

    /* ============================================================ Dialog ============================================================ */

    showDialog(closeable, components) {
        if (!components) {
            return this.setState({dialogCloseable: false, dialog: false});
        }

        if (!Array.isArray(components)) {
            components = [components];
        }
        this.setState({dialogCloseable: closeable, dialog: components});
    }

    resetDialog() {
        this.showDialog(false);
    }

    /* ============================================================ Login Rerender ============================================================ */

    registerLoginRerender(comp) {
        this.setState(prevState => { return {loginListeners: prevState.loginListeners.concat([comp])} });
        if (this.state.profile) {
            comp.onLogin(this.state.profile);
        }
    }

    unregisterLoginRerender(comp) {
        this.setState({ loginListeners: this.state.loginListeners.filter(value => value !== comp) });
    }

    /* ============================================================ IPC ============================================================ */

    sendIpc(topic, data, cb) {
        let request = {request: topic, persistent: true};
        if (data) {
            request.request += ':' + JSON.stringify(data);
        }
        if (cb) {
            request.onSuccess = data => cb(false, JSON.parse(data));
            request.onFailure = (code, message) => cb(message);
        }
        window.ipcQuery(request);
    }

    listenIpc(topic, cb) {
        window.ipcQuery({request: topic + ':register', persistent: true,
            onSuccess: data => cb(false, JSON.parse(data)), 
            onFailure: (code, message) => cb(message)
        });
    }

    listenIpcRaw(topic, cb) {
        window.ipcQuery({request: topic + ':register', persistent: true,
            onSuccess: data => cb(false, data), 
            onFailure: (code, message) => cb(message)
        });
    }

    unregisterIpc(topic) {
        window.ipcQuery({request: topic + ':unregister'});
    }

    /* ============================================================ Login ============================================================ */

    handleLoginInput(e) {
        let disabled = document.getElementById('username').value.trim().length == 0 || document.getElementById('password').value.trim().length == 0
        this.setState({loginDisabled: disabled});
    }

    handleLogin() {
        this.loading(true);
        this.sendIpc('mc_login', {username: document.getElementById('username').value, password: document.getElementById('password').value}, (err, data) => {
            this.loading(false);
            if (err) {
                this.setState({loginError: err});
                return;
            }

            this.setState({loginForm: false});
        });
    }

    handlePasswordKeyDown(e) {
        if (e.keyCode === 13 && document.getElementById('username').value.trim().length > 0 && document.getElementById('password').value.trim().length > 0) {
            this.handleLogin();
        }
    }

    logout() {
        this.sendIpc('logout');
        this.state.loginListeners.forEach(comp => comp.onLogin(false));
    }

    /* ============================================================ Features ============================================================ */

    generalFeatureCallback(ipcChannel, cb) {
        let ipcCb = (err, data) => {
            this.loading(false);
            if (err) {
                return this.showDialog(true, <p>{err}</p>);
            }

            if (data.features) {
                data.features = JSON.parse(data.features);
                data.ipc_channel = ipcChannel;
                return this.setState({featureMessage: data});
            }

            return cb(data);
        };

        this.setState({featureCallback: ipcCb});

        return ipcCb;
    }

    acceptFeatureDialog() {
        let features = [];
        let featureChecks = document.querySelectorAll('.feature-dialog input[type=checkbox]');
        for (let i = 0; i < featureChecks.length; i++) {
            if (featureChecks[i].checked) {
                features.push(featureChecks[i].getAttribute('data-feature'));
            }
        }

        let featureMessage = this.state.featureMessage;
        delete featureMessage[features];
        featureMessage.selected_features = features;
        this.setState({featureMessage: false});
        this.sendIpc(featureMessage.ipc_channel, featureMessage, this.state.featureCallback);
    }

    closeFeatureDialog() {
        this.setState({featureMessage: false, featureCallback: false});
    }

    launchModpack(modpack) {
        this.loading(true);

        this.sendIpc('launch_modpack', {modpack: modpack.name}, this.generalFeatureCallback('launch_modpack', data => {
            if (data.installing) {
                this.setState({installationStatus: {progress: data.installing, pack: modpack.title}});
            } else if (data.installed) {
                this.setState({installationStatus: false});
            } else if (data.launching) {
                this.setState({modpackRunning: true});
            } else if (data.closed) {
                this.setState({modpackRunning: false});
            }
        }));
    }

    static getDerivedStateFromProps(props, state) {
        state.consoleOpen = props.history.location.pathname === '/console';
        return state;
    }

    uploadLog() {
        this.loading(true);
        this.sendIpc('upload_log', false, (err, data) => {
            this.loading(false);
            if (err) {
                return this.showDialog(true, <p>{err}</p>);
            }
        });
    }

    killMinecraft() {
        this.sendIpc('kill_minecraft', false);
    }

    render() {
        return (
            <div>
                {this.state.loginForm && (
                    <div className="overlayed-dark">
                        <div className="login-form dialog">
                            <div className="form-group"><b>Bitte melde dich mit deinem Minecraft-Account an</b></div>
                            {this.state.loginError && <div className="error-alert">{this.state.loginError}</div>}
                            <div className="form-group">
                                <p>Benutzername / Email</p>
                                <input type="text" id="username" key={this.state.loginFormPrefill ? 'prefilled' : 'empty'} onInput={this.handleLoginInput} defaultValue={this.state.loginFormPrefill}></input>
                            </div>
                            <div className="form-group">
                                <p>Passwort</p>
                                <input type="password" id="password" onInput={this.handleLoginInput} onKeyDown={this.handlePasswordKeyDown}></input>
                            </div>
                            <div className="form-group">
                                <button className="btn login-btn" onClick={this.handleLogin} disabled={this.state.loginDisabled}>Anmelden</button>
                            </div>
                        </div>
                    </div>
                )}

                <div className={'console-breakout' + (this.state.modpackRunning && !this.state.consoleOpen ? ' active': '')}>
                    <NavLink to="/console" activeClassName="active"><FontAwesomeIcon icon="terminal"/></NavLink>
                </div>

                <div className={'console-breakout' + (this.state.consoleOpen ? ' active': '')}>
                    <a href="#" title="Log hochladen" onClick={this.uploadLog}><FontAwesomeIcon icon="upload"/></a>
                </div>

                <div className={'console-breakout' + (this.state.modpackRunning && this.state.consoleOpen ? ' active': '')} style={{bottom: '81px'}}>
                    <a href="#" title="Minecraft töten" onClick={this.killMinecraft}><FontAwesomeIcon icon="skull"/></a>
                </div>

                {this.state.loading && <Loading />}

                {this.state.dialog && (
                    <div className="overlayed-dark">
                        <div className="dialog">
                            {this.state.dialog.map((dialogChild, i) => React.cloneElement(dialogChild, {key: i}))}
                            {this.state.dialogCloseable && (<button className="btn" onClick={this.resetDialog}>OK</button>)}
                        </div>
                    </div>
                )}

                {this.state.installationStatus && (
                    <div className="overlayed-dark">
                        <div className="dialog">
                            <h3>Installiere Modpack: {this.state.installationStatus.pack.title}</h3>
                            <p>{this.state.installationStatus.progress.total} Aufgaben gesamt, {this.state.installationStatus.progress.finished} abgeschlossen, {this.state.installationStatus.progress.failed} fehlgeschlagen</p>
                            <ProgressBar data-progress={Math.round(this.state.installationStatus.progress.finished / this.state.installationStatus.progress.total * 100)}></ProgressBar>
                        </div>
                    </div>
                )}

                {this.state.featureMessage && (
                    <div className="overlayed-dark">
                        <div className="dialog feature-dialog">
                            <h3>Optionale Features</h3>
                            <p>Wähle optionale Features aus der unten aufgeführten Liste aus, welche zusätzlich installiert werden sollen</p>
                            {this.state.featureMessage.features.map((feature, i) => {
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
                            <button className="btn" onClick={this.acceptFeatureDialog}>OK</button>
                            <button className="btn" onClick={this.closeFeatureDialog}>Abbrechen</button>
                        </div>
                    </div>
                )}
            </div>
        );
    }

}

export default withRouter(Launcher);