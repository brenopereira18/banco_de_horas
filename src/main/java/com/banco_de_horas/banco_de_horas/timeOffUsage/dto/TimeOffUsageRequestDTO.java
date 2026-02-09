package com.banco_de_horas.banco_de_horas.timeOffUsage.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeOffUsageRequestDTO(
    @NotNull
    Long taxId,

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    LocalDate solicitationDate,

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    LocalDate startDate,

    @JsonFormat(pattern = "dd/MM/yyyy")
    LocalDate endDate,

    BigDecimal fractionalHours
) {
}
