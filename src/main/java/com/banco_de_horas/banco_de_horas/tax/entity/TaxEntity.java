package com.banco_de_horas.banco_de_horas.tax.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "fiscal")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaxEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false)
    @NotBlank(message = "Nome é obrigatório")
    private String fullName;

    @Column(name = "matricula", nullable = false, unique = true)
    @NotBlank(message = "Matrícula é obrigatória")
    @Pattern(regexp = "\\d{5}-\\d", message = "A matrícula deve estar no formato 00000-0")
    private String registration;

    @Column(name = "email")
    private String email;

    @Column(name = "senha", nullable = false)
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 7, message = "A senha deve ter no mínimo 7 caracteres")
    private String password;

    @Column(name = "saldo_de_horas", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Saldo de horas é obrigatório")
    @DecimalMin(value = "0.00", message = "Saldo não pode ser negativo")
    private BigDecimal balanceOfHours = BigDecimal.ZERO;

    @Column(name = "ultima_adicao_horas", precision = 10, scale = 2)
    private BigDecimal lastAddedHours;

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
    public long getBalanceInDays() {
        BigDecimal hoursPerDay = switch (userType) {
            case FISCAL -> BigDecimal.valueOf(6);
            case SUPERVISOR, ADMINISTRADOR -> BigDecimal.valueOf(8);
        };

        return balanceOfHours
            .divide(hoursPerDay, 0, RoundingMode.FLOOR)
            .longValue();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
            new SimpleGrantedAuthority("ROLE_" + userType.name())
        );
    }

    @Override
    public @Nullable String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.registration;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
