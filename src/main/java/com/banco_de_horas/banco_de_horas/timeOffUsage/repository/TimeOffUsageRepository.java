package com.banco_de_horas.banco_de_horas.timeOffUsage.repository;

import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.timeOffUsage.entity.TimeOffUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TimeOffUsageRepository extends JpaRepository<TimeOffUsageEntity, Long> {
    @Query("""
        SELECT COUNT(t)
        FROM TimeOffUsageEntity t
        WHERE t.taxEntity = :tax
          AND t.startDateTime BETWEEN :start AND :end
    """)
    Long countByTaxAndPeriod(
        @Param("tax") TaxEntity tax,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COALESCE(SUM(t.hoursUsed), 0)
        FROM TimeOffUsageEntity t
        WHERE t.taxEntity = :tax
          AND t.startDateTime BETWEEN :start AND :end
    """)
    BigDecimal sumHoursUsedByTaxAndPeriod(
        @Param("tax") TaxEntity tax,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
