package com.banco_de_horas.banco_de_horas.tax.repository;

import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRepository extends JpaRepository<TaxEntity, Long> {

    boolean existsByRegistration(String registration);
}
