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

import com.sendal.common.state.StateIdentifier;
import com.sendal.common.state.StateValue;
import com.sendal.common.olock.OLockFailureException;

import io.dropwizard.auth.Auth;
import org.bson.types.ObjectId;

import io.dropwizard.client.JerseyClientBuilder;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;
import java.util.HashSet;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sendal.lighttracker.pojo.LightTrackerStateHistory;
import com.sendal.lighttracker.pojo.LightTrackerReportedAnomaly;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;
import com.sendal.lighttracker.db.dao.LightTrackerAnomalyDAO;
import com.sendal.lighttracker.db.LightTrackerAnomaly;

import com.sendal.lighttracker.pojo.LightTrackerUsageSummary;

import com.sendal.common.coredb.DBPermissions;
import com.sendal.common.state.StateReport;

import com.sendal.externalapicommon.security.IDPrincipal;
import com.sendal.externalapicommon.ExternalHomeConfiguration;
import com.sendal.externalapicommon.ExternalDeviceConfiguration;
import com.sendal.externalapicommon.ExternalRoomConfiguration;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.TimeZone;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

import com.codahale.metrics.annotation.*;

// this interface is accessed directly by IS supporting the UI
@Timed
@ResponseMetered
@ExceptionMetered
@Path("/api/v1/homes/{homeid}/passthrough")
@Produces(MediaType.APPLICATION_JSON)
public class LightTrackerIntegrationService {
    private final Client client;
    private final Validator validator;
    private final LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao;
    private final LightTrackerAnomalyDAO lightTrackerAnomalyDao;
    private final String scsEndpoint;
    private final String scs3pssId;
    private final Logger logger = LoggerFactory.getLogger(LightTrackerIntegrationService.class);


    public LightTrackerIntegrationService(Client client, LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao, LightTrackerAnomalyDAO lightTrackerAnomalyDao, String scsEndpoint, String scs3pssId, Validator validator) {
        this.client = client;
        this.validator = validator;
        this.scsEndpoint = scsEndpoint;
        this.scs3pssId = scs3pssId;
        this.lightTrackerSubscriptionRecordDao = lightTrackerSubscriptionRecordDao;
        this.lightTrackerAnomalyDao = lightTrackerAnomalyDao;
    }

