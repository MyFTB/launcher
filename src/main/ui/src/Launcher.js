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

import Loading from './components/Loading.react';

export default class Launcher extends React.Component {

    constructor(props) {
        super(props);
        this.state = {loading: true, loginForm: false, loginFormPrefill: false, loginDisabled: true, loginError: '', profile: false, dialog: false, dialogCloseable: false, loginListeners: []};
        window.launcher = this;

        this.handleLoginInput = this.handleLoginInput.bind(this);
        this.handleLogin = this.handleLogin.bind(this);
        this.resetDialog = this.resetDialog.bind(this);

        this.listenIpc('logged_in', (err, data) => {
            if (!err) {
                this.setState({profile: data});
                this.state.loginListeners.forEach(comp => comp.onLogin(data));
            }
        });

        this.listenIpc('show_login_form', (err, data) => {
            this.setState({loginForm: true, loginFormPrefill: data.username});
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

    registerLoginRerender(comp) {
        this.setState(prevState => { return {loginListeners: prevState.loginListeners.concat([comp])} });
        if (this.state.profile) {
            comp.onLogin(this.state.profile);
        }
    }

    unregisterLoginRerender(comp) {
        this.setState({ loginListeners: this.state.loginListeners.filter(value => value !== comp) });
    }

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

    render() {
        return (
            <div>
                {this.state.loginForm && (
                    <div className="overlayed-dark">
                        <div className="login-form dialog">
                            <div className="form-group"><b>Bitte melde dich mit deinem Minecraft-Account an</b></div>
                            {this.state.loginError && <div className="error-alert">{this.state.loginError}</div>}
                            <div className="form-group">
                                <p>Benutzername</p>
                                <input type="text" id="username" key={this.state.loginFormPrefill ? 'prefilled' : 'empty'} onInput={this.handleLoginInput} defaultValue={this.state.loginFormPrefill}></input>
                            </div>
                            <div className="form-group">
                                <p>Passwort</p>
                                <input type="password" id="password" onInput={this.handleLoginInput}></input>
                            </div>
                            <div className="form-group">
                                <button className="btn login-btn" onClick={this.handleLogin} disabled={this.state.loginDisabled}>Anmelden</button>
                            </div>
                        </div>
                    </div>
                )}

                {this.state.loading && <Loading />}

                {this.state.dialog && (
                    <div className="overlayed-dark">
                        <div className="dialog">
                            {this.state.dialog.map((dialogChild, i) => React.cloneElement(dialogChild, {key: i}))}
                            {this.state.dialogCloseable && (<button className="btn" onClick={this.resetDialog}>OK</button>)}
                        </div>
                    </div>
                )}
            </div>
        );
    }

}