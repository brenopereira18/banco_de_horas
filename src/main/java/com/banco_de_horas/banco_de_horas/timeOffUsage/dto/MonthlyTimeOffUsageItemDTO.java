package com.banco_de_horas.banco_de_horas.timeOffUsage.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyTimeOffUsageItemDTO(
    Long id,
    LocalDate solicitationDate,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal hoursUsed,
    BigDecimal fractionalHours,
    String formattedHours,
    long businessDays
) {
}
