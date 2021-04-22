package com.sendal.lighttracker.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response;
import javax.annotation.PostConstruct;
import javax.validation.Validator;
import javax.validation.ConstraintViolation;

import com.sendal.common.StateIdentifier;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import com.sendal.lighttracker.db.LightTrackerAnomoly;
import com.sendal.lighttracker.db.LightTrackerSubscriptionRecord;
import com.sendal.lighttracker.db.dao.LightTrackerSubscriptionRecordDAO;
import com.sendal.lighttracker.db.dao.LightTrackerAnomolyDAO;

import com.sendal.externalapicommon.security.IDPrincipal;
import io.dropwizard.auth.Auth;

import com.sendal.common.StateReport;

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
    private final LightTrackerAnomolyDAO lightTrackerAnomolyDao;
    private final String scsEndpoint;
    private final String scs3pssId;
    private final Logger logger = LoggerFactory.getLogger(LightTrackerStates.class);

    public LightTrackerStates(Client client, LightTrackerSubscriptionRecordDAO lightTrackerSubscriptionRecordDao, LightTrackerAnomolyDAO lightTrackerAnomolyDao, String scsEndpoint, String scs3pssId, Validator validator) {
        this.client = client;
        this.validator = validator;
        this.scsEndpoint = scsEndpoint;
        this.scs3pssId = scs3pssId;
        this.lightTrackerSubscriptionRecordDao = lightTrackerSubscriptionRecordDao;
        this.lightTrackerAnomolyDao = lightTrackerAnomolyDao;
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
                LightTrackerAnomoly.lightStateUpdate(updatedState, lightTrackerAnomolyDao);
           }
        }

        return response;
    }
}
