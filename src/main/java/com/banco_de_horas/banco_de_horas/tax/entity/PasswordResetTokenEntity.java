package com.banco_de_horas.banco_de_horas.tax.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@Table(name = "resetar_senha")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordResetTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiryIn;

    @Column(name = "gerado_em", nullable = false)
    private LocalDateTime generatedIn;

    @Column(name = "utilizado", nullable = false)
    private boolean used = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_id", nullable = false)
    private TaxEntity taxEntity;
}
