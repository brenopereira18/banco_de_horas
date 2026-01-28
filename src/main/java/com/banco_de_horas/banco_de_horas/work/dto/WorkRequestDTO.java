package com.banco_de_horas.banco_de_horas.work.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record WorkRequestDTO(
    @NotNull
    Long taxId,

    String description,

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    LocalDateTime startDateTime,

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    LocalDateTime endDateTime
) {
}
