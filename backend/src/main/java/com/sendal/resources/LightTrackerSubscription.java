package com.sendal.lighttracker.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response;
import javax.annotation.PostConstruct;
import javax.validation.Validator;
import javax.validation.ConstraintViolation;

import com.sendal.common.Constants;
import com.sendal.common.StateIdentifier;
import com.sendal.common.coredb.DBDevice;
import com.sendal.common.StateRegistration;

import io.dropwizard.auth.Auth;
import org.bson.types.ObjectId;

import io.dropwizard.client.JerseyClientBuilder;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.InputStream;

import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;

import com.sendal.common.coredb.DBPermissions;
import com.sendal.common.StateUpdater;
import com.sendal.common.StateReport;
import com.sendal.common.StateRegistration;

import com.sendal.externalapicommon.security.IDPrincipal;
import com.sendal.externalapicommon.db.dao.AccessPermissionDAO;
import com.sendal.externalapicommon.db.AccessPermission;

import com.sendal.externalapicommon.ExternalHomeConfiguration;
import com.sendal.externalapicommon.ExternalDeviceConfiguration;
import com.sendal.externalapicommon.ExternalRoomConfiguration;
import com.codahale.metrics.annotation.*;

@Timed
@ResponseMetered
@ExceptionMetered
@Path("/api/v1/homes/{homeid}")
@Produces(MediaType.APPLICATION_JSON)
public class LightTrackerSubscription {
    private final Client client;
    private final Validator validator;
    private final LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao;
    private final String scsEndpoint;
    private final String scs3pssId;
    private final Logger logger = LoggerFactory.getLogger(LightTrackerSubscription.class);

    public LightTrackerSubscription(Client client, LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao, String scsEndpoint, String scs3pssId, Validator validator) {
        this.client = client;
        this.validator = validator;
        this.scsEndpoint = scsEndpoint;
        this.scs3pssId = scs3pssId;
        this.lightTrackerSubscriptionRecordDao = lightTrackerSubscriptionRecordDao;
    }

