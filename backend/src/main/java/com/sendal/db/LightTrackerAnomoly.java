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

import org.bson.codecs.pojo.annotations.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.sendal.common.StateIdentifier;
import com.sendal.common.StateReport;
import com.sendal.externalapicommon.ExternalHomeConfiguration;

import com.sendal.lighttracker.LightTrackerConfiguration;
import com.sendal.lighttracker.db.dao.LightTrackerAnomolyDAO;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LightTrackerAnomoly implements Serializable {

    public static final String NEXTACTION_INITIALANOMOLYTEST = "InitialAnomolyTest";
    public static final String NEXTACTION_ALERTUSER = "AlertUser";
    private final static Logger logger = LoggerFactory.getLogger(LightTrackerAnomoly.class);

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
    private static final int INITIALEVALUATION_DELAY_SECONDS = 30; //120;
    private static final int LOOKAHEAD_MINUTES = 45; // the timeframe to check the ML model
    private static final int WAITBEFORENOTIFICATION_SECONDS =  20 * SECONDS_PER_MINUTE; // when to alert the user if the light is still on
    private static final int ERROR_BACKOFF_DELAY_SECONDS = 10;


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


    private String nextAction;
    
    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    StateIdentifier stateIdentifier;

    public StateIdentifier getStateIdentifier() {
        return stateIdentifier;
    }

    public void setStateIdentifier(StateIdentifier stateIdentifier) {
        this.stateIdentifier = stateIdentifier;
    }

    // in seconds, not mS
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
        timeOfNextAction = (currentDate.getTime() / 1000) + secondsInFuture;
    }

    // used for atomic timer operations
    @JsonIgnore
    public double olock;

    //
    // Anomoly Logic Handlers
    //

    static public void lightStateUpdate(StateReport updatedState, LightTrackerAnomolyDAO lightTrackerAnomolyDao) {
        if (updatedState.getStateValue().equals("0")) {
            logger.info("Anomoly detection light off " + updatedState.getStateIdentifier().getDevice());
            lightTrackerAnomolyDao
                    .deleteLightTrackerAnomoly(updatedState.getStateIdentifier().getDevice());
        } else {
            logger.info("Anomoly detection light on " + updatedState.getStateIdentifier().getDevice());

            // the light is on. We create a new anomoly state detector. 
            // due to configuration updates there's a chance the light is already on.
            if(lightTrackerAnomolyDao.getLightTrackerAnomoly(updatedState.getStateIdentifier().getDevice()) == null) {
                LightTrackerAnomoly ltAnomoly = new LightTrackerAnomoly();
                ltAnomoly.setDeviceId(updatedState.getStateIdentifier().getDevice());
                ltAnomoly.setHomeId(updatedState.getStateIdentifier().getHome());
                ltAnomoly.setStateIdentifier(updatedState.getStateIdentifier());
                ltAnomoly.setNextAction(LightTrackerAnomoly.NEXTACTION_INITIALANOMOLYTEST);
                // don't evaluate right away in case the light is turned off quickly.
                ltAnomoly.setTimeOfNextActionSecondsInFuture(INITIALEVALUATION_DELAY_SECONDS);
                lightTrackerAnomolyDao.createLightTrackerAnomoly(ltAnomoly);
            } else {
                logger.warn("Light on entry already exists for " + updatedState.getStateIdentifier().getDevice());
            }
        }
    }


    public void timerExpired(Client client, LightTrackerConfiguration configuration, LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao, LightTrackerAnomolyDAO lightTrackerAnomoloyDao) {
        logger.debug("Checking for update on record " + deviceId.toString());

        boolean deleteRecord = true;

        // check the anomoly state.
        if (nextAction.equals(NEXTACTION_INITIALANOMOLYTEST)) {
            // here we run ML for now and the next 3 timeslots to see if we believe the
            // light should be off
            LightTrackerSubscriptionRecord homeSubscription = lightTrackerSubscriptionRecordDao
                    .getHomeSubscription(homeId);
            String timeZone = "America/New_York";
            Invocation invocation;
            Response response = null;

            if (homeSubscription != null && homeSubscription.getHomeConfig().timeZone != null) {
                timeZone = homeSubscription.getHomeConfig().timeZone;
            } else {
                logger.warn("using default time zone");
            }

            List<Float> timeslotProbabilities = null;

            synchronized (client) {
                invocation = client.target(configuration.getSendalSoftwareService().getScsUrl())
                        .path("/api/v1/homes/" + homeId.toString() + "/devices/" + deviceId.toString()
                                + "/models/lighting/on")
                        .queryParam("timezone", timeZone).queryParam("numberofminutes", LOOKAHEAD_MINUTES)
                        // no starttimer param means use the current time.
                        .request().header("Sendal3PSSId", configuration.getSendalSoftwareService().getScs3pssId())

                        .buildGet();
            }

            try {
                response = invocation.invoke(Response.class);

                if (response.getStatus() == Status.OK.getStatusCode()) {
                    timeslotProbabilities = response.readEntity(new GenericType<List<Float>>() {
                    });
                } else {
                    String responseBody = response.readEntity(String.class);
                    logger.warn("Requesting model run -  " + response.getStatus() + " - " + responseBody);
                }
            } catch (Exception exception) {
                // Output expected ConnectException.
                logger.error("Model run notification exception - " + exception);
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
                        logger.debug("Timeslot index " + index + " probability " + timeslotProbabilities.get(index));
                        if (timeslotProbabilities.get(index) > 0.05) {
                            highestIndexOfHighProbability = index;
                        }
                    }

                    if (highestIndexOfHighProbability == -1) {
                        logger.info("Light on unexpectedly " + deviceId.toString());

                        // we feel the light should not be on, we check again in 15m
                        setNextAction(LightTrackerAnomoly.NEXTACTION_ALERTUSER);
                        setTimeOfNextActionSecondsInFuture(WAITBEFORENOTIFICATION_SECONDS);

                        // we could check further in the future and start a shorter timer too to be more
                        // tolerant
                    } else {
                        // the light should be on according to the near term view.
                        // we'll check again later
                        logger.info("Light on expected " + deviceId.toString());

                        // the light on is not unusual, but poll it again later on in case it's left on too long
                        setTimeOfNextActionSecondsInFuture(SECONDS_PER_MINUTE * (highestIndexOfHighProbability + 1));
                    }

                    deleteRecord = false;
                } else {
                    // empty data was returned, not sure what to do in this case
                    logger.warn("empty ML data returned");
                }
            } else {
                // some type of error occured, likely something bad/permenant
                logger.error("no ML data returned");
            }
        } else if (nextAction.equals(LightTrackerAnomoly.NEXTACTION_ALERTUSER)) {
            // since we got here it means the initial check thinks the next 3 or so timeslots should be unlikely to have a light on.  
            // sent a notification.
            logger.info("Sending a notification!");
            Invocation invocation;
            Response response = null;

            List<UserNotification> userNotifications = new ArrayList<UserNotification>();
            UserNotification un = new UserNotification();

             Map<String, String> alertHeading = new HashMap<String, String>();
             alertHeading.put("en", "Light Left On");

             Map<String, String> alertContent = new HashMap<String, String>();

            // look up the home config to get the name of the light.
             String deviceReference = "unknown - " +deviceId.toString();

            LightTrackerSubscriptionRecord homeSubscription = lightTrackerSubscriptionRecordDao.getHomeSubscription(homeId);
            if(homeSubscription != null) {
                ExternalHomeConfiguration homeconfig = homeSubscription.getHomeConfig();
                if(homeconfig != null) {
                    List<ExternalDeviceConfiguration> deviceConfigurations = homeconfig.deviceConfigurations.get("lighting");
                    if(deviceConfigurations != null) {
                        String deviceIdString = deviceId.toString();
                        for(ExternalDeviceConfiguration edc : deviceConfigurations) {
                            if(deviceIdString.equals(edc.deviceId)) {
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
                                .header("Sendal3PSSId", configuration.getSendalSoftwareService().getScs3pssId())
                                .buildPost(javax.ws.rs.client.Entity.json(userNotifications));
            }

            try {
                response = invocation.invoke(Response.class);

                if(response.getStatus() == Status.OK.getStatusCode()) {
                    logger.debug("Successfully sent notification");
                } else {
                    String responseBody = response.readEntity(String.class);
                    logger.error("Failed to request user notification -  " + response.getStatus() + " - " + responseBody);
                    setTimeOfNextActionSecondsInFuture(ERROR_BACKOFF_DELAY_SECONDS);
                    deleteRecord = false; // may be aggressive, but we'll retry on every tick.
                }
            } catch (Exception exception) {
                // Output expected ConnectException.
                logger.error("Request notification exception - " + exception);
                setTimeOfNextActionSecondsInFuture(ERROR_BACKOFF_DELAY_SECONDS);
                deleteRecord = false; // may be aggressive, but we'll retry on every tick.
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }

        if (deleteRecord == true) {
            lightTrackerAnomoloyDao.deleteLightTrackerAnomoly(deviceId);
        } else {
            double previousolock = olock;

            olock = 0;
            lightTrackerAnomoloyDao.updateLightTrackerAnomoly(deviceId, previousolock,
                    olock, nextAction, timeOfNextAction);
        }
    }
}
