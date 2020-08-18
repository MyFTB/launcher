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
import { CSSTransition } from 'react-transition-group';

import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faHome, faNewspaper, faBoxOpen, faCloud, faCogs, faPlusSquare } from '@fortawesome/free-solid-svg-icons'

library.add(faHome);
library.add(faNewspaper);
library.add(faBoxOpen);
library.add(faCloud);
library.add(faCogs);
library.add(faPlusSquare);

export default class Sidebar extends React.Component {

    constructor(props) {
        super(props);
        this.state = {profiles: false, hoverLogout: false, maxDisplayableProfiles: 4};
        this.onProfileMouseEnter = this.onProfileMouseEnter.bind(this);
        this.onProfileMouseLeave = this.onProfileMouseLeave.bind(this);
        this.onProfileClick = this.onProfileClick.bind(this);
        this.onAddProfileClick = this.onAddProfileClick.bind(this);
        this.onProfileLogoutClick = this.onProfileLogoutClick.bind(this);
    }

    componentDidMount() {
        window.launcher.registerUpdateProfilesRerender(this);
        window.addEventListener('resize', () =>  {
            const sidebar = document.getElementsByClassName('sidebar')[0];
            const sidebarHeight = sidebar.clientHeight;
            const navHeight = sidebar.getElementsByTagName('ul')[0].clientHeight;

            this.setState({maxDisplayableProfiles: Math.ceil((sidebarHeight - navHeight) / 2 / 48)});
        });
    }

    componentWillUnmount() {
        window.launcher.unregisterUpdateProfilesRerender(this);
    }

    onUpdateProfiles(profiles) {
        this.setState({profiles: profiles});
    }

    onProfileMouseEnter() {
        this.setState({hoverLogout: true});
    }

    onProfileMouseLeave() {
        this.setState({hoverLogout: false});
    }

    onProfileClick(i) {
        window.launcher.switchProfile(i)
    }

    onAddProfileClick() {
        window.launcher.showLoginForm('', true)
    }

    onProfileLogoutClick() {
        window.launcher.logout();
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
                </ul>
                <ul className="profiles">
                    {this.state.profiles && this.state.profiles.length > 0 && (
                        <li className="profile" onMouseEnter={this.onProfileMouseEnter} onMouseLeave={this.onProfileMouseLeave} onClick={this.onProfileLogoutClick}>
                            <img src={"playerhead://launcher/" + this.state.profiles[0].id}></img>
                            {this.state.hoverLogout ? (<span>Abmelden</span>) : (<span>Angemeldet als: {this.state.profiles[0].name}</span>)}
                        </li>
                    )}
                    <CSSTransition in={this.state.hoverLogout} timeout={200} classNames={"fade"} unmountOnExit>
                        <div onMouseEnter={this.onProfileMouseEnter} onMouseLeave={this.onProfileMouseLeave}>
                            {this.state.profiles.length > 1 && (
                                this.state.profiles.map((profile, i) => {
                                    if (i === 0 || i > this.state.maxDisplayableProfiles - 2) return;
                                    return (
                                        <li className="profile" key={i} onClick={() => this.onProfileClick(i)}>
                                            <img src={"playerhead://launcher/" + profile.id}></img>
                                            <span>Wechseln zu: {profile.name}</span>
                                        </li>
                                    )
                                })
                            )}
                            {this.state.profiles.length > 0 && (
                                <li className="profile" onClick={() => this.onAddProfileClick()}>
                                    <FontAwesomeIcon icon="plus-square"/>
                                    <span>Benutzer hinzufügen</span>
                                </li>
                            )}
                        </div>
                    </CSSTransition>
                </ul>
            </div>
        )
    }

}