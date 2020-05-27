package com.sendal.lighttracker.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response;
import com.codahale.metrics.annotation.*;

@Timed
@ResponseMetered
@ExceptionMetered
@Path("/")
public class HealthResource {
    public HealthResource() {

    }

    @GET
    @Path("/")
    public Response getHealth() {
        Response response;

        response = Response.ok().build();
        return response;
    }
}
