const webpack = require('webpack');

const isDevelopment = process.env.NODE_ENV !== 'production';

module.exports = {
    mode: isDevelopment ? 'development' : 'production',
    entry: {
        app: ['./src/App.jsx'],
        vendor: ['webpack-hot-middleware/client', 'react', 'react-dom', 'react-router-dom', 'react-bootstrap', 'reactstrap'],
    },
    plugins: [
        // OccurrenceOrderPlugin is needed for webpack 1.x only
        new webpack.optimize.OccurrenceOrderPlugin(),
        new webpack.HotModuleReplacementPlugin(),
        // Use NoErrorsPlugin for webpack 1.x
        new webpack.NoEmitOnErrorsPlugin()
    ],
    output: {
        path: __dirname + '/static',
        filename: '[name].bundle.js'
    },
    optimization: {
        splitChunks: {
            cacheGroups: {
                commons: {
                    test: /[\\/]node_modules[\\/]/,
                    name: 'vendor',
                    chunks: 'all'
                }
            }
        },
        nodeEnv: isDevelopment ? 'development' : 'none'
    },
    module: {
        rules: [
            // JavaScript/JSX Files
            {
                test: /\.jsx$/,
                exclude: /node_modules/,
                use: ['babel-loader'],
            },
            // CSS Files
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader'],
            }
        ]
    },
    devServer: {
        port: 8000,
        contentBase:'static',
        host: '127.0.0.1',
        hot: true,
        proxy: {
            '/api/*': {
                target: 'http://localhost:3000'
            }
        },
        historyApiFallback: true,
    },
    devtool: isDevelopment ? 'source-map' : false
};
