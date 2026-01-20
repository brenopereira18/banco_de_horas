package com.banco_de_horas.banco_de_horas.holiday.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "feriado")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HolidayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false)
    private LocalDate date;

    @Column(name = "descricao", nullable = false)
    private String description;
}
