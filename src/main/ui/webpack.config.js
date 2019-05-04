const path = require('path');

module.exports = {
    module: {
        rules: [
            {
                test: /\.(js|jsx)$/,
                exclude: /node_modules/,
                use: {
                    loader: "babel-loader"
                }
            },
            {
                test: /\.scss$/,
                use: [
                    "style-loader", // creates style nodes from JS strings
                    "css-loader", // translates CSS into CommonJS
                    "sass-loader" // compiles Sass to CSS, using Node Sass by default
                ]
            },
            {
                test: /\.(png|jpg|gif|woff2|html)$/,
                use: [
                    {
                        loader: 'file-loader',
                        options: {
                            name: '[name].[ext]',
                        }
                    },
                ],
            }
        ]
    },
    output: {
        filename: 'app.js',
        path: path.resolve(__dirname, 'build/webroot')
    },
    performance: {
        maxAssetSize: Number.MAX_VALUE
    },
    devServer: {
        historyApiFallback: true,
    }
};