package org.acme.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.acme.dto.SpeechRequest;
import org.acme.service.TtsService;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
class TtsResourceTest {

    @InjectMock
    TtsService ttsServiceMock;

    @Test
    void deveRetornarAudioComSucesso() throws Exception {
        byte[] audioFicticio = "conteudo-do-audio-wav".getBytes();
        when(ttsServiceMock.synthesize(anyString())).thenReturn(audioFicticio);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(new SpeechRequest("Texto válido para falar"))
        .when()
            .post("/tts")
        .then()
            .statusCode(200)
            .contentType("audio/wav")
            .body(is(new String(audioFicticio)));
    }

    @Test
    void deveRetornar400QuandoTextoForVazio() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"text\":\"\"}")
        .when()
            .post("/tts")
        .then()
            .statusCode(400)
            .header("validation-exception", "true")
            .body(containsString("O texto não pode ser vazio"));
    }

    @Test
    void deveRetornar400QuandoCorpoNaoTemTexto() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
        .when()
            .post("/tts")
        .then()
            .statusCode(400)
            .header("validation-exception", "true")
            .body(containsString("O texto não pode ser vazio"));
    }

    @Test
    void deveRetornar400QuandoTextoForMuitoLongo() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"text\":\"" + "A".repeat(501) + "\"}")
        .when()
            .post("/tts")
        .then()
            .statusCode(400)
            .header("validation-exception", "true")
            .body(containsString("entre 3 e 500 caracteres"));
    }

    @Test
    void deveRetornar500QuandoServicoFalhar() throws Exception {
        when(ttsServiceMock.synthesize(anyString()))
            .thenThrow(new IOException("Piper falhou"));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(new SpeechRequest("Texto que vai falhar"))
        .when()
            .post("/tts")
        .then()
            .statusCode(500);
    }

    @Test
    void deveRetornar429QuandoServidorOcupado() throws Exception {
        when(ttsServiceMock.synthesize(anyString()))
            .thenThrow(new BulkheadException("cheio"));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(new SpeechRequest("Texto qualquer"))
        .when()
            .post("/tts")
        .then()
            .statusCode(429)
            .header("Retry-After", "5");
    }
}