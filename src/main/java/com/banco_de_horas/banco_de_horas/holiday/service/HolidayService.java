package com.banco_de_horas.banco_de_horas.holiday.service;

import com.banco_de_horas.banco_de_horas.exceptions.EntityAlreadyExists;
import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.holiday.dto.HolidayRequestDTO;
import com.banco_de_horas.banco_de_horas.holiday.entity.HolidayEntity;
import com.banco_de_horas.banco_de_horas.holiday.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayEntity create(HolidayRequestDTO dto) {
        if (holidayRepository.existsByDate(dto.date())) {
            throw new EntityAlreadyExists("Já existe feriado cadastrado nessa data");
        }

        HolidayEntity holiday = HolidayEntity.builder()
            .date(dto.date())
            .description(dto.description())
            .build();

        HolidayEntity saved = holidayRepository.save(holiday);
        log.info("Feriado criado | Data: {} | Descrição: {}", dto.date(), dto.description());
        return saved;
    }

    public HolidayEntity update(Long id, HolidayRequestDTO dto) {
        HolidayEntity existing = holidayRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Feriado não encontrado"));

        // Valida data duplicada apenas se a data foi alterada
        if (!existing.getDate().equals(dto.date()) && holidayRepository.existsByDate(dto.date())) {
            throw new EntityAlreadyExists("Já existe feriado nessa data");
        }

        existing.setDate(dto.date());
        existing.setDescription(dto.description());

        HolidayEntity updated = holidayRepository.save(existing);
        log.info("Feriado atualizado | ID: {} | Nova data: {}", id, dto.date());
        return updated;
    }

    public void delete(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Feriado não encontrado");
        }
        holidayRepository.deleteById(id);
        log.info("Feriado deletado | ID: {}", id);
    }

    @Transactional(readOnly = true)
    public List<HolidayEntity> listAll() {
        return holidayRepository.findAll(Sort.by("date"));
    }
}
