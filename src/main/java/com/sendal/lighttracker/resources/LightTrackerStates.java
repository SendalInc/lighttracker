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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sendal.lighttracker.db.LightTrackerAnomaly;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;
import com.sendal.lighttracker.db.dao.LightTrackerAnomalyDAO;

import com.sendal.common.coredb.DBPermissions;
import com.sendal.common.state.StateReport;

import com.sendal.externalapicommon.security.IDPrincipal;

import com.sendal.externalapicommon.ExternalHomeConfiguration;
import com.sendal.externalapicommon.ExternalDeviceConfiguration;
import com.sendal.externalapicommon.ExternalRoomConfiguration;
import com.codahale.metrics.annotation.*;

@Timed
@ResponseMetered
@ExceptionMetered
@Path("/api/v1/states")
@Produces(MediaType.APPLICATION_JSON)
public class LightTrackerStates {
    private final Client client;
    private final Validator validator;
    private final LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao;
    private final LightTrackerAnomalyDAO lightTrackerAnomalyDao;
    private final String scsEndpoint;
    private final String scs3pssId;
    private final Logger logger = LoggerFactory.getLogger(LightTrackerStates.class);

    public LightTrackerStates(Client client, LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao, LightTrackerAnomalyDAO lightTrackerAnomalyDao, String scsEndpoint, String scs3pssId, Validator validator) {
        this.client = client;
        this.validator = validator;
        this.scsEndpoint = scsEndpoint;
        this.scs3pssId = scs3pssId;
        this.lightTrackerSubscriptionRecordDao = lightTrackerSubscriptionRecordDao;
        this.lightTrackerAnomalyDao = lightTrackerAnomalyDao;
    }

    @POST
    @Path("/stateupdate")
    public Response stateSubscriptionUpdate(@Auth IDPrincipal principal, StateReport updatedState) {
        Response response = Response.ok().build(); // can be replaced below

        Set<ConstraintViolation<StateReport>> violations = validator.validate(updatedState);
        if(violations.size() > 0) {
            // Validation errors occurred
            ArrayList<String> validationMessages = new ArrayList<String>();
            for (ConstraintViolation<StateReport> violation : violations) { 
                validationMessages.add(violation.getPropertyPath().toString() +": " + violation.getMessage()); 
            } 

            response = Response.status(Status.BAD_REQUEST).entity(validationMessages)
                    .type(MediaType.TEXT_PLAIN).build(); 
        } else { 
            // check for a deviceState.
            if(updatedState.getStateIdentifier().getDevice() != null &&
               updatedState.getStateIdentifier().getStateName().equals("isOn")) {
                LightTrackerAnomaly.lightStateUpdate(updatedState, client, lightTrackerAnomalyDao, scsEndpoint);
           }
        }

        return response;
    }
}
