package com.banco_de_horas.banco_de_horas.tax.dto;

import java.math.BigDecimal;

public record TaxResponseDTO(
    Long id,
    String fullName,
    String balanceOfHours,
    BigDecimal lastAddedHours
) {
}
