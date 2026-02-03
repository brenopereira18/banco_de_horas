package com.banco_de_horas.banco_de_horas.tax.dto;

import com.banco_de_horas.banco_de_horas.tax.entity.UserType;

public record TaxManagementRequestDTO(
    String fullName,
    UserType userType
) {
}
