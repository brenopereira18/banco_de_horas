package com.banco_de_horas.banco_de_horas.tax.repository;

import com.banco_de_horas.banco_de_horas.tax.entity.PasswordResetTokenEntity;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    Optional<PasswordResetTokenEntity> findByTaxEntity(TaxEntity tax);
}
