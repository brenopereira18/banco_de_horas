package com.banco_de_horas.banco_de_horas.work.repository;

import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.work.entity.WorkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface WorkRepository extends JpaRepository<WorkEntity, Long> {
    @Query("""
        SELECT COALESCE(SUM(w.generatedTimeOff), 0)
        FROM WorkEntity w
        WHERE w.taxEntity = :tax
    """)
    BigDecimal sumAllHoursGeneratedByTax(@Param("tax") TaxEntity tax);

    Page<WorkEntity> findByTaxEntityOrderByRegistrationDateDesc(
        TaxEntity tax,
        Pageable pageable
    );
}
