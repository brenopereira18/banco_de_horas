package com.banco_de_horas.banco_de_horas.tax.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "fiscal")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false)
    @NotBlank(message = "Nome é obrigatório")
    private String fullName;

    @Column(name = "matricula", nullable = false, unique = true)
    @NotBlank(message = "Matrícula é obrigatória")
    private String registration;

    @Column(name = "saldo_de_horas", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Saldo de horas é obrigatório")
    @DecimalMin(value = "0.00", message = "Saldo não pode ser negativo")
    private BigDecimal balanceOfHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private UserType userType;

    @PrePersist
    protected void onCreate() {
        if (balanceOfHours == null) {
            balanceOfHours = BigDecimal.ZERO;
        }
    }

    /**
     * Adiciona horas ao saldo
     */
    public void addHours(BigDecimal hours) {
        if (hours == null || hours.signum() <= 0) {
            return;
        }
        this.balanceOfHours = this.balanceOfHours.add(hours);
    }

    /**
     * Remove horas do saldo (validação deve ocorrer no service)
     */
    public void subtractHours(BigDecimal hours) {
        if (hours == null || hours.signum() <= 0) {
            return;
        }
        this.balanceOfHours = this.balanceOfHours.subtract(hours);
    }

    /**
     * Retorna saldo em dias
     * Fiscal: 6h = 1 dia
     * Supervisor/Admin: 8h = 1 dia
     */
    public BigDecimal getBalanceInDays() {
        BigDecimal divisor = switch (userType) {
            case FISCAL -> BigDecimal.valueOf(6);
            case SUPERVISOR, ADMINISTRADOR -> BigDecimal.valueOf(8);
        };

        return balanceOfHours
            .divide(divisor, 2, RoundingMode.HALF_UP);
    }
}
