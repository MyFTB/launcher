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

import placeholder from '../../img/pack_placeholder.png';

export default class Modpack extends React.Component {

    constructor(props) {
        super(props)
        this.onContextMenu = this.onContextMenu.bind(this);
    }

    componentWillUnmount() {
        if (window.contextmenu_modpack === this) {
            window.contextmenu_modpack = null;
        }
    }

    getLastUpdate() {
        return this.props.pack.version.split('_').reverse()
            .map((v, i) => i == 0
                ? v.split('-').slice(0, 2).join(':')
                : v.split('-').reverse().join('.'))
            .join(' ');
    }

    onContextMenu(e) {
        if (this.props.packinstalled) {
            e.preventDefault();

            window.contextmenu_modpack = this;
            let contextMenu = document.getElementsByClassName('contextmenu')[0];
            contextMenu.style.left = 0;
            contextMenu.style.top = 0;
            contextMenu.style.display = 'block';
            contextMenu.style.top = e.pageY + 'px';

            if ((e.pageX + contextMenu.clientWidth) > document.body.clientWidth) {
                contextMenu.style.left = (e.pageX - contextMenu.clientWidth) + 'px';
            } else {
                contextMenu.style.left = e.pageX + 'px';
            }
        }
    }

    onImageError(e) {
        e.target.src = placeholder;
    }

    render() {
        return (
            <div className="pack" {...this.props} onContextMenu={this.onContextMenu}>
                <img src={'modpackimage://launcher/' + encodeURI(this.props.pack.name)} onError={this.onImageError}></img>
                <div className="blackout"></div>
                <p><b>{this.props.pack.title}</b></p>
            </div>
        )
    }

}