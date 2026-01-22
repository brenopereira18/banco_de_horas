package com.banco_de_horas.banco_de_horas.timeOffUsage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TimeOffUsageRequestDTO(
    @NotNull
    Long taxId,

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    LocalDateTime startDateTime,

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    LocalDateTime endDateTime
) {
}
