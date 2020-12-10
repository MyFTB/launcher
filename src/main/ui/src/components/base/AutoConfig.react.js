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

import ToggleSwitch from './ToggleSwitch.react';

import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus, faMinus } from '@fortawesome/free-solid-svg-icons'

library.add(faPlus);
library.add(faMinus);

export default class AutoConfig extends React.Component {

    constructor(props) {
        super(props);
        this.state = {observer: false, popoverActive: false, setOptions: []};

        this.onClickAdd = this.onClickAdd.bind(this);
        this.observerCallback = this.observerCallback.bind(this);
        this.onOptionPopoverClick = this.onOptionPopoverClick.bind(this);
    }

    componentDidMount() {
        let observer = new IntersectionObserver(this.observerCallback, {root: document.querySelector('.content'), rootMargin: '0px', threshold: 1.0});
        observer.observe(this.refs.plus);

        this.setState({
            observer: observer
        });
    }

    componentWillUnmount() {
        this.state.observer.disconnect();
    }

    observerCallback(e) {
        if (!e[0].isIntersecting) {
            this.refs.popover.classList.remove('active');
        }
    }

    onClickAdd() {
        let rect = this.refs.plus.getBoundingClientRect();
        this.refs.popover.style.left = `${rect.x + rect.width - 250}px`;
        this.refs.popover.style.top = `${rect.y - 210}px`;

        this.setState(state => {
            return {
                popoverActive: !state.popoverActive
            }
        });
    }

    isOptionSet(option) {
        for (let opt of this.state.setOptions) {
            if (opt.id === option.id) {
                return true;
            }
        }

        return false;
    }

    onOptionPopoverClick(newOption) {
        if (this.isOptionSet(newOption)) {
            return;
        }

        this.setState(state => {
            return {
                setOptions: state.setOptions.concat(newOption),
                popoverActive: false
            }
        });
    }

    getAutoConfigValue(option, index) {
        let setting;
        let settingClass = 'setting-value';

        if (option.type === 'boolean') {
            settingClass += ' switch-container';
            setting = <ToggleSwitch></ToggleSwitch>
        } else if (option.type === 'int') {
            let constraint;
            for (let constraintVal of this.props.constraints) {
                if (constraintVal.id === option.id) {
                    constraint = constraintVal;
                    break;
                }
            }

            setting = <input type="number" {...constraint}></input>
        } else if (option.type === 'double') {
            setting = <input type="number" min="0" max="100"></input>
        } else {
            for (let type of this.props.types) {
                if (type.type === option.type) {
                    setting = (
                        <select>
                            {type.values.map((option, index) => {
                                return <option key={index}>{option}</option>
                            })}
                        </select>
                    )
                    break;
                }
            }
        }

        return (
            <li key={index}>
                <span>{option.name}</span>
                <span className={settingClass}>{setting}</span>
            </li>
        );
    }

    render() {
        return (
            <div className="autoconfig-container">
                <div className="autoconfig-list">
                    <ul ref="configList">
                        {this.state.setOptions.map((option, index) => this.getAutoConfigValue(option, index))}
                    </ul>
                </div>
                <div className="autoconfig-toolbar">
                    <ul>
                        <li onClick={this.onClickAdd} ref="plus"><FontAwesomeIcon icon="plus"/></li>
                    </ul>
                </div>
                <div className={'autoconfig-popover ' + (this.state.popoverActive ? 'active' : '')} ref="popover">
                    <ul>
                        {this.props.configs.map((option, index) => {
                            return <li key={index} className={this.isOptionSet(option) ? 'disabled' : ''} onClick={() => this.onOptionPopoverClick(option)}>{option.name}</li>
                        })}
                    </ul>
                </div>
            </div>
        );
    }

}