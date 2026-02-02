package com.banco_de_horas.banco_de_horas.timeOffUsage.entity;

import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "uso_folga")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimeOffUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiscal_id", nullable = false)
    @NotNull(message = "Fiscal é obrigatório")
    private TaxEntity taxEntity;

    @Column(name = "data_inicio_folga", nullable = false)
    @NotNull(message = "Data de início da folga é obrigatória")
    private LocalDate startDate;

    @Column(name = "data_fim_folga")
    private LocalDate endDate;

    @Column(name = "horas_fracionadas")
    private BigDecimal fractionalHours;

    @Column(name = "horas_utilizadas", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Horas utilizadas é obrigatório")
    @DecimalMin(value = "0.01", message = "Horas utilizadas deve ser maior que zero")
    private BigDecimal hoursUsed;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime registrationDate;


    @PrePersist
    private void onCreate() {
        this.registrationDate = LocalDateTime.now();
    }

    public void calculateHoursUsed() {
        if (startDate != null && endDate != null) {
            long totalMinutes = Duration.between(startDate, endDate).toMinutes();

            this.hoursUsed = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
    }
}
