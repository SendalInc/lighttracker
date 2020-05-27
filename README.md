



# Light Tracker

Light Tracker is an example partner software service (PSS) developed by Sendal.  The goal of Light Tracker is to provide a reference implementation and simple 'how to' for Sendal partners looking to integrate their software services with Sendal.

## What is it?

Light Tracker is an example Partner Software Service developed by Sendal.  LightTracker implements the following high-level features:

* Used Sendal's historical data API to provide daily, weekly, and monthly summaries of light usage.  Displays daily and weekly views of the time usage of each light in a home.  
* Displays a forecast of the next hour light usage based on the Sendal predictive cyber model for each light in the home.
* Tracks light states in realtime to delete anomolies (e.g. a light left on by mistake).  Alerts users (via push notification) if LightTracker believes a light is left on by mistake based on the Sendal cybermodel light state prediction.

## Design

Light tracker implements the 3 parts of a typical Partner Software Service (PSS)

### Javascript-driven UI within the Interaction Service

A primary role of the Interaction Service is to serve UI files to the Sendal mobile app embedded webview.  LightTracker implements a simple React-based UI to display lighting historical and predictive information.

### Interaction Service (IS) based on Node.js

The LightTracker IS makes use of the Sendalserver middleware to handle requests coming from clients.  The  IS contains the UI files and implements a single API to retrieve LightTracker data from the backend server

### PSS Backend in Java

The LightTracker PSS backend is a web service that runs externally to the Sendal Cloud Solution (SCS) and interacts with SCS via the Sendal Partner Services Integration (PSI) API.  

The example PSS backend is implemented in Java using the Dropwizard web framework.  The PSS backend relies on a local Mongo datastore for application state (e.g. which homes are configured to use LightTracker and the current state of lights on anomaly detection).

# Code is for Reference Only!

The code as provided will not compile as it references Sendal common libraries which are not available publicly.  The libraries are used for class definitions for some standard data, and to implement the HMAC signing (for incoming and outgoing messages).  However, the code provides a reference for a typical REST-based backend PSS.



