package com.banco_de_horas.banco_de_horas.tax.dto;

import com.banco_de_horas.banco_de_horas.tax.entity.UserType;

import java.math.BigDecimal;

public record TaxResponseDTO(
    Long id,
    String fullName,
    String email,
    String balanceOfHours,
    BigDecimal lastAddedHours,
    UserType userType
) {
}
