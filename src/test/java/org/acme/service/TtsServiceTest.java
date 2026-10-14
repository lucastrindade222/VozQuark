package org.acme.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TtsServiceTest {

    @Inject
    TtsService ttsService;

    @Test
    void deveSintetizarAudioComSucesso() throws Exception {
        // Cenário usando o 'echo' configurado no properties de teste
        String texto = "Olá mundo Quarkus";

        // Execução
        byte[] resultado = ttsService.synthesize(texto);

        // Verificação (Como o 'echo' não gera um arquivo WAV real, o arquivo lido estará vazio, mas o array não será nulo)
        assertNotNull(resultado, "O retorno do áudio não deveria ser nulo");
    }

    @Test
    void deveLancarExcecaoQuandoBinarioNaoExistir() {
        // Alterando a propriedade programaticamente para forçar uma falha
        ttsService.piperBin = "caminho/invalido/para/o/binario";

        // Verificação se lança IOException em caso de falha no processo
        assertThrows(IOException.class, () -> {
            ttsService.synthesize("Texto de teste");
        }, "Deveria lançar IOException devido ao binário inválido");
    }
}