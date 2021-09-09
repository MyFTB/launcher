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
            loading: true, dialog: false, dialogCloseable: false, modpackRunning: false,
            featureMessage: false, featureCallback: false, installationStatus: false,
            welcomeMessage: false,

            profiles: false,
            loginForm: false, selectedLoginProvider: false,
            loginDisabled: true, loginError: '', loginListeners: [],
        };
        
        window.launcher = this;

        this.startMicrosoftLogin = this.startMicrosoftLogin.bind(this);
        this.handleLoginInput = this.handleLoginInput.bind(this);
        this.handleLogin = this.handleLogin.bind(this);
        this.handlePasswordKeyDown = this.handlePasswordKeyDown.bind(this);
        this.handleCancelLogin = this.handleCancelLogin.bind(this);
        this.resetDialog = this.resetDialog.bind(this);

        this.acceptFeatureDialog = this.acceptFeatureDialog.bind(this);
        this.closeFeatureDialog = this.closeFeatureDialog.bind(this);

        this.uploadLog = this.uploadLog.bind(this);
        this.killMinecraft = this.killMinecraft.bind(this);

        this.onInstallDialogClick = this.onInstallDialogClick.bind(this);
        this.cancelDownload = this.cancelDownload.bind(this);

        this.listenIpc('update_profiles', (err, data) => {
            if (!err) {
                this.setState({profiles: data});
                this.state.loginListeners.forEach(comp => comp.onUpdateProfiles(data));
            }
        });

        this.listenIpc('show_login_form', (err, data) => {
            this.showLoginForm();
        });

        this.listenIpc('microsoft_login_error', (err, data) => {
            this.setState({selectedLoginProvider: false, loginError: data.error});
        });

        this.listenIpc('close_microsoft_login', (err, data) => {
            if (this.state.loginForm && this.state.selectedLoginProvider === 'microsoft') {
                this.setState({selectedLoginProvider: false, loginForm: false, loginError: ''});
            }
        });

        this.listenIpc('launch_pack', (err, data) => {
            let pack = JSON.parse(data.pack);
            if (data.install) {
                this.loading(true);
                this.sendIpc('install_modpack', pack, this.generalFeatureCallback('install_modpack', data => {
                    if (data.installing) {
                        this.setState({installationStatus: {progress: data.installing, pack: pack}});
                    } else if (data.installed) {
                        let success = data.success;
                        this.setState({installationStatus: false});
                        this.showDialog(false, [
                            <p>{success ? 'Das Modpack ' + pack.title + ' wurde erfolgreich installiert' : 'Bei der Installation von ' + pack.title + ' sind Fehler aufgetreten'}</p>,
                            <button className="btn" onClick={this.onInstallDialogClick(pack, success)}>OK</button>
                        ]);
                    }
                }));
            } else {
                this.launchModpack(pack);
            }
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
                this.showLoginForm(data.login_username, data.new_profile);
            }
        });
    }

    loading(state) {
        this.setState({loading: state});
    }

    onInstallDialogClick(pack, success) {
        return () => {
            this.resetDialog();

            if (success) {
                this.launchModpack(pack);
            }
        }
    }

    cancelDownload() {
        this.sendIpc('cancel_download', {});
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

    registerUpdateProfilesRerender(comp) {
        this.setState(prevState => { return {loginListeners: prevState.loginListeners.concat([comp])} });
        if (this.state.profiles) {
            comp.onUpdateProfiles(this.state.profiles);
        }
    }

    unregisterUpdateProfilesRerender(comp) {
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

    startMicrosoftLogin() {
        this.sendIpc('start_microsoft_login', {}, (err, success) => {
            if (err) {
                return this.setState({loginError: err, loginError: ''});
            }

            this.setState({selectedLoginProvider: 'microsoft'});
        });
    }

    handleLoginInput(e) {
        let disabled = document.getElementById('username').value.trim().length == 0 || document.getElementById('password').value.trim().length == 0
        this.setState({loginDisabled: disabled});
    }

    handleLogin() {
        this.loading(true);
        this.sendIpc('on_mojang_login', {username: document.getElementById('username').value, password: document.getElementById('password').value}, err => {
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

    handleCancelLogin() {
        this.setState({loginForm: false});
    }

    switchProfile(uuid) {
        this.sendIpc('switch_profile', {uuid: uuid});
    }

    logout() {
        this.sendIpc('logout');
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
                this.setState({installationStatus: {progress: data.installing, pack: modpack}});
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

    showLoginForm() {
        this.setState({loginForm: true, loginError: '', selectedLoginProvider: ''})
    }

    render() {
        return (
            <div>
                {this.state.loginForm && (
                    <div className="overlayed-dark">
                        <div className="login-form dialog">
                            <div className="form-group"><b>Bitte melde dich an</b></div>
                            {this.state.loginError && <div className="error-alert">{this.state.loginError}</div>}
                            
                            {!this.state.selectedLoginProvider && (
                                <div>
                                    <div className="form-group">
                                        <button className="btn centered" onClick={this.startMicrosoftLogin}>
                                            &nbsp;Mit Microsoft-Account anmelden
                                        </button>
                                    </div>
                                    <div className="form-group">
                                        <button className="btn centered" onClick={() => this.setState({selectedLoginProvider: 'mojang', loginError: ''})}>
                                            Mit Mojang-Account anmelden
                                        </button>
                                    </div>
                                    <div className="form-group">
                                        <button className="btn centered" onClick={() => this.setState({loginForm: false})}>
                                            Abbrechen
                                        </button>
                                    </div>
                                </div>
                            )}

                            {this.state.selectedLoginProvider === 'microsoft' && (
                                <div>
                                    <p>Bitte schließe den Anmeldevorgang in deinem Browser ab</p>

                                    <div className="form-group">
                                        <button className="btn" onClick={() => this.setState({selectedLoginProvider: false})}>Zurück</button>
                                    </div>
                                </div>
                            )}

                            {this.state.selectedLoginProvider === 'mojang' && (
                                <div>
                                    <div className="form-group">
                                        <p>Benutzername / Email</p>
                                        <input type="text" id="username" spellCheck="false"></input>
                                    </div>
                                    <div className="form-group">
                                        <p>Passwort</p>
                                        <input type="password" id="password"></input>
                                    </div>

                                    <div className="form-group">
                                        <button className="btn login-btn" onClick={this.handleLogin}>Anmelden</button>
                                        <button className="btn" onClick={() => this.setState({selectedLoginProvider: false})}>Zurück</button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                )}

                <div className={'console-breakout' + (!this.state.consoleOpen ? ' active': '')}>
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
                            <button className="btn" onClick={this.cancelDownload}>Abbrechen</button>
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