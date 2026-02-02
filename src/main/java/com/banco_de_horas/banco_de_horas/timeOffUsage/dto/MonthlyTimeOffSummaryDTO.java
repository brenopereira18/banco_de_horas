package com.banco_de_horas.banco_de_horas.timeOffUsage.dto;

import java.math.BigDecimal;

public record MonthlyTimeOffSummaryDTO(
    Long totalTimeOffs,
    BigDecimal totalHoursUsed
) {
}
