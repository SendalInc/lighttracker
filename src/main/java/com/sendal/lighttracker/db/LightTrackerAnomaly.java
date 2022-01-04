package com.sendal.lighttracker.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;
import org.bson.types.ObjectId;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sendal.common.coredb.DBPermissionsConfiguration;
import com.sendal.common.notification.UserNotification;
import com.sendal.externalapicommon.ExternalHomeConfiguration;
import com.sendal.externalapicommon.ExternalDeviceConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.sendal.common.pss.PSSUtils;

import org.bson.codecs.pojo.annotations.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.sendal.common.state.StateIdentifier;
import com.sendal.common.state.StateReport;
import com.sendal.externalapicommon.ExternalHomeConfiguration;

import com.sendal.lighttracker.LightTrackerConfiguration;
import com.sendal.lighttracker.db.dao.LightTrackerAnomalyDAO;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LightTrackerAnomaly implements Serializable {

    public static final String STATE_IDLE = "Idle"; // never stored, if idle the anomaly state is removed
    public static final String STATE_ERROR_NO_MLDATA = "ErrorNoMlData"; // never stored, if idle the anomaly state is removed
    public static final String STATE_INITIALANOMALYTEST = "InitialAnomalyTestWait";
    public static final String STATE_LIGHTONNORMAL = "LightOnNormal";
    public static final String STATE_LIGHTPENDINGANOMALY = "LightPendingAnomaly";
    public static final String STATE_LIGHTONANOMALY = "LightOnAnomaly";
    public static final String STATE_LIGHTONANOMALYIGNORE = "LightOnAnomalyUserIgnore";

    private final static Logger logger = LoggerFactory.getLogger(LightTrackerAnomaly.class);

    private static final int SECONDS_PER_MINUTE = 60;

    // when LOOKAHED_MINUTES > WAITBEFORENOTIFICATION_SECONDS (in minutes) then we
    // will check longer into the future for the light to be off
    // and alert earlier. i.e. if the light is not supposed to be on for the next 45
    // minutes then alert the user after 20 minutes of the light being on.
    // This helps to handle cases where someone turns on a light a bit earlier than
    // usual.
    
/*    
     //debug values 
     private static final int INITIALEVALUATION_DELAY_SECONDS = 2;
     private static final int LOOKAHEAD_MINUTES = 2;
     private static final int WAITBEFORENOTIFICATION_SECONDS = 10;
     private static final int ERROR_BACKOFF_DELAY_SECONDS = 10;
 */   

    // production values
    private static final int INITIALEVALUATION_DELAY_SECONDS = 30;
    private static final int INITIALEVALUATION_ERROR_RETRY_SECONDS = 60;
    private static final int LOOKAHEAD_MINUTES = 45; // the timeframe to check the ML model
    private static final int WAITBEFORENOTIFICATION_SECONDS =  20 * SECONDS_PER_MINUTE; // when to alert the user if the light is still on
    private static final int ERROR_BACKOFF_DELAY_SECONDS = 10;
    private static final int NOTIFICATION_ERROR_BACKOFF_DELAY_SECONDS = 15;

    public LightTrackerAnomaly() {
        super();
    }

    public LightTrackerAnomaly(LightTrackerAnomaly parentAnomaly) {
        super();
        this.setDeviceId(parentAnomaly.getDeviceId());
        this.setHomeId(parentAnomaly.getHomeId());
        this.setAnomalyState(parentAnomaly.getAnomalyState());
        this.setTimeOfNextAction(parentAnomaly.getTimeOfNextAction());
        this.setLightOnTime(parentAnomaly.getLightOnTime());
    }

    @BsonId
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId deviceId;

    public ObjectId getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(ObjectId deviceId) {
        this.deviceId = deviceId;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId homeId;

    public ObjectId getHomeId() {
        return homeId;
    }

    public void setHomeId(ObjectId homeId) {
        this.homeId = homeId;
    }

    private String anomalyState;

    public String getAnomalyState() {
        return anomalyState;
    }

    public void setAnomalyState(String anomalyState) {
        this.anomalyState = anomalyState;
    }

    // in mS
    private Long timeOfNextAction;

    public Long getTimeOfNextAction() {
        return timeOfNextAction;
    }

    public void setTimeOfNextAction(Long timeOfNextAction) {
        this.timeOfNextAction = timeOfNextAction;
    }

    @JsonIgnore
    private void setTimeOfNextActionSecondsInFuture(long secondsInFuture) {
        Date currentDate = new Date();
        timeOfNextAction = currentDate.getTime() + (secondsInFuture * 1000);
    }

    // in mS
    private Date lightOnTime;

    public Date getLightOnTime() {
        return lightOnTime;
    }

    public void setLightOnTime(Date lightOnTime) {
        this.lightOnTime = lightOnTime;
    }

    // used for atomic timer operations
    @JsonIgnore
    public double olock; // in milliseconds

    //
    // Anomaly Logic Handlers
    //

    static public void lightStateUpdate(StateReport updatedState, Client client, LightTrackerAnomalyDAO lightTrackerAnomalyDao, String scsEndpoint) {
        if (updatedState.getStateValue().equals("0")) {
            logger.info("Anomaly detection light off " + updatedState.getStateIdentifier().getDevice());
            lightTrackerAnomalyDao
                    .deleteLightTrackerAnomaly(updatedState.getStateIdentifier().getDevice());

            PSSUtils.updateAppState(client,
                    scsEndpoint, logger,
                    updatedState.getStateIdentifier().getHome(), String.valueOf(System.currentTimeMillis()));
        } else {
            logger.info("Anomaly detection light on " + updatedState.getStateIdentifier().getDevice());

            // the light is on. We create a new anomaly state detector. 
            // due to configuration updates there's a chance the light is already on.
            if(lightTrackerAnomalyDao.getLightTrackerAnomaly(updatedState.getStateIdentifier().getDevice()) == null) {
                LightTrackerAnomaly ltAnomaly = new LightTrackerAnomaly();
                ltAnomaly.setDeviceId(updatedState.getStateIdentifier().getDevice());
                ltAnomaly.setHomeId(updatedState.getStateIdentifier().getHome());
                ltAnomaly.setLightOnTime(updatedState.getLastUpdate());
                ltAnomaly.setAnomalyState(STATE_INITIALANOMALYTEST);
                // don't evaluate right away in case the light is turned off quickly.
                ltAnomaly.setTimeOfNextActionSecondsInFuture(INITIALEVALUATION_DELAY_SECONDS);
                lightTrackerAnomalyDao.createLightTrackerAnomaly(ltAnomaly);
            } else {
                logger.warn("Light on entry already exists for " + updatedState.getStateIdentifier().getDevice());
            }
        }

        updateBadgeForAnomolies(client, updatedState.getStateIdentifier().getHome(), lightTrackerAnomalyDao,
                scsEndpoint);
    }

    static public void updateBadgeForAnomolies(Client client, ObjectId homeId,
            LightTrackerAnomalyDAO lightTrackerAnomalyDao, String scsEndpoint) {
        String appBadge = PSSUtils.APPBADGE_GREEN;
        String appBadgeIcon = PSSUtils.APPBADGEICON_NONE;

        List<LightTrackerAnomaly> anomalies = lightTrackerAnomalyDao.getHouseActiveAnomalies(homeId);

        if (anomalies.size() > 0) {
            appBadge = PSSUtils.APPBADGE_YELLOW;
            appBadgeIcon = PSSUtils.APPBADGEICON_EXCLAMATION;
        }

        PSSUtils.updateBadgeForHome(client, scsEndpoint, logger, homeId, appBadge, appBadgeIcon);
    }

    public void timerExpired(Client client, LightTrackerConfiguration configuration, LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao, LightTrackerAnomalyDAO lightTrackerAnomalyDao) {
        logger.debug("Checking for update on record " + deviceId.toString());

        setTimeOfNextAction(0L); // start by assuming there's no next action.

        // check the anomaly state.
        switch (anomalyState) {
            case STATE_INITIALANOMALYTEST: 
            case STATE_LIGHTONNORMAL: // we get here if the light has been evaluated and this is the first low probability time, so we need to reevaluate
            {
                LightTrackerSubscriptionRecord homeSubscription = lightTrackerSubscriptionRecordDao
                        .getHomeSubscription(homeId);
                String timeZone = "America/New_York";
                Invocation invocation;
                Response response = null;
                boolean retryOnError = false;

                if (homeSubscription != null && homeSubscription.getHomeConfig().timeZone != null) {
                    timeZone = homeSubscription.getHomeConfig().timeZone;
                } else {
                    logger.warn("using default time zone");
                }

                List<Float> timeslotProbabilities = null;

                synchronized (client) {
                    invocation = client.target(configuration.getSendalSoftwareService().getScsUrl())
                            .path("/api/v1/devices/" + deviceId.toString()
                                    + "/models/lighting/on")
                            .queryParam("timezone", timeZone).queryParam("numberofminutes", LOOKAHEAD_MINUTES)
                            // no starttimer param means use the current time.
                            .request()
                            .buildGet();
                }

                try {
                    response = invocation.invoke(Response.class);

                    if (response.getStatus() == Status.OK.getStatusCode()) {
                        timeslotProbabilities = response.readEntity(new GenericType<List<Float>>() {
                        });
                    } else {
                        String responseBody = response.readEntity(String.class);
                        logger.info("Requesting model run -  " + response.getStatus() + " - " + responseBody);

                        if (response.getStatusInfo().getFamily() == Status.Family.SERVER_ERROR) {
                            retryOnError = true;
                        }
                    }
                } catch (Exception exception) {
                    // Output expected ConnectException.
                    logger.error("Model run notification exception - " + exception);
                    retryOnError = true;
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }

                if (timeslotProbabilities != null) {
                    if (timeslotProbabilities.size() > 0) {
                        // good results.
                        int highestIndexOfHighProbability = -1;

                        for (int index = 0; index < timeslotProbabilities.size(); index++) {
                            logger.debug(
                                    "Timeslot index " + index + " probability " + timeslotProbabilities.get(index));
                            if (timeslotProbabilities.get(index) > 0.05) {
                                highestIndexOfHighProbability = index;
                            }
                        }

                        if (highestIndexOfHighProbability == -1) {
                            logger.info("Light on unexpectedly " + deviceId.toString());

                            // we feel the light should not be on, we give some delay before notifying
                            setAnomalyState(STATE_LIGHTPENDINGANOMALY);
                            setTimeOfNextActionSecondsInFuture(WAITBEFORENOTIFICATION_SECONDS);
                            // we could check further in the future and start a shorter timer too to be more
                            // tolerant
                        } else {
                            // the light should be on according to the near term view.
                            // we'll check again later
                            logger.info("Light on expected " + deviceId.toString());

                            // the light on is not unusual, but poll it again later on in case it's left on
                            // too long
                            setAnomalyState(STATE_LIGHTONNORMAL);

                            setTimeOfNextActionSecondsInFuture(SECONDS_PER_MINUTE * (highestIndexOfHighProbability + 1));
                        }
                    } else {
                        // empty data was returned, not sure what to do in this case
                        logger.warn("empty ML data returned");
                        setAnomalyState(STATE_ERROR_NO_MLDATA);
                        setTimeOfNextActionSecondsInFuture(WAITBEFORENOTIFICATION_SECONDS);
                    }
                } else {
                    // some type of error occured, likely something bad/permenant
                    logger.warn("no ML data returned for device " + deviceId);

                    if (retryOnError == true) {
                        setTimeOfNextActionSecondsInFuture(INITIALEVALUATION_ERROR_RETRY_SECONDS);
                    } else {
                        setAnomalyState(STATE_ERROR_NO_MLDATA);
                        setTimeOfNextActionSecondsInFuture(WAITBEFORENOTIFICATION_SECONDS);
                    }
                }
            }
            break;

            case STATE_IDLE:
                logger.error("Anomaly in Idle state reports timer event " + deviceId);
                break;

            case STATE_ERROR_NO_MLDATA:
            case STATE_LIGHTPENDINGANOMALY:
                // since we got here it means the initial check thinks the next 3 or so timeslots should be unlikely to have a light on.  
                // sent a notification.              
                setAnomalyState(STATE_LIGHTONANOMALY);
                sendAnomalyUserNotification(client, configuration, lightTrackerSubscriptionRecordDao,
                        lightTrackerAnomalyDao);

                PSSUtils.updateAppState(client,
                        configuration.getSendalSoftwareService()
                                .getScsUrl(), logger,
                        getHomeId(), String.valueOf(System.currentTimeMillis()));
                break;

            case STATE_LIGHTONANOMALY:
                // a timer here is due to failed to send the notification previously
                sendAnomalyUserNotification(client, configuration, lightTrackerSubscriptionRecordDao,
                        lightTrackerAnomalyDao);
                break;

            case STATE_LIGHTONANOMALYIGNORE:
                break;
        }

        if (getAnomalyState().equals(STATE_IDLE)) {
            lightTrackerAnomalyDao.deleteLightTrackerAnomaly(deviceId);
        } else {
            // clear the object lock
            double previousolock = olock;

            olock = 0;
            lightTrackerAnomalyDao.updateLightTrackerAnomaly(deviceId, previousolock,
                    olock, anomalyState, timeOfNextAction);
        }

        updateBadgeForAnomolies(client, getHomeId(), lightTrackerAnomalyDao,
                configuration.getSendalSoftwareService().getScsUrl());
    }

    private void sendAnomalyUserNotification(Client client, LightTrackerConfiguration configuration,
            LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao,
            LightTrackerAnomalyDAO lightTrackerAnomalyDao) {
        logger.info("Sending a notification for device " + deviceId);
        Invocation invocation;
        Response response = null;

        List<UserNotification> userNotifications = new ArrayList<UserNotification>();
        UserNotification un = new UserNotification();

        Map<String, String> alertHeading = new HashMap<String, String>();
        alertHeading.put("en", "Light Left On");

        Map<String, String> alertContent = new HashMap<String, String>();

        // look up the home config to get the name of the light.
        String deviceReference = "unknown - " + deviceId.toString();

        LightTrackerSubscriptionRecord homeSubscription = lightTrackerSubscriptionRecordDao.getHomeSubscription(homeId);
        if (homeSubscription != null) {
            ExternalHomeConfiguration homeconfig = homeSubscription.getHomeConfig();
            if (homeconfig != null) {
                List<ExternalDeviceConfiguration> deviceConfigurations = homeconfig.deviceConfigurations
                        .get("lighting");
                if (deviceConfigurations != null) {
                    String deviceIdString = deviceId.toString();
                    for (ExternalDeviceConfiguration edc : deviceConfigurations) {
                        if (deviceIdString.equals(edc.deviceId)) {
                            deviceReference = edc.deviceName;
                        }
                    }
                }
            }
        }

        alertContent.put("en", "You may have left your " + deviceReference + " light on by mistake.");

        un.setAlertHeading(alertHeading);
        un.setAlertContent(alertContent);
        userNotifications.add(un);

        synchronized (client) {
            invocation = client
                    .target(configuration.getSendalSoftwareService().getScsUrl())
                    .path("/api/v1/homes/" + homeId.toString() + "/notifications/send")
                    .request()
                    .buildPost(javax.ws.rs.client.Entity.json(userNotifications));
        }

        try {
            response = invocation.invoke(Response.class);

            if (response.getStatus() == Status.OK.getStatusCode()) {
                logger.debug("Successfully sent notification");
            } else {
                String responseBody = response.readEntity(String.class);
                logger.error("Failed to request user notification -  " + response.getStatus() + " - " + responseBody);
                if (response.getStatusInfo().getFamily() == Status.Family.SERVER_ERROR) {
                    setTimeOfNextActionSecondsInFuture(NOTIFICATION_ERROR_BACKOFF_DELAY_SECONDS);
                }
            }
        } catch (Exception exception) {
            // Output expected ConnectException.
            logger.error("Request notification exception - " + exception);
            setTimeOfNextActionSecondsInFuture(NOTIFICATION_ERROR_BACKOFF_DELAY_SECONDS);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    // assumes the object is locked in the DB
    public boolean setAnomalyIgnore(Client client, String scsEndpoint,
            LightTrackerAnomalyDAO lightTrackerAnomalyDao) {
        boolean success = false;

        if (getAnomalyState().equals(STATE_LIGHTONANOMALY)) {
            setAnomalyState(STATE_LIGHTONANOMALYIGNORE);
            success = true;
        } else if (getAnomalyState().equals(STATE_LIGHTONANOMALYIGNORE)) {
            success = true;
        }

        // update the database, release the lock, and update UI states.
        lightTrackerAnomalyDao.updateLightTrackerAnomaly(
                deviceId, olock, 0, getAnomalyState(), getTimeOfNextAction());

        PSSUtils.updateAppState(client,
                scsEndpoint, logger,
                getHomeId(), String.valueOf(System.currentTimeMillis()));

        updateBadgeForAnomolies(client, getHomeId(), lightTrackerAnomalyDao,
                scsEndpoint);

        return success;
    }
}
