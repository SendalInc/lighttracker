import path from 'path';
const babelPolyfill = require('babel-polyfill');
const SourceMapSupport = require('source-map-support');
const fetch = require("node-fetch");
import {
    logger,
    addNewRoom,
    updateClientWithRooms,
    appManagedResource,
    sendalConfig,
    httpPort,
    applicationId,
    expressApp,
    clientContexts,
    sendalServer
} from 'sendalserver';

SourceMapSupport.install();

if (process.env.NODE_ENV !== 'production') {
    const webpack = require('webpack');
    const webpackDevMiddleware = require('webpack-dev-middleware');
    const webpackHotMiddleware = require('webpack-hot-middleware');

    const config = require('../webpack.config');
    config.entry.app.push('webpack-hot-middleware/client', 'webpack/hot/only-dev-server');
    config.plugins.push(new webpack.HotModuleReplacementPlugin());

    const bundler = webpack(config);
    expressApp.use(webpackDevMiddleware(bundler, { noInfo: true }));
    expressApp.use(webpackHotMiddleware(bundler, { log: console.log }));
}

expressApp.get("/api/:facility/users/:userid/homes/:homeid/usagesummary", async (req, res) => {
    // get the control struct for this home only if the home is not already
    // active in our app.  We use the sendal-server clientContext to carry
    // both home and connection context for us.
    let headers = { authorization: req.headers.authorization}

    fetch("http://ssoutbound:8003/api/pss/" + sendalConfig.sendalConfiguration.softwareServicesId +"/homes/" + req.params.homeid + "/passthrough/usagesummary", {headers: headers})
        .then(response => {
            if (response.ok == true) {
                response.json().then(json => {
                    res.status(response.status).json(json)
                })
            } else {
                res.status(response.status).send(response.body)
            }
        })
        .catch(error => {
            logger.error(error)
            res.status(500).send(error)
        })
})
