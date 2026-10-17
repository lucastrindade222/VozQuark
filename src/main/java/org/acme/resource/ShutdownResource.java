package org.acme.resource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.Quarkus;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/shutdown")
public class ShutdownResource {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response shutdown() {
        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                .execute(Quarkus::asyncExit);
        return Response.accepted("Encerrando VozQuark").build();
    }
}