package com.banco_de_horas.banco_de_horas.timeOffUsage.service;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.entity.UserType;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.TimeOffUsageRequestDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.entity.TimeOffUsageEntity;
import com.banco_de_horas.banco_de_horas.timeOffUsage.repository.TimeOffUsageRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class TimeOffUsageService {

    @Autowired
    private TimeOffUsageRepository timeOffUsageRepository;

    @Autowired
    private TaxRepository taxRepository;

    public TimeOffUsageEntity create(TimeOffUsageRequestDTO dto) {
        TaxEntity tax = taxRepository.findById(dto.taxId())
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        long daysOff = calculateDaysOff(
            dto.startDateTime(),
            dto.endDateTime()
        );

        BigDecimal hoursToDebit = convertDaysToHours(daysOff, tax.getUserType());

        TimeOffUsageEntity usage = TimeOffUsageEntity.builder()
            .taxEntity(tax)
            .startDateTime(dto.startDateTime())
            .endDateTime(dto.endDateTime())
            .hoursUsed(hoursToDebit)
            .build();

        applyBusinessRules(usage);
        return timeOffUsageRepository.save(usage);
    }

    public TimeOffUsageEntity update(Long id, TimeOffUsageEntity updated) {
        TimeOffUsageEntity existing = timeOffUsageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Uso de folga não encontrado"));

        // Reverte saldo antigo
        restoreTaxBalance(existing);

        existing.setStartDateTime(updated.getStartDateTime());
        existing.setEndDateTime(updated.getEndDateTime());

        applyBusinessRules(existing);

        return timeOffUsageRepository.save(existing);
    }

    private void applyBusinessRules(TimeOffUsageEntity usage) {
        TaxEntity tax = usage.getTaxEntity();
        BigDecimal hoursUsed = usage.getHoursUsed();

        if (hoursUsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Horas utilizadas inválidas");
        }

        if (tax.getBalanceOfHours().compareTo(hoursUsed) < 0) {
            throw new IllegalStateException("Saldo de horas insuficiente");
        }

        tax.subtractHours(hoursUsed);
        taxRepository.save(tax);
    }

    private long calculateDaysOff(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(
            start.toLocalDate(),
            end.toLocalDate()
        ) + 1;
    }

    private BigDecimal convertDaysToHours(long days, UserType userType) {

        BigDecimal hoursPerDay = switch (userType) {
            case FISCAL -> BigDecimal.valueOf(6);
            case SUPERVISOR, ADMINISTRADOR -> BigDecimal.valueOf(8);
        };

        return hoursPerDay.multiply(BigDecimal.valueOf(days));
    }

    private void restoreTaxBalance(TimeOffUsageEntity usage) {

        TaxEntity tax = usage.getTaxEntity();

        BigDecimal hoursUsed = usage.getHoursUsed();

        if (hoursUsed != null && hoursUsed.compareTo(BigDecimal.ZERO) > 0) {
            tax.addHours(hoursUsed);
            taxRepository.save(tax);
        }
    }
}
