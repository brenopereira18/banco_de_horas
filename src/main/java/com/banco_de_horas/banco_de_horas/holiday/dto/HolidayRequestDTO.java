package com.banco_de_horas.banco_de_horas.holiday.dto;

import java.time.LocalDate;

public record HolidayRequestDTO(
    LocalDate date,
    String description
) {
}
