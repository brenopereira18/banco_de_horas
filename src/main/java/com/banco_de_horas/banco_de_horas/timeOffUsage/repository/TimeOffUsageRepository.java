package com.banco_de_horas.banco_de_horas.timeOffUsage.repository;

import com.banco_de_horas.banco_de_horas.timeOffUsage.entity.TimeOffUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeOffUsageRepository extends JpaRepository<TimeOffUsageEntity, Long> {
}
