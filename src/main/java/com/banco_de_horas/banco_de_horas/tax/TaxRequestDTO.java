package com.banco_de_horas.banco_de_horas.tax;

import com.banco_de_horas.banco_de_horas.tax.entity.UserType;

public record TaxRequestDTO(
    String fullName,
    String registration,
    UserType userType
) {
}
