# LightTracker Interaction Service

The UI and Interaction Service (IS) server for the LightTracker example Partner Software Service (PSS).  Implemented using Node.JS and React.  

Interaction services are designed to run within the Sendal Cloud Solution (SCS) to provide front-end interactions in support of the PSS backend. 



# Sendalserver Middleware

LightTracker makes use fo the Sendalserver middleware - A node.JS library meant to simplify termination of connections from clients and integration with the Sendal cloud.

Sendalserver is a Node.JS/Javascript middleware to simplify partner software service's implementation of their Integration Server.  The goal of sendalserver is to provide common code for all integration services.  Sendal uses sendalserver middleware internally to implement our interaction services (e.g. homescreen and default device interaction services). 

# Middleware Services

The following services are provided by the sendalserver middleware.

## config.yml parsing

Sendalserver supports parting of a standard config.yml file for configuring information in the interaction server.  Some configuration values are needed by sendalserver and are processed directly by it.  Interaction services can extend the config.yml as needed.

### logging

The logging section of config.yml configures the logger subsystem in sendalserver.  The logger is available to interaction services via the 'logger' exported value.   The logger is based on Node.JS Bunyan logger.

  `logging:`

​    `stdout:`

​      `level: info`

​    `googleCloud:`

​      `level: info`

Logging defines 2 optional logging types, stdout and googleCloud.  googleCloud logging type should be used only when deploying in Sendal's cloud as it will make google cloud API calls to record logs in the central log collector.

The level parameter defines the minimum log level to record.  All log levels 'below' the indicated level will be ignored/not logged.

If no logging section is specified, sendalserver will default to stdout logging at info level filtering.

Sendalserver's logger is available via the logger export.

### sendalConfiguration

sendalConfiguration is a collection of miscellaneous IDs required to identify the interaction service and the other entities will communicates with.

| sendalConfiguration element | Description                                                  |
| --------------------------- | ------------------------------------------------------------ |
| applicationId               | The Sendal-assigned ID for your interaction service.  Available via the applicationId export. |
| httpPort                    | The Sendal-assigned HTTP server port number assigned for your service.  For most interaction services, the value 8004 is used.  Available via the http export. |
| appServiceName              | The Sendal-assigned serviceName for your interaction service.  appServiceNames are typically used in logs and for debugging.   Available via the appServiceName export. |
| stateUpdatesRequired        | boolean value indicating if the interaction service will ask for state updates.  If true, sendalserver will initiate the AMQP subsystem to receive state updates via a queue. |
| softwareServicesId          | The Sendal-assigned ID for the partner software service backend.  PSS backends are assigned different IDs than the interaction service's frontend (defined in the applicationId) field. |

The complete parsed yaml is available to interaction services via sendalserver's sendalConfig export.

### Example config.yml

 `default:`

  `sendalConfiguration:` 

​    `appServiceName: lighttracker`

​    `applicationId: <Sendal provided interaction service ID>`

​    `softwareServicesId: <Sendal provided software services ID>`

​    `stateUpdatesRequired: false`

​    `httpPort: 8004`

  `logging:`

​    `stdout:`

​      `level: info`

​    `googleCloud:`

​      `level: info`

## expressApp

Sendalserver exports an expressApp variable to reference the express server sendalserver creates.  Interaction services creating APIs should use the expressApp variable to define their APIs.

### Terminated APIs

Sendalserver terminates a number of APIs using express.

| URL                                                          | Return Value                                        | Notes                                                        |
| ------------------------------------------------------------ | --------------------------------------------------- | ------------------------------------------------------------ |
| GET */images/:imagefilename                                  |                                                     | relative extension to any valid path for the interaction service.  Will return the specified image file. |
| GET /applications/:facility/ users/:userid/homes/:homeid     | index.html                                          | returns the javascript client code.  This case is used if the mobile app sends a get request for this UI 'route'. |
| GET /applications/:facility/ users/:userid/homes/:homeid/ rooms/:roomId | index.html                                          | returns the javascript client code.  This case is used if the mobile app sends a get request for this UI 'route'. |
| GET /applications/:facility/ users/:userid/homes/:homeid/ devices/:deviceId | index.html                                          | returns the javascript client code.  This case is used if the mobile app sends a get request for this UI 'route'. |
| GET /applications/:facility/ users/:userid/homes/:homeid/ configure | index.html                                          | returns the javascript client code.  This case is used if the mobile app sends a get request for this UI 'route'. |
| GET /applications/:facility/ users/:userid/homes/:homeid/ devicecontrollers/:devicecontrollerid/ configure | index.html                                          | returns the javascript client code.  This case is used if the mobile app sends a get request for this UI 'route'. |
| GET /applications/:facility/ users/:userid/homes/:homeid/ scenedefine/:sceneid | index.html                                          | returns the javascript client code.  This case is used if the mobile app sends a get request for this UI 'route'. |
| GET /applications/:facility/ users/:userid/homes/:homeid/ scenedefine/:sceneid/devices/:deviceid | index.html                                          | returns the javascript client code.  This case is used if the mobile app sends a get request for this UI 'route'. |
| GET /applications/:facility/ users/:userid/homes/:homeid/ scenedefine/:sceneid/rooms/:roomId | index.html                                          | returns the javascript client code.  This case is used if the mobile app sends a get request for this UI 'route'. |
| GET /api/:facility/servertimestamp                           | JSON {<br />serverTimestamp: '<server timestamp>' } | Used to detect updates on the interaction server side.  Clients will typically poll this value when they are active and will reload themselves if the server timestamp changes. |
| PUT /api/:facility/ users/:userid/homes/:homeid/ devices/:deviceid/command | HTTP status and data from the device cloud.         | Used to invoke a device command.   Request data format: <br />{<br />command: '<command String>',<br />isCustom: <boolean if a custom command>,<br />resource: '<resource being controlled>',<br />controlledId: '<Sendal defined device controller to be used to control the device>',<br />body: <JSON formatted argument data>}<br /><br />Please contact Sendal if your want to send device commands from your interaction service. |



## Websocket

Will be described in a future update.

## State Updates

Will be described in a future update.



# Using Sendalserver

To use the sendalserver, simply import it into your server javascript file.  Importing from the file will activate the sendalserver logic automatically.

`import {`

​    `logger,`

​    `sendalConfig,`

   `expressApp,`

`} from 'sendalserver';`

## Reference Implementation

Sendal's publicly available example LightTracker PSS uses the Sendalserver to implement the IS functionality.

# Code is for Reference Only!

The code as provided will not compile as it references the Sendalserver, which is not yet publicly available.   However, the code provides a reference for a typical REST-based backend PSS.