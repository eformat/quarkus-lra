package org.acme;

import org.eclipse.microprofile.lra.annotation.*;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Random;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@Path("/hello")
public class GreetingResource {

    private static final Logger log = LoggerFactory.getLogger(GreetingResource.class);

    @GET
    @LRA // Step 2b: The method should run within an LRA
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId /* Step 2c the context is useful for associating compensation logic */) {
        log.info("hello with context {}", lraId);
        return "Hello RESTEasy";
    }

    @GET
    @Path("/start")
    @LRA(end = false) // Step 3a: The method should run within an LRA
    @Produces(MediaType.TEXT_PLAIN)
    public String start(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        log.info("hello with context {}", lraId);
        return lraId.toASCIIString();
    }

    // Step 2d: There must be a method to compensate for the action if it's cancelled
    // does not end the LRA
    @PUT
    @Path("compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        log.info("compensating {}", lraId);
        return Response.ok(lraId.toASCIIString()).build();
    }

    @PUT
    @Path("tidyup")
    @LRA(value = LRA.Type.MANDATORY,
            cancelOn = {
                    Response.Status.INTERNAL_SERVER_ERROR // cancel on a 500 code
            },
            cancelOnFamily = {
                    Response.Status.Family.CLIENT_ERROR // cancel on any 4xx code
            },
            end = true) // end the LRA
    @Compensate
    public Response compensateWorkAndEnd(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        log.info("compensate and end {}", lraId);
        if (new Random().nextBoolean())
            return Response.serverError().build();
        return Response.ok(lraId.toASCIIString()).build();
    }

    // Step 2e: An optional callback notifying that the LRA is closing
    @PUT
    @Path("complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        log.info("completing {}", lraId);
        return Response.ok(lraId.toASCIIString()).build();
    }

    @GET
    @Path("/end")
    @LRA(value = LRA.Type.MANDATORY) // Step 3a: The method MUST be invoked with an LRA
    @Produces(MediaType.TEXT_PLAIN)
    public String end(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return lraId.toASCIIString();
    }

    @PUT
    @Path("/after")
    @AfterLRA // method is called when the LRA associated with the method activityWithLRA finishes
    public Response notifyLRAFinished(@HeaderParam(LRA.LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraId,
                                      LRAStatus status) {
        switch (status) {
            case Closed:
                log.info("LRA closed and finished OK {}", lraId);
                // the LRA was successfully closed
                break;
            default:
                log.info("LRA finished {}", lraId);
        }
        return Response.ok(lraId.toASCIIString()).build();
    }

    @Forget
    @Path("/forget")
    @DELETE
    public Response forget(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok().build();
    }
}
