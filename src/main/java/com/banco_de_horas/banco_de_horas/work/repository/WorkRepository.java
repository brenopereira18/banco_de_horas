package com.banco_de_horas.banco_de_horas.work.repository;

import com.banco_de_horas.banco_de_horas.work.entity.WorkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRepository extends JpaRepository<WorkEntity, Long> {
}
