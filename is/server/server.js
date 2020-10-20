import path from 'path';
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
import "regenerator-runtime/runtime.js"

SourceMapSupport.install();

if (process.env.NODE_ENV !== 'production') {
    let webpack = require('webpack');
    let webpackConfig = require('../webpack.config');
    let compiler = webpack(webpackConfig);

    expressApp.use(require("webpack-dev-middleware")(compiler, {
        noInfo: true, publicPath: webpackConfig.output.publicPath
    }));

    expressApp.use(require("webpack-hot-middleware")(compiler));
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
