



# Light Tracker

Light Tracker is an example Partner Software Service developed by Sendal.  The goal of Light Tracker is to provide a reference implementation and  'how to' for Sendal partners looking to integrate their software services with Sendal.

Please visit https://developer.sendal.io for more information on Partner Software Services and developing applications for the Sendal Cloud Platform.

## What is it?

Light Tracker is an example Sendal Partner Software Service implements the following high-level features:

* Uses Sendal's historical data API to provide daily, weekly, and monthly summaries of light usage.  Displays daily and weekly views of the time usage of each light in a home.  
* Displays a forecast of the next hour light usage based on the Sendal predictive cybermodel for each light in the home.
* Tracks light states in realtime to delete anomalies (e.g. a light left on by mistake) based on Sendal's predictive cybe models .  Alerts users via push notification if Light Tracker believes a light is left on by mistake based on the Sendal cybermodel light state prediction.

## Design

Light tracker implements the 2 parts of a typical Partner Software Service.

### Javascript-driven UI

Serves UI files to the Sendal mobile app embedded webview.  Light Tracker implements a simple React-based UI to display lighting historical and predictive information.

### Backend Server in Java

The Light Tracker PSS backend is a web service that runs externally to the Sendal Cloud Platform and interacts with Sendal via the Partner Software Services API.  See the Sendal developer site at https://developer.sendal.io.

for more information on the Partner Software Services API.

The example PSS backend is implemented in Java using the Dropwizard web framework.  The PSS backend relies on a local Mongo datastore for application state (e.g. which homes are configured to use Light Tracker and the current state of lights on anomaly detection).

# Getting Started

Light Tracker is a full web-based application that integrates with the Sendal Cloud Platform.  As a result, it's non-trivial software that interacts with the Sendal Cloud Platform via REST and delivers a user interface through the Sendal mobile application. 

This section describes the step needed for you to build Light Tracker and deploy it through the Sendal Cloud Platform. 

## Development Environment

