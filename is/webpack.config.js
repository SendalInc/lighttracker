const webpack = require('webpack');

module.exports = {
    entry: {
        app: ['./src/App.jsx'],
        vendor:['react', 'react-dom', 'react-router-dom', 'react-bootstrap', 'reactstrap'],
    },
    output: {
        path: __dirname + '/static',
        filename: 'app.bundle.js'
    },
    plugins: [
        new webpack.optimize.CommonsChunkPlugin({ name: 'vendor', filename: 'vendor.bundle.js' })
    ],
    module: {
        loaders: [
            {
                test: /\.jsx$/,
                loader: 'babel-loader',
                query: {
                    presets:['react','es2015']
                }
            },
            {
                test: /\.css$/,
                include: /node_modules/,
                loaders: ['style-loader', 'css-loader'],
            },
        ]
    },
    devServer: {
        port: 8000,
        contentBase:'static',
        host: '127.0.0.1',
        proxy: {
            '/api/*': {
                target: 'http://localhost:3000'
            }
        },
        historyApiFallback: true,
    },
    devtool: 'source-map'
};