    @GET
    @Path("/usagesummary")
    public Response lightUsageSummary(@Auth IDPrincipal principal, @PathParam("homeid") ObjectId homeId) {
        Response response;

        // verify the home is subscribed.
        LightTrackerSubscriptionRecord subRecord = lightTrackerSubscriptionRecordDao.getHomeSubscription(homeId);
        if(subRecord != null) {
            List<LightTrackerUsageSummary> usageSummary = new ArrayList<LightTrackerUsageSummary>();

            // iterate through each device calculating usage information.
            List<ExternalDeviceConfiguration> devicesConfig = subRecord.getHomeConfig().deviceConfigurations.get("lighting");

            if(devicesConfig != null) {
                // form a single query for 30 days of historical data for all the devices. 
                List<String> deviceIds = new ArrayList<String>();

                for(ExternalDeviceConfiguration deviceConfig : devicesConfig) {
                    deviceIds.add(deviceConfig.deviceId);
                }

                String timeZone = "UTC"; //"America/New_York";

                TimeZone tz = TimeZone.getTimeZone(timeZone);
                long currentTimestamp = System.currentTimeMillis();
                Calendar cal = Calendar.getInstance(tz);

                // month usage
                cal.setTimeInMillis(currentTimestamp);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                // cal.set(Calendar.DATE,1);
                // we query 30+5 days back, which lets us get light stats before the range (in case a light is left on overnight)
                cal.add(Calendar.DATE, -35);

                Date queryDate = cal.getTime();
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                dateFormatter.setTimeZone(tz);
                String queryStrDate = dateFormatter.format(queryDate); 

                Invocation invocation;
                Response bqResponse = null;

                List<LightTrackerStateHistory> stateHistory = null;

                String[] deviceArray = new String[deviceIds.size()];
                deviceIds.toArray(deviceArray);

                synchronized (client) {
                    invocation = client.target(scsEndpoint)
                            .path("/api/v1/states/history/devices")
                            .queryParam("homeid", homeId)
                            .queryParam("deviceid", deviceArray)                             
                            .queryParam("resource", "lighting")
                            .queryParam("statename", "isOn")                             
                            .queryParam("from", queryStrDate)
                            .request()
                            .buildGet();
                }

                try {
                    bqResponse = invocation.invoke(Response.class);

                    if (bqResponse.getStatus() == Status.OK.getStatusCode()) {
                        stateHistory = bqResponse.readEntity(new GenericType<List<LightTrackerStateHistory>>() {
                        });
                    } else {
                        String responseBody = bqResponse.readEntity(String.class);
                        logger.warn("Requesting BQ states -  " + bqResponse.getStatus() + " - " + responseBody);
                    }
                } catch (Exception exception) {
                    // Output expected ConnectException.
                    logger.error("Big Query exception - " + exception);
                    exception.printStackTrace();

                } finally {
                    if (bqResponse != null) {
                        bqResponse.close();
                    }
                }

                if(stateHistory != null) {
                    if (subRecord.getHomeConfig().timeZone != null) {
                        timeZone = subRecord.getHomeConfig().timeZone;
                    } else {
                        timeZone = "America/New_York";
                        logger.warn("using default time zone for usage summary");
                    }

                    tz = TimeZone.getTimeZone(timeZone);
                    cal = Calendar.getInstance(tz);

                    for (ExternalDeviceConfiguration deviceConfig : devicesConfig) {
                        LightTrackerUsageSummary ltus = new LightTrackerUsageSummary(deviceConfig.deviceId,
                                deviceConfig.deviceName);

                        // get a baseline time reference
                        long totalMillis = 0;

                        // daily usage
                        cal.setTimeInMillis(currentTimestamp);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);

                        totalMillis = calculateLightOnTime(cal, currentTimestamp, deviceConfig.deviceId, stateHistory);
                        ltus.setUsageDaySeconds(totalMillis / 1000);

                        // invoke the predictive model and determine the next hour's likelihood of being
                        // on.
                        // determine the timeslot and day of week for the next hour.
                        // the predictive model uses 5 minute timeslots.
                        long finalProbability = -1;

                        Response mlResponse = null;
                        List<Float> timeslotProbabilities = null;

                        synchronized (client) {
                            invocation = client.target(scsEndpoint)
                                    .path("/api/v1/devices/" + deviceConfig.deviceId
                                            + "/models/lighting/on")
                                    .queryParam("timezone", timeZone)
                                    .queryParam("numberofminutes", "60")
                                    // no starttimer param means use the current time.
                                    .request().buildGet();
                        }

                        try {
                            mlResponse = invocation.invoke(Response.class);

                            if (mlResponse.getStatus() == Status.OK.getStatusCode()) {
                                timeslotProbabilities = mlResponse.readEntity(new GenericType<List<Float>>() {
                                });
                            } else {
                                String responseBody = mlResponse.readEntity(String.class);
                                logger.warn("Requesting model run -  " + mlResponse.getStatus() + " - " + responseBody);
                            }
                        } catch (Exception exception) {
                            // Output expected ConnectException.
                            logger.error("Model run notification exception - " + exception);
                        } finally {
                            if (mlResponse != null) {
                                mlResponse.close();
                            }
                        }

                        if (timeslotProbabilities != null) {
                            if (timeslotProbabilities.size() > 0) {
                                float nextHourProbability = 0;

                                // average the return values.
                                for (Float rtn : timeslotProbabilities) {
                                    nextHourProbability += rtn;
                                }

                                nextHourProbability = nextHourProbability / 60.0f;
                                finalProbability = Math.round(nextHourProbability * 100.0f);

                                logger.debug("Next hour probability for " + deviceConfig.deviceId + " = "
                                        + nextHourProbability);
                            } else {
                                logger.error("empty ML data returned");
                            }
                        } else {
                            logger.error("no ML data returned");
                        }

                        ltus.setNextHourOnProbability(finalProbability);

                        // weekly usage
                        cal.setTimeInMillis(currentTimestamp);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        cal.add(Calendar.DATE, -7);

                        /*
                         * int currentDayOfWeek= cal.get(Calendar.DAY_OF_WEEK);
                         * 
                         * if(currentDayOfWeek > 1) { cal.add(Calendar.DATE, -1*(currentDayOfWeek-1)); }
                         */

                        
                        totalMillis = calculateLightOnTime(cal, currentTimestamp, deviceConfig.deviceId, stateHistory);
                        ltus.setUsageWeekSeconds(totalMillis / 1000);
                        ltus.setWeekAverageSeconds((totalMillis / 1000) / 7);

                        // month usage
                        cal.setTimeInMillis(currentTimestamp);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        // cal.set(Calendar.DATE,1);
                        cal.add(Calendar.DATE, -30);

                        totalMillis = calculateLightOnTime(cal, currentTimestamp, deviceConfig.deviceId, stateHistory);
                        ltus.setUsageMonthSeconds(totalMillis / 1000);
                        ltus.setThirtyDayAverageSeconds((totalMillis / 1000) / 30);

                        // add the light entry
                        usageSummary.add(ltus);
                    }
                } else {
                    logger.error("No BQ History for home - " + homeId);
                }

            } else {
                logger.error("No devices configured fr home - " + homeId);
            }

            response = Response.ok(usageSummary).build();

        } else {
            response = Response.status(Status.NOT_FOUND).entity("Home is not subscribed to the light tracker service.")
                    .type(MediaType.TEXT_PLAIN).build();
        }
        return response;
    }


    private long calculateLightOnTime(Calendar cal, long currentTimestamp, String deviceId, List<LightTrackerStateHistory> records) {
        long totalMillis = 0;
        long startTimestamp = cal.toInstant().getEpochSecond()*1000; // in millis
        Date startDate = cal.getTime();
        Integer startingState = null;
        Date lightOnTime = null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        boolean previousReportsFound = false;

        // as we scale one can envision more efficent ways to process the data.  The main constraint is making a single call to 
        // SCS for all devices data, which will comingle many devices data into the one array.
        if(records != null) {
            for (LightTrackerStateHistory record : records) {
                if (record.device.equals(deviceId) == true) {
                    // is it in our test time range?
                    Date date = null;
                    
                    try {
                        date = simpleDateFormat.parse(record.timestamp);
                    } catch (ParseException e) {
                        logger.error("Error parsing date string - " + e);
                        e.printStackTrace();
                    }
                    
                    long stateTimestamp = date.getTime();

                    if (stateTimestamp >= startTimestamp) {
                        // this value needs to be evaluated.
                        if (lightOnTime != null) {
                            if (record.stateValue.equals("0")) {
                                logger.debug("Light " + deviceId + " off in range at " + record.timestamp + " adding " + (stateTimestamp
                                        - lightOnTime.getTime()));

                                totalMillis += (stateTimestamp - lightOnTime.getTime());
                                lightOnTime = null;
                            } else {
                                // quick on-off or off-on within the same second can be reported 'out of order'
                                // we currently can't tell which on came first, which can lead to inaccuracies.
                                logger.debug("Light on when already on at " + record.timestamp);
                            }
                        } else {
                            if (record.stateValue.equals("1")) {
                                logger.debug("Light " + deviceId + " on in range at " + record.timestamp);

                                lightOnTime = date;
                            } else {
                                if(previousReportsFound == true) {
                                    // quick on-off or off-on within the same second can be reported 'out of order'
                                    // we currently can't tell which on came first, which can lead to inaccuracies.
                                    logger.debug("Light " + deviceId + " off in range with no start at " + record.timestamp + " ignoring");
                                } else {
                                    //  a potential corner case of a light left on for days before the start of the range.
                                    // or when a new light is added and it first reports off state.
                                    // we will detect it in most cases
                                    // as we retrieve more data than we need, but here we didn't find it.
                                    logger.debug("Light " + deviceId + " off in range with no start at "
                                            + record.timestamp + " ignore as no previous reports found");
                                }
                            }
                        }

                        previousReportsFound = true;

                    } else {
                        previousReportsFound = true;

                        // this date is before, but it may indicate the previous state, so record it in
                        // previous state
                        if (record.stateValue.equals("1")) {
                            logger.debug("Light " + deviceId + " on before start at " + record.timestamp);
                            lightOnTime = startDate; // set the start time to the start of the time range.
                        } else {
                            // clear the light on time as the light went off before our test range.
                            logger.debug("Light " + deviceId + " off before start at " + record.timestamp);
                            lightOnTime = null;
                        }
                    }
                }
            }

            // if we exit the loop and lightOnTime is not null that means the light is
            // currently on.
            if (lightOnTime != null) {
                long startTime = (lightOnTime.getTime() < startTimestamp)? startTimestamp : lightOnTime.getTime();
                totalMillis += currentTimestamp - lightOnTime.getTime();
            }
        }

        return totalMillis;
    }

    @POST
    @Path("/anomalies/{deviceid}/ignore")
    public Response anomalyIgnore(@Auth IDPrincipal principal, @PathParam("deviceid") ObjectId deviceId) {
        Response response;

        // try to get and lock the anomaly
        try {
            LightTrackerAnomaly anomaly = lightTrackerAnomalyDao.getLightTrackerAnomalyWithLock(deviceId);

            if (anomaly != null) {
                anomaly.setAnomalyIgnore(client, scsEndpoint, lightTrackerAnomalyDao);
                // the setAnomalyIgnore call takes care of updating the database.

                response = Response.ok().build();
            } else {
                response = Response.status(Status.NOT_FOUND).entity("Device report not found.")
                        .type(MediaType.TEXT_PLAIN).build();
            }
        } catch (OLockFailureException e) {
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Device could not be locked to ignore.")
                    .type(MediaType.TEXT_PLAIN).build();
        }

        return response;
    }

    @GET
    @Path("/anomalies")
    public Response getActieAnomalies(@Auth IDPrincipal principal, @PathParam("homeid") ObjectId homeId) {
        Response response;

        LightTrackerSubscriptionRecord subRecord = lightTrackerSubscriptionRecordDao
                .getHomeSubscription(homeId);
        if (subRecord != null) {
            // verify the home is subscribed.
            List<LightTrackerAnomaly> anomalies = lightTrackerAnomalyDao.getHouseActiveAnomalies(homeId);
            List<LightTrackerReportedAnomaly> reportedAnomalies = new ArrayList<LightTrackerReportedAnomaly>();
            List<ExternalDeviceConfiguration> deviceConfigurations = subRecord.getHomeConfig().deviceConfigurations
                    .get("lighting");

            for (LightTrackerAnomaly anomaly : anomalies) {
                String deviceName = "unknown";

                for (ExternalDeviceConfiguration edc : deviceConfigurations) {
                    if (edc.deviceId.equals(anomaly.getDeviceId().toString())) {
                        deviceName = edc.deviceName;
                        break; // from for loop
                    }
                }
                LightTrackerReportedAnomaly amra = new LightTrackerReportedAnomaly(anomaly, deviceName);
                reportedAnomalies.add(amra);
            }

            response = Response.ok(reportedAnomalies).build();
        } else {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Home is not subscribed to the appliance monitor service.")
                    .type(MediaType.TEXT_PLAIN).build();
        }

        return response;
    }
}
