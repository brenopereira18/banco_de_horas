package com.banco_de_horas.banco_de_horas.tax.repository;

import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxRepository extends JpaRepository<TaxEntity, Long> {

    boolean existsByRegistration(String registration);

    Optional<TaxEntity> findByRegistration(String registration);

    List<TaxEntity> findAllByOrderByFullNameAsc();
}
