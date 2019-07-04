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
        this.onTextChanged = this.onTextChanged.bind(this);
    }

    componentDidMount() {
        this.updateBubble(true);
    }

    onValChanged() {
        this.updateBubble(true);
    }

    onTextChanged() {
        let intVal = parseInt(this.refs.text.value);
        if (intVal !== NaN && this.refs.slider.value !== intVal && intVal >= this.refs.slider.min && intVal <= this.refs.slider.max) {
            this.refs.slider.value = intVal;
            this.updateBubble(false);
        }
    }

    updateBubble(updateText) {
        let slider = this.refs.slider;
        let percentage = (slider.value - slider.min) / (slider.max - slider.min);
        let offset = 18 * percentage + 9 + 16;
        this.refs.bubble.style.left = `${percentage * slider.clientWidth - offset}px`
        this.setState({value: slider.value});
        if (updateText) {
            this.refs.text.value = slider.value;
        }
    }

    render() {
        return (
            <div className="range">
                <input type="range" ref="slider" className="with-text" {...this.props} onChange={this.onValChanged}></input>
                <span ref="bubble">{this.state.value}</span>
                <input type="text" ref="text" className="range-text" onChange={this.onTextChanged}></input>
            </div>
        )
    }

}