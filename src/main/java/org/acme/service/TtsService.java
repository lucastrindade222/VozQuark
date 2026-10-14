package org.acme.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TtsService {

    @ConfigProperty(name = "tts.piper.bin")
    String piperBin;

    @ConfigProperty(name = "tts.piper.model")
    String model;

    @Bulkhead(2)
    @RateLimit(value = 20, window = 1, windowUnit = ChronoUnit.MINUTES)
    public byte[] synthesize(String text) throws IOException, InterruptedException {
        Path out = Files.createTempFile("tts-", ".wav");
        try {
            Process p = new ProcessBuilder(piperBin, "--model", model, "--output_file", out.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            try (var os = p.getOutputStream()) {
                os.write(text.getBytes(StandardCharsets.UTF_8));
            }

            if (!p.waitFor(30, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("Timeout no Piper");
            }
            if (p.exitValue() != 0) {
                throw new IOException("Piper falhou: " + p.exitValue());
            }
            return Files.readAllBytes(out);
        } catch (Exception e) {
            throw new IOException("Erro ao sintetizar áudio", e);
        } finally {
            Files.deleteIfExists(out);
        }
    }
}