    @POST
    @Path("/subscription")
    public Response subscribeHome(@PathParam("homeid") ObjectId homeId, DBPermissions appPermission) {
        Response response = Response.ok().build(); // can be replaced below

        Set<ConstraintViolation<DBPermissions>> violations = validator.validate(appPermission);
        if(violations.size() > 0) {
            // Validation errors occurred
            ArrayList<String> validationMessages = new ArrayList<String>();
            for (ConstraintViolation<DBPermissions> violation : violations) { 
                validationMessages.add(violation.getPropertyPath().toString() +": " + violation.getMessage()); 
            } 

            response = Response.status(Status.BAD_REQUEST).entity(validationMessages)
                    .type(MediaType.TEXT_PLAIN).build(); 
        } else { 
            // see if we have this subscription already - if so, we update it, else we
            // create it.
            LightTrackerSubscriptionRecord ltSubRec = lightTrackerSubscriptionRecordDao.getHomeSubscription(homeId);

            boolean newSubscription = false;

            if (ltSubRec == null) {
                newSubscription = true;
                ltSubRec = new LightTrackerSubscriptionRecord();
                ltSubRec.setHomeId(homeId);
                ltSubRec.setPermissions(appPermission);
            }

            // query the SCS cloud for the new subscription information we need to get started.
            // Read lighting configuration and room mapping
            // GET /api/v1/configurations/ - will be factored for permissions
            Invocation invocation;
            Response homeConfigResponse = null;

            synchronized (client) {
                invocation = client
                    .target(scsEndpoint)
                    .path("/api/v1/homes/"+homeId.toString()+"/configuration")
                    .request()
                    .header("Sendal3PSSId", scs3pssId)
                    .buildGet();
            }

            try {
                homeConfigResponse = invocation.invoke(Response.class);

                if(homeConfigResponse.getStatus() == Status.OK.getStatusCode()) {
                    ExternalHomeConfiguration homeConfig = homeConfigResponse.readEntity(ExternalHomeConfiguration.class);

                    ltSubRec.setHomeConfig(homeConfig);

                    // The resulting external home configuration is tailored to us based on the defined permissions within
                    // SCS.  We get only the info we are allowed to have.

                    // store or update the configuration of the home.

                    // Light tracker is interested in each light state, each room's light state4, and each home's light state.
                    // create a state registration set for the states.

                    List<StateRegistration> stateRegistrations = new ArrayList<StateRegistration>();

                    if(homeConfig.deviceConfigurations.get("lighting") != null) {
                        // home lighting summary state
                        StateRegistration sr = new StateRegistration();
                        StateIdentifier si = new StateIdentifier();
                        si.setHome(homeId);
                        si.setStateName("LightsOn");
                        sr.setStateIdentifier(si);
                        stateRegistrations.add(sr);

                        // per-device states
                        for(ExternalDeviceConfiguration edc : homeConfig.deviceConfigurations.get("lighting")) {
                            if((boolean)(((Map<String, Object>)(edc.resourcesConfig.get("lighting"))).get("onOff")) == true) {
                                sr = new StateRegistration();
                                si = new StateIdentifier();
                                si.setHome(homeId);
                                si.setDevice(new ObjectId(edc.deviceId));
                                si.setResource("lighting");
                                si.setStateName("isOn");
                                sr.setStateIdentifier(si);
                                stateRegistrations.add(sr);
                            }

                            if((boolean)(((Map<String, Object>)(edc.resourcesConfig.get("lighting"))).get("dimmable")) == true) {
                                sr = new StateRegistration();
                                si = new StateIdentifier();
                                si.setHome(homeId);
                                si.setDevice(new ObjectId(edc.deviceId));
                                si.setResource("lighting");
                                si.setStateName("dimmerLevel");
                                sr.setStateIdentifier(si);
                                stateRegistrations.add(sr);
                            }
                        }

                        // per-room states
                        // our permissions in SCS allow us to see only rooms w/lights
                        for(String roomId : homeConfig.roomConfigurations.keySet()) {
                            sr = new StateRegistration();
                            si = new StateIdentifier();
                            si.setHome(homeId);
                            si.setRoom(new ObjectId(roomId));
                            si.setStateName("LightsOn");
                            sr.setStateIdentifier(si);
                            stateRegistrations.add(sr);
                        }
                    }

                    boolean saveHomeConfig = true;

                    if(stateRegistrations.size() > 0) {
                        // send the state registration request.
                        synchronized (client) {
                            invocation = client
                                .target(scsEndpoint)
                                .path("api/v1/states/subscriptions/subscribe")
                                .request()
                                .header("Sendal3PSSId", scs3pssId)
                                .buildPost(Entity.json(stateRegistrations));
                        }

                        Response stateRegistrationResponse = null;

                        try {
                            stateRegistrationResponse = invocation.invoke(Response.class);

                            if(stateRegistrationResponse.getStatus() != Status.OK.getStatusCode()) {
                                saveHomeConfig = false;
                                logger.error("Cannot subscribe for states -  " + stateRegistrationResponse.getStatus() + " - " + stateRegistrationResponse.readEntity(String.class));
                                response = Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .entity("Error while subscribing for states - " + stateRegistrationResponse.getStatus())
                                        .type(MediaType.TEXT_PLAIN).build();
                            }

                        } catch (Exception exception) {
                            // Output expected ConnectException.
                            saveHomeConfig = false;
                            logger.error("Register state exception - " + exception);
                            response = Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .entity("Exception while subscribing for states - " + exception)
                                    .type(MediaType.TEXT_PLAIN).build();
                        } finally {
                            if(stateRegistrationResponse != null) {
                                stateRegistrationResponse.close();
                            }
                        }
                    }

                    if(saveHomeConfig == true) {
                        if(newSubscription == true) {
                            lightTrackerSubscriptionRecordDao.createHomeSubscription(ltSubRec);
                        } else {
                            lightTrackerSubscriptionRecordDao.updateHomeSubscription(ltSubRec);
                        }
                    }
                } else {
                    // no home config was retrieved.  this is an error
                    String responseBody =  homeConfigResponse.readEntity(String.class);

                    logger.error("Cannot retrieve home config -  " + homeConfigResponse.getStatus() + " - " + responseBody);

                    response = Response.status(homeConfigResponse.getStatus()).entity(responseBody)
                            .type(MediaType.TEXT_PLAIN).build();

                }
            } catch (Exception exception) {
                // Output expected ConnectException.
                logger.error("Get home config exception - " + exception);
                response = Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Exception while sending home config request - " + exception)
                        .type(MediaType.TEXT_PLAIN).build();
            } finally {
                if(homeConfigResponse != null) {
                    homeConfigResponse.close();
                }
            }
        }

        return response;
    }


