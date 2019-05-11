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

import { ScrollFollow } from 'react-lazylog';
import ConsoleLog from './ConsoleLog.react';

export default class Console extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="fullheight">
                <ScrollFollow
                    startFollowing
                    render={({ onScroll, follow, startFollowing, stopFollowing }) => (
                        <ConsoleLog url='no' stream onScroll={onScroll} follow={follow} lineClassName="console-line" highlightLineClassName="highlighted" />
                    )}
                />
            </div>
        )
    }

}

