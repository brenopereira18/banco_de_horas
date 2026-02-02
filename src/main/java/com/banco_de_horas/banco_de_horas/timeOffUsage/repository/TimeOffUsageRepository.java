package com.banco_de_horas.banco_de_horas.timeOffUsage.repository;

import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.timeOffUsage.entity.TimeOffUsageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TimeOffUsageRepository extends JpaRepository<TimeOffUsageEntity, Long> {
    @Query("""
        SELECT COALESCE(SUM(t.hoursUsed), 0)
        FROM TimeOffUsageEntity t
        WHERE t.taxEntity = :tax
    """)
    BigDecimal sumAllHoursUsedByTax(@Param("tax") TaxEntity tax);

    Page<TimeOffUsageEntity> findByTaxEntityOrderByRegistrationDateDesc(
        TaxEntity tax,
        Pageable pageable
    );
}
