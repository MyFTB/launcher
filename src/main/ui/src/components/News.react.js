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

import ExternalLink from './base/ExternalLink.react';

export default class News extends React.Component {

    constructor(props) {
        super(props);
        this.state = {posts: false};
    }

    componentDidMount() {
        window.launcher.loading(true);
        window.launcher.sendIpc('request_posts', false, (err, data) => {
            window.launcher.loading(false);
            if (err) {
                return window.launcher.showDialog(true, <p>{err}</p>);
            }
            this.setState({posts: data.posts});
        });
    }

    render() {
        return (
            <div className="news">
                {this.state.posts && (
                    this.state.posts.map((post, i) => {
                        return (
                            <div key={i}><ExternalLink data-link={post.url}>{post.title}</ExternalLink></div>
                        )
                    })
                )}
            </div>
        )
    }

}