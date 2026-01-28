package com.banco_de_horas.banco_de_horas.work.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkResponseDTO(
    Long id,

    Long taxId,
    String fiscalName,
    String description,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,

    BigDecimal hoursWorked,
    BigDecimal generatedTimeOff,

    String detailedCalculation,

    LocalDateTime registrationDate
) {
}
