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

import ExternalLink from './base/ExternalLink.react';
import Modpack from './base/Modpack.react';

export default class Main extends React.Component {

    constructor(props) {
        super(props);
        this.state = {username: false, packages: false};
    }

    componentDidMount() {
        window.launcher.registerLoginRerender(this);

        window.launcher.sendIpc('request_recent_packs', false, (err, data) => {
            if (err) {
                return window.launcher.showDialog(true, <p>{err}</p>);
            }
            this.setState({ packages: JSON.parse(data.packages) });
        });
    }

    componentWillUnmount() {
        window.launcher.unregisterLoginRerender(this);
    }

    onLogin(profile) {
        this.setState({username: profile.name});
    }

    onModpackClick(index) {
        window.launcher.launchModpack(this.state.packages[index]);
    }

    render() {
        return (
            <div className="main-page">
                <h2>Willkommen{this.state.username && ' ' + this.state.username}!</h2>
                <ul>
                    <li><ExternalLink data-link="https://myftb.de">Webseite</ExternalLink></li>
                    <li><ExternalLink data-link={"ts3server://ts.myftb.de?password=myftbtspw" + (this.state.username ? "&nickname=" + this.state.username : "")}>Teamspeak</ExternalLink></li>
                    <li><ExternalLink data-link="https://discord.myftb.de">Discord</ExternalLink></li>
                    <li><ExternalLink data-link="https://forum.myftb.de">Forum</ExternalLink></li>
                    <li><ExternalLink data-link="https://torch.myftb.de">Torch</ExternalLink></li>
                </ul>

                <h3>Vote für uns</h3>
                <p>Durch das Voten auf <ExternalLink data-link="https://myftb.de/voten">unseren Serverlisten</ExternalLink> kannst du uns durch einen kleinen Aufwand unterstützen.</p>

                <h3>Wähle ein Modpack</h3>
                <p>Bereits installierte Modpacks kannst du im linken Menü unter <b>Installierte Modpacks</b> starten. Suchst du ein neues Modpack kannst du dies über den Menüpunkt <b>Verfügbare Modpacks</b> installieren</p>

                <div className="bottom">
                    <div className="packs recent">
                        {this.state.packages && (
                            this.state.packages.map((pack, i) => {
                                return <Modpack key={i} pack={pack} packinstalled="true" onClick={this.onModpackClick.bind(this, i)}></Modpack>
                            })
                        )}
                    </div>
                </div>
            </div>
        )
    }

}