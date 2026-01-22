package com.banco_de_horas.banco_de_horas.holiday.service;

import com.banco_de_horas.banco_de_horas.exceptions.EntityAlreadyExists;
import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.holiday.dto.HolidayRequestDTO;
import com.banco_de_horas.banco_de_horas.holiday.entity.HolidayEntity;
import com.banco_de_horas.banco_de_horas.holiday.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@Transactional
public class HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    public HolidayEntity create(HolidayRequestDTO dto) {
        if (holidayRepository.existsByDate(dto.date())) {
            throw new EntityAlreadyExists("Já existe feriado cadastrado nessa data");
        }

        HolidayEntity holiday = HolidayEntity.builder()
            .date(dto.date())
            .description(dto.description())
            .build();

        return holidayRepository.save(holiday);
    }

    public HolidayEntity update(Long id, HolidayRequestDTO dto) {
        HolidayEntity existing = holidayRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Feriado não encontrado"));

        if (!existing.getDate().equals(dto.date())
            && holidayRepository.existsByDate(dto.date())) {
            throw new EntityAlreadyExists("Já existe feriado nessa data");
        }

        existing.setDate(dto.date());
        existing.setDescription(dto.description());
        return holidayRepository.save(existing);
    }

    public void delete(Long id) {
        HolidayEntity existing = holidayRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Feriado não encontrado"));

        holidayRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<HolidayEntity> listAll() {
        return holidayRepository.findAll(Sort.by("date"));
    }
}
