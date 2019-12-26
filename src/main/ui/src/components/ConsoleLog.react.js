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

import { LazyLog } from 'react-lazylog';
import { convertBufferToLines } from 'react-lazylog/src/utils';
import { encode } from 'react-lazylog/src/encoding';

export default class ConsoleLog extends LazyLog {

    constructor(props) {
        super(props);
    }

    request() {
        this.endRequest();
        let encoder = new TextEncoder();

        window.launcher.listenIpcRaw('console_data', (err, dataRaw) => {
            let encodedLog = encode(dataRaw);
            const { lines, remaining } = convertBufferToLines(encodedLog);
            this.handleUpdate({
                lines: remaining ? lines.concat(remaining) : lines,
                encodedLog,
            });
        });
        window.launcher.sendIpc('request_console', false);
    }

    componentWillUnmount() {
        window.launcher.unregisterIpc('console_data');
    }

    static getDerivedStateFromProps(props, state) {
        let newState = super.getDerivedStateFromProps(props, state);
        newState.lineLimit = 10000;
        return newState;
    }

}