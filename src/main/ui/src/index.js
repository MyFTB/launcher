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
import ReactDOM from 'react-dom';
import { BrowserRouter as Router, Route } from "react-router-dom";

import Launcher from './Launcher';

import './index.html';
import './styles/app.scss';
import './fonts/lato.ttf';

import Sidebar from './components/Sidebar.react';
import Main from './components/Main.react';
import Settings from './components/Settings.react';
import AvailablePacks from './components/AvailablePacks.react';
import InstalledPacks from './components/InstalledPacks.react';
import ModpackContextMenu from './components/ModpackContextMenu.react';

document.addEventListener('click', () => {
    let contextMenu = document.getElementsByClassName('contextmenu')[0];
    contextMenu.style.display = 'none';
});

ReactDOM.render(
    <Router>
        <div>
            <Sidebar />
            <Launcher />
            <div className="content">
                <Route exact path="/" component={Main} />
                <Route path="/install" component={AvailablePacks} />
                <Route path="/modpacks" component={InstalledPacks} />
                <Route path="/settings" component={Settings} />
            </div>
            <ModpackContextMenu />
        </div>
    </Router>,
    document.getElementById('main')
);