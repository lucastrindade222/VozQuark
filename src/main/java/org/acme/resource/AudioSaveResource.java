package org.acme.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/audio/save")
public class AudioSaveResource {

    private static final Logger LOG = Logger.getLogger(AudioSaveResource.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @ConfigProperty(name = "tts.save-dir", defaultValue = "audios")
    String saveDir;

    @POST
    @Consumes("audio/wav")
    @Produces(MediaType.APPLICATION_JSON)
    public Response save(byte[] data,
                         @Context HttpServerRequest request,
                         @HeaderParam("X-Requested-With") String header) throws IOException {

        String host = request.remoteAddress().host();
        boolean local = "127.0.0.1".equals(host) || "::1".equals(host)
                || "0:0:0:0:0:0:0:1".equals(host);
        if (!local || !"VozQuark".equals(header)) {
            return Response.status(403).entity(Map.of("error", "Proibido")).build();
        }
        if (!isWav(data)) {
            return Response.status(400).entity(Map.of("error", "Arquivo WAV inválido")).build();
        }

        java.nio.file.Path dir = java.nio.file.Path.of(saveDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        java.nio.file.Path target = uniqueTarget(dir);
        Files.write(target, data);

        LOG.infof("Áudio salvo em %s", target);
        return Response.ok(Map.of("path", target.toString(),
                                  "name", target.getFileName().toString())).build();
    }

    private static boolean isWav(byte[] d) {
        return d != null && d.length > 44
                && "RIFF".equals(new String(d, 0, 4, StandardCharsets.US_ASCII))
                && "WAVE".equals(new String(d, 8, 4, StandardCharsets.US_ASCII));
    }

    private static java.nio.file.Path uniqueTarget(java.nio.file.Path dir) {
        String base = "vozquark-" + FMT.format(LocalDateTime.now());
        java.nio.file.Path p = dir.resolve(base + ".wav");
        int i = 1;
        while (Files.exists(p)) {
            p = dir.resolve(base + "-" + i++ + ".wav");
        }
        return p;
    }
}