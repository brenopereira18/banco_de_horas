package com.banco_de_horas.banco_de_horas.work.entity;

import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "trabalho")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_id", nullable = false)
    @NotNull(message = "Fiscal é obrigatório")
    private TaxEntity taxEntity;

    @Column(name = "data_hora_inicio", nullable = false)
    @NotNull(message = "Data/hora de início é obrigatória")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime startDateTime;

    @Column(name = "descricao")
    private String description;

    @Column(name = "data_hora_fim", nullable = false)
    @NotNull(message = "Data/hora de fim é obrigatória")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime endDateTime;

    @Column(name = "horas_trabalhadas", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Horas trabalhadas é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Horas trabalhadas deve ser maior que zero")
    private BigDecimal hoursWorked;

    @Column(name = "horas_folga_geradas", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Horas de folga geradas é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Horas de folga deve ser maior que zero")
    private BigDecimal generatedTimeOff;

    @Column(name = "detalhamento_calculo", columnDefinition = "TEXT")
    private String detailedCalculation;

    @Column(name = "data_cadastro", nullable = false)
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime registrationDate;


    @PrePersist
    @PreUpdate
    private void calculateHoursWorked() {
        this.registrationDate = LocalDateTime.now();

        if (startDateTime != null && endDateTime != null) {
            long minutes = Duration.between(startDateTime, endDateTime).toMinutes();

            this.hoursWorked = BigDecimal
                .valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Valida se a data/hora de fim é posterior à de início
     */
    @AssertTrue(message = "Data/hora de fim deve ser posterior à de início")
    public boolean isEndDateAfterStartDate() {
        if (startDateTime == null || endDateTime == null) {
            return true;
        }
        return endDateTime.isAfter(startDateTime);
    }

    /**
     * Calcula automaticamente as horas trabalhadas
     */
    public Duration getDuration() {
        if (startDateTime == null || endDateTime == null) {
            return Duration.ZERO;
        }

        return Duration.between(startDateTime, endDateTime);
    }

    /**
     * Verifica se o serviço cruza a meia-noite
     */
    public boolean crossesMidnight() {
        if (startDateTime == null || endDateTime == null) {
            return false;
        }
        return !startDateTime.toLocalDate().equals(endDateTime.toLocalDate());
    }


    /**
     * Retorna uma descrição resumida do serviço
     */
    public String getdescriptionSummary() {
        return String.format("Serviço de %s às %s - %.2fh trabalhadas = %.2fh folga (%.1f%% bonificação)",
            startDateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            endDateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            hoursWorked,
            generatedTimeOff);
    }
}
