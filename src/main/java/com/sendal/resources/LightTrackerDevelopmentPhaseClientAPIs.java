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
import javax.validation.Validator;

import com.sendal.common.StateValue;

import org.bson.types.ObjectId;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

import com.sendal.lighttracker.db.LightTrackerStateHistory;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;

import com.sendal.lighttracker.LightTrackerUsageSummary;

import com.sendal.externalapicommon.ExternalDeviceConfiguration;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.TimeZone;
import java.util.Date;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.tika.Tika;

import com.codahale.metrics.annotation.*;

//
// This resource class supports direct connections by the client during the development phase.
// It in-effect terminates client to Sendal Cloud PSS Proxy APIs, and does so w/out any authentication.
// This class should be used during local development only.

@Timed
@ResponseMetered
@ExceptionMetered
@Path("/v1/pss/{pssid}/users/{userid}/homes/{homeid}")
public class LightTrackerDevelopmentPhaseClientAPIs {
    private final LightTrackerIntegrationService lightTrackerIntegrationService;

    private final Logger logger = LoggerFactory.getLogger(LightTrackerDevelopmentPhaseClientAPIs.class);


    public LightTrackerDevelopmentPhaseClientAPIs(LightTrackerIntegrationService lightTrackerIntegrationService) {
        this.lightTrackerIntegrationService = lightTrackerIntegrationService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/api/passthrough/usagesummary")
    public Response lightUsageSummary(@PathParam("pssid") ObjectId pssId, @PathParam("userid") ObjectId userId, @PathParam("homeid") ObjectId homeId) {
        return lightTrackerIntegrationService.lightUsageSummary(null, homeId);
    }
}
