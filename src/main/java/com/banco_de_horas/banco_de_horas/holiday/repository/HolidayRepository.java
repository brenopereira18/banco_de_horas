package com.banco_de_horas.banco_de_horas.holiday.repository;

import com.banco_de_horas.banco_de_horas.holiday.entity.HolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface HolidayRepository extends JpaRepository<HolidayEntity, Long> {

    boolean existsByDate(LocalDate date);
}