The Light Tracker application is based on a React web UI and a Java Software Service backend.  This repository includes a vagrant (https://vagrantup.com) Debian Linux virtual machine definition that includes the development dependencies.  Run the vagrant VM to configure a development virtual machine.  Once the VM is running, perform the following one-time setup.  Run the following commands to complete the setup:

```
wget -qO- https://raw.githubusercontent.com/creationix/nvm/v0.33.11/install.sh | bash
source ~/.bashrc
nvm install 10.19.0
curl -L https://www.npmjs.com/install.sh | sh
```



If you want to use you own development environment, you must ensure the following development dependencies are met:

* Java 11 development environment
* Latest version of maven (https://maven.apache.org)
* A local install of mongodb (https://mongodb.org)
* Node.js v10.19.0 (https://nodejs.org)
* Node.js package manager (npm) (https://npmjs.org)



## Build Light Tracker

Building Light Tracker is a two-step procedure.  

### Build UI

Build the UI files using npm and react.  From the light tracker root directory run the following commands.

```
cd ui
npm ci
npm run build
```

The resulting bundle.js files are placed in the lighttracker/ui/static directory.

### Build the Backend Server

To build the backend server, use maven to build the Java code.  From the light tracker root directory run the following command.

```
mvn clean package
```

The resulting jar file is created - lighttracker/target/lighttracker-1.0-SNAPSHOT.jar

## Create a Partner Software Service in the Sendal Cloud Platform

The Sendal developer portal (https://developer.sendal.io) allows any registered user to define a personal user Partner Software Service.  A Partner Software Service definition is required to have your Partner Software Service communicate with Sendal.  Follow these steps:

* Follow the steps listed on the developer site for Getting Started - https://developer.sendal.io/guides/howtousesite#software-service-or-device-gateway-development .   
  * Name your Partner Software Service something unique as Sendal already makes Light Tracker available to all users.  For example, 'My Light Tracker'.
  * Make sure when creating your Partner Software Service you choose 'Partner Software Service' for the Software Service Type.
  * Light Tracker expects specific permissions to function correctly. When defining your Partner Software Service please ensure the following Permissions are enabled.
    * Configuration Peremissions
      * Device Classes
        * lighting
      * Room Configuration
        * Device Room Mapping
        * Room Configuration
      * Home Configuration
        * Home Time Zone
    * Cybermodel Permissions
      * Device Classes
        * lighting
    * States Permissions
      * Device Classes
        * lighting
      * Other States
        * Room States
        * Home States
    * User Permissions
      * User Notifications
        * Sendal User Notifications
  * For your Light Tracker Personal service to communicate with the Sendal Cloud your service must be accessible to the Sendal Cloud Platform.  This means making your service available on the open internet.  This can be done a number of ways, including local firewall pinholes, or deploying Light Tracker to a cloud service.
    * **IMPORTANT**: During development your partner software service may not be fully secured if you cannot implement an HTTPS endpoint with root signed certificate.  During development Sendal allows the Software Service API Endpoint to use HTTP to make development easier.  However, HTTP is not secure and can lead to unauthorized people viewing the messages the Sendal Cloud Platform sends to you.  While the HMAC signatures should provide some ability to authenticate requests, HTTP will still have the potential to leak other important information.
* Edit the Light Tracker config.yml file and find the 'sendalSoftwareService' section.  Copy the following values from the developer site PSS Setup screen.  These value are needed to correctly send and received HMAC signed requests with the Sendal Cloud Platform  (See the https://developer.alpha.sendal.tech/api/pss#hmac-signatures section for more details on HMAC Signatures for Sendal APIs).
  * Set the config.yml scs3pssId value to 'Software Service ID' from the developer site. 
  * Set the config.yml apiKey value to 'API Key' from the developer site. 
  * Set the config.yml secretKey value to 'Secret Key' from the developer site. 
* Choose your deployment type in the config.yml.  The config.yml file start as a 'type 1' deployment.  For more information on deployment types see the developer site Software Services API Software Services UI section.
  * In type 1 and type 2 development Light Tracker will server UI content, so you can contact it directly from your local browser.

## Run Light Tracker

To run Light Tracker you run the following command from the light tracker root directory

```
java -jar target/lighttracker-1.0-SNAPSHOT.jar server config.yml 
```

Light Tracker has verbose logging to help you see the requests and responses sent between it and the Sendal Cloud Platform.



## Use Your Instance of Light Tracker

### Subscribe to Your Instance of Light Tracker

To user Light Tracker you must first subscribe to it from your Sendal mobile app.  

* Open the mobile app and navigate to user settings (the person on the upper right) -> Software Services.
* You should see a service with the same name as the one you created in the developer site.  Select it and tap 'Enable Service'.  Tapping enable service will initiate the subscription API procedure with your instance of Light Tracker (see https://developer.sendal.io/api/pss/management#subscription)

### Access the Light Tracker UI

#### Local Browser Access

In type 1 and type 2 development the Light Tracker server will serve UI assets.  To access them through a local web browser, you must know the correct URL path to access.   The URL path is defined by Sendal to enable multiple UI services to be accessible to end users.  You will need the following information:

* Software Service ID - available from the developer site as 'Software Service ID'.
* Your user ID - available from the developer site.  Click on the user icon (right side of the header bar) -> User Information
* Your Home ID - available from the developer site.  Click on the user icon (right side of the header bar) -> User Information



Open a web browser and type:

```
http://<IP address or host name where you run Light Tracker>:8000/v1/pss/<software service ID>/users/<your user ID>/homes/<your home ID>/assets/index.html
```



#### Sendal App Access

In the app, navigate to the Services tab.  Your Service should appear. Tap on the icon and the Sendal Cloud Platform will retrieve the UI files from your server (if it is running in type 1 or type 2 development).

