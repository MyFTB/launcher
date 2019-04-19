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
import { NavLink } from "react-router-dom";

import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faHome, faNewspaper, faBoxOpen, faCloud, faCogs } from '@fortawesome/free-solid-svg-icons'

library.add(faHome)
library.add(faNewspaper)
library.add(faBoxOpen)
library.add(faCloud)
library.add(faCogs)

export default class Sidebar extends React.Component {

    constructor(props) {
        super(props);
        this.state = {profile: false};
    }

    componentDidMount() {
        window.launcher.registerLoginRerender(this);
    }

    componentWillUnmount() {
        window.launcher.unregisterLoginRerender(this);
    }

    onLogin(profile) {
        this.setState({profile: profile});
    }

    render() {
        return (
            <div className="sidebar">
                <ul>
                    <li><NavLink exact to="/" activeClassName="active"><FontAwesomeIcon icon="home"/> Startseite</NavLink></li>
                    <li><NavLink to="/news" activeClassName="active"><FontAwesomeIcon icon="newspaper"/> Neuigkeiten</NavLink></li>
                    <li><NavLink to="/modpacks" activeClassName="active"><FontAwesomeIcon icon="box-open"/> Installierte Modpacks</NavLink></li>
                    <li><NavLink to="/install" activeClassName="active"><FontAwesomeIcon icon="cloud"/> Verfügbare Modpacks</NavLink></li>
                    <li><NavLink to="/settings" activeClassName="active"><FontAwesomeIcon icon="cogs"/> Einstellungen</NavLink></li>
                    
                    {this.state.profile && (
                        <li className="profile"><img src={"playerhead://" + this.state.profile.id}></img> Angemeldet als: {this.state.profile.name}</li>
                    )}
                </ul>
            </div>
        )
    }

}