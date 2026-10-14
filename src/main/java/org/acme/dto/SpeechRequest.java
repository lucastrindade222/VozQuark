package org.acme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpeechRequest(
    @NotBlank(message = "O texto não pode ser vazio")
    @Size(min = 3, max = 500, message = "O texto deve ter entre 3 e 500 caracteres")
    String text) {}
