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

export default class RangeInput extends React.Component {

    constructor(props) {
        super(props);
        this.state = {value: 0};
        this.onValChanged = this.onValChanged.bind(this);
    }

    componentDidMount() {
        this.updateBubble();
    }

    onValChanged() {
        this.updateBubble();
    }

    updateBubble() {
        let slider = this.refs.slider;
        let percentage = (slider.value - slider.min) / (slider.max - slider.min);
        let offset = 18 * percentage + 9 + 16;
        this.refs.bubble.style.left = `calc(${percentage * 100}% - ${offset}px)`;
        this.setState({value: slider.value});
    }

    render() {
        return (
            <div className="range">
                <input type="range" ref="slider" {...this.props} onChange={this.onValChanged}></input>
                <span ref="bubble">{this.state.value}</span>
            </div>
        )
    }

}