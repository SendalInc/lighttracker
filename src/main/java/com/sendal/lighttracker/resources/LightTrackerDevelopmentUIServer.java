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

import com.sendal.common.state.StateValue;

import org.bson.types.ObjectId;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

import com.sendal.lighttracker.pojo.LightTrackerStateHistory;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;

import com.sendal.lighttracker.pojo.LightTrackerUsageSummary;

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
public class LightTrackerDevelopmentUIServer {

    private final Logger logger = LoggerFactory.getLogger(LightTrackerDevelopmentUIServer.class);

    public LightTrackerDevelopmentUIServer() {
    }

    // below is a very simple file service to return UI content when testing locally.
    @GET
    @Path("/assets/{subResources:.*}")
    public Response basicUIFileServer(@PathParam("pssid") ObjectId pssId, @PathParam("userid") ObjectId userId, @PathParam("homeid") ObjectId homeId, @PathParam("subResources") String subResources) throws Exception{
        try {
            Tika tika = new Tika();
            String path = "ui/static/" + subResources;
            InputStream is = new FileInputStream(path);
            String contentType = tika.detect(path);
            
            return Response.ok(is).type(contentType).build();
        }
        catch(FileNotFoundException e) {
            logger.error("File not found - " + e);
            return Response.status(Status.NOT_FOUND).entity(e)
                .type(MediaType.TEXT_PLAIN).build();
        }
    }
}
