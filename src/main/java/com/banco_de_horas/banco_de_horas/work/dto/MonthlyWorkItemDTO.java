package com.banco_de_horas.banco_de_horas.work.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MonthlyWorkItemDTO(
    Long id,
    String description,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    boolean holiday,
    BigDecimal generatedTimeOff,
    String formattedGeneratedTimeOff
) {
}
