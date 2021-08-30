package org.acme;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

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

    // Step 2d: There must be a method to compensate for the action if it's cancelled
    @PUT
    @Path("compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        log.info("compensating {}", lraId);
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
    @Path("/start")
    @LRA(end = false) // Step 3a: The method should run within an LRA
    @Produces(MediaType.TEXT_PLAIN)
    public String start(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        log.info("hello with context {}", lraId);
        return lraId.toASCIIString();
    }

    @GET
    @Path("/end")
    @LRA(value = LRA.Type.MANDATORY) // Step 3a: The method MUST be invoked with an LRA
    @Produces(MediaType.TEXT_PLAIN)
    public String end(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return lraId.toASCIIString();
    }
}