    @POST
    @Path("/configuration/updated")
    // currently does not handle de-registering for lights that no longer exist.
    public Response subscribeHome(@PathParam("homeid") ObjectId homeId) {
        Response response = Response.ok().build(); // can be replaced below

        logger.info("Received configuration updated for home - " + homeId);
        // see if we have this subscription already - if so, we update it, else we
        // create it.
        LightTrackerSubscriptionRecord ltSubRec = lightTrackerSubscriptionRecordDao.getHomeSubscription(homeId);

        if (ltSubRec != null) {
            // query the SCS cloud for the new subscription information we need to get
            // started.
            // Read lighting configuration and room mapping
            // GET /api/v1/configurations/ - will be factored for permissions
            Invocation invocation;
            Response homeConfigResponse = null;

            synchronized (client) {
                invocation = client.target(scsEndpoint).path("/api/v1/homes/" + homeId.toString() + "/configuration")
                        .request().header("Sendal3PSSId", scs3pssId).buildGet();
            }

            try {
                homeConfigResponse = invocation.invoke(Response.class);

                if (homeConfigResponse.getStatus() == Status.OK.getStatusCode()) {
                    ExternalHomeConfiguration homeConfig = homeConfigResponse
                            .readEntity(ExternalHomeConfiguration.class);

                    ltSubRec.setHomeConfig(homeConfig);

                    // The resulting external home configuration is tailored to us based on the
                    // defined permissions within
                    // SCS. We get only the info we are allowed to have.

                    // store or update the configuration of the home.

                    // Light tracker is interested in each light state, each room's light state4,
                    // and each home's light state.
                    // create a state registration set for the states.

                    List<StateRegistration> stateRegistrations = new ArrayList<StateRegistration>();

                    if (homeConfig.deviceConfigurations.get("lighting") != null) {
                        // home lighting summary state
                        StateRegistration sr = new StateRegistration();
                        StateIdentifier si = new StateIdentifier();
                        si.setHome(homeId);
                        si.setStateName("LightsOn");
                        sr.setStateIdentifier(si);
                        stateRegistrations.add(sr);

                        // per-device states
                        for (ExternalDeviceConfiguration edc : homeConfig.deviceConfigurations.get("lighting")) {
                            if ((boolean) (((Map<String, Object>) (edc.resourcesConfig.get("lighting")))
                                    .get("onOff")) == true) {
                                sr = new StateRegistration();
                                si = new StateIdentifier();
                                si.setHome(homeId);
                                si.setDevice(new ObjectId(edc.deviceId));
                                si.setResource("lighting");
                                si.setStateName("isOn");
                                sr.setStateIdentifier(si);
                                stateRegistrations.add(sr);
                            }

                            if ((boolean) (((Map<String, Object>) (edc.resourcesConfig.get("lighting")))
                                    .get("dimmable")) == true) {
                                sr = new StateRegistration();
                                si = new StateIdentifier();
                                si.setHome(homeId);
                                si.setDevice(new ObjectId(edc.deviceId));
                                si.setResource("lighting");
                                si.setStateName("dimmerLevel");
                                sr.setStateIdentifier(si);
                                stateRegistrations.add(sr);
                            }
                        }

                        // per-room states
                        // our permissions in SCS allow us to see only rooms w/lights
                        for (String roomId : homeConfig.roomConfigurations.keySet()) {
                            sr = new StateRegistration();
                            si = new StateIdentifier();
                            si.setHome(homeId);
                            si.setRoom(new ObjectId(roomId));
                            si.setStateName("LightsOn");
                            sr.setStateIdentifier(si);
                            stateRegistrations.add(sr);
                        }
                    }

                    boolean saveHomeConfig = true;

                    if (stateRegistrations.size() > 0) {
                        // send the state registration request.
                        synchronized (client) {
                            invocation = client.target(scsEndpoint)
                                    .path("api/v1/states/subscriptions/subscribe")
                                    .request().header("Sendal3PSSId", scs3pssId)
                                    .buildPost(Entity.json(stateRegistrations));
                        }

                        Response stateRegistrationResponse = null;

                        try {
                            stateRegistrationResponse = invocation.invoke(Response.class);

                            if (stateRegistrationResponse.getStatus() != Status.OK.getStatusCode()) {
                                saveHomeConfig = false;
                                logger.error("Cannot subscribe for states -  " + stateRegistrationResponse.getStatus()
                                        + " - " + stateRegistrationResponse.readEntity(String.class));
                                response = Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .entity("Error while subscribing for states - "
                                                + stateRegistrationResponse.getStatus())
                                        .type(MediaType.TEXT_PLAIN).build();
                            }

                        } catch (Exception exception) {
                            // Output expected ConnectException.
                            saveHomeConfig = false;
                            logger.error("Register state exception - " + exception);
                            response = Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .entity("Exception while subscribing for states - " + exception)
                                    .type(MediaType.TEXT_PLAIN).build();
                        } finally {
                            if (stateRegistrationResponse != null) {
                                stateRegistrationResponse.close();
                            }
                        }
                    }

                    if (saveHomeConfig == true) {
                        lightTrackerSubscriptionRecordDao.updateHomeSubscription(ltSubRec);
                    }
                } else {
                    // no home config was retrieved. this is an error
                    String responseBody = homeConfigResponse.readEntity(String.class);

                    logger.error(
                            "Cannot retrieve home config -  " + homeConfigResponse.getStatus() + " - " + responseBody);

                    response = Response.status(homeConfigResponse.getStatus()).entity(responseBody)
                            .type(MediaType.TEXT_PLAIN).build();

                }
            } catch (Exception exception) {
                // Output expected ConnectException.
                logger.error("Get home config exception - " + exception);
                response = Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Exception while sending home config request - " + exception).type(MediaType.TEXT_PLAIN)
                        .build();
            } finally {
                if (homeConfigResponse != null) {
                    homeConfigResponse.close();
                }
            }
        } else {
            logger.error("Home is unknown to LT - " + homeId);
        }

        return response;
    }

}
