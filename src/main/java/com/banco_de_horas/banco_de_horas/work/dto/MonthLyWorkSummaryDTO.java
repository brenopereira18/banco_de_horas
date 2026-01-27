package com.banco_de_horas.banco_de_horas.work.dto;

import java.math.BigDecimal;

public record MonthLyWorkSummaryDTO(
    Long totalServices,
    BigDecimal totalHoursGenerated
) {
}
