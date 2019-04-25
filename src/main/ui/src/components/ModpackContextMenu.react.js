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

export default class ModpackContextMenu extends React.Component {

    constructor(props) {
        super(props);
    }

    onEntryClick(index) {
        if (index == 0) {
            window.contextmenu_modpack.props.onClick();
        } else {
            window.launcher.loading(true);
            window.launcher.sendIpc('modpack_menu_click', {pack: window.contextmenu_modpack.props.pack.name, index: index}, (err, data) => {
                window.launcher.loading(false);
                if (err) {
                    return window.launcher.showDialog(true, <p>{err}</p>);
                }
            });
        }
    }

    render() {
        return (
            <div className="contextmenu">
                <ul>
                    <li onClick={this.onEntryClick.bind(this, 0)}>Starten</li>
                    <li onClick={this.onEntryClick.bind(this, 1)}>Ordner öffnen</li>
                    <li onClick={this.onEntryClick.bind(this, 2)}>Modpack löschen</li>
                    <li onClick={this.onEntryClick.bind(this, 3)}>Modpack aktualisieren</li>
                    <li onClick={this.onEntryClick.bind(this, 4)}>Crashreport hochladen</li>
                    <li onClick={this.onEntryClick.bind(this, 5)}>Desktop-Verknüpfung anlegen</li>
                </ul>
            </div>
        )
    }

}