package org.acme.resource;

import org.acme.dto.SpeechRequest;
import org.acme.service.TtsService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/tts")
public class TtsResource {

   @Inject
    TtsService tts;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces ("audio/wav")
    public Response speak(@Valid SpeechRequest speechRequest) throws Exception {
        System.out.println("Texto recebido: " + speechRequest.text());
        if (speechRequest.text() == null || speechRequest.text().isBlank()) {
            return Response.status(400).entity("Texto vazio").type(MediaType.TEXT_PLAIN).build();
        }
        return Response.ok(tts.synthesize(speechRequest.text())).build();
    }
}
