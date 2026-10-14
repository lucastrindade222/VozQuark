package org.acme.resource.exception;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.smallrye.faulttolerance.api.RateLimitException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

public class ExceptionMappers {

    @ServerExceptionMapper
    public Response busy(BulkheadException e) {
        return Response.status(429)
                .header("Retry-After", "5")
                .entity("Servidor ocupado, tente novamente")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    @ServerExceptionMapper
    public Response tooMany(RateLimitException e) {
        return Response.status(429)
                .header("Retry-After", "60")
                .entity("Muitas requisições")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}