package com.banco_de_horas.banco_de_horas.timeOffUsage.service;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.entity.UserType;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.MonthLyTimeOffSummaryDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.TimeOffUsageRequestDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.entity.TimeOffUsageEntity;
import com.banco_de_horas.banco_de_horas.timeOffUsage.repository.TimeOffUsageRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
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

    /**
     * Atualização de uso de folga
     */
    public TimeOffUsageEntity update(Long id, TimeOffUsageRequestDTO dto) {

        TimeOffUsageEntity existing = timeOffUsageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Uso de folga não encontrado"));

        // Reverte saldo antigo
        restoreTaxBalance(existing);

        // Atualiza datas
        existing.setStartDateTime(dto.startDateTime());
        existing.setEndDateTime(dto.endDateTime());

        // Recalcula horas administrativas
        long daysOff = calculateDaysOff(
            dto.startDateTime(),
            dto.endDateTime()
        );

        BigDecimal hoursToDebit = convertDaysToHours(
            daysOff,
            existing.getTaxEntity().getUserType()
        );

        existing.setHoursUsed(hoursToDebit);

        // Aplica regras novamente
        applyBusinessRules(existing);

        return timeOffUsageRepository.save(existing);
    }

    /**
     * Aplica regras de negócio e debita saldo
     */
    private void applyBusinessRules(TimeOffUsageEntity usage) {

        TaxEntity tax = usage.getTaxEntity();
        BigDecimal hoursUsed = usage.getHoursUsed();

        if (hoursUsed == null || hoursUsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Horas utilizadas inválidas");
        }

        if (tax.getBalanceOfHours().compareTo(hoursUsed) < 0) {
            throw new IllegalStateException("Saldo de horas insuficiente");
        }

        tax.subtractHours(hoursUsed);
        taxRepository.save(tax);
    }

    /**
     * Reverte saldo quando um uso de folga é alterado
     */
    private void restoreTaxBalance(TimeOffUsageEntity usage) {
        BigDecimal hoursUsed = usage.getHoursUsed();

        if (hoursUsed != null && hoursUsed.compareTo(BigDecimal.ZERO) > 0) {
            TaxEntity tax = usage.getTaxEntity();
            tax.addHours(hoursUsed);
            taxRepository.save(tax);
        }
    }

    /**
     * Calcula dias administrativos (datas inclusivas)
     */
    private long calculateDaysOff(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Data final não pode ser anterior à data inicial");
        }

        return ChronoUnit.DAYS.between(
            start.toLocalDate(),
            end.toLocalDate()
        ) + 1;
    }

    /**
     * Converte dias administrativos em horas conforme o tipo do usuário
     */
    private BigDecimal convertDaysToHours(long days, UserType userType) {
        BigDecimal hoursPerDay = switch (userType) {
            case FISCAL -> BigDecimal.valueOf(6);
            case SUPERVISOR, ADMINISTRADOR -> BigDecimal.valueOf(8);
        };

        return hoursPerDay.multiply(BigDecimal.valueOf(days));
    }

    /**
     * Conta quantas folgas e horas foram usadas no mês
     */
    public MonthLyTimeOffSummaryDTO getMonthLySummary(TaxEntity tax) {
        YearMonth currentMonth = YearMonth.now();

        LocalDateTime start = currentMonth
            .atDay(1)
            .atStartOfDay();

        LocalDateTime end = currentMonth
            .atEndOfMonth()
            .atTime(23, 59, 59);

        Long totalTimeOffs =
            timeOffUsageRepository.countByTaxAndPeriod(tax, start, end);

        BigDecimal totalHoursUsed =
            timeOffUsageRepository.sumHoursUsedByTaxAndPeriod(tax, start, end);

        return new MonthLyTimeOffSummaryDTO(
            totalTimeOffs,
            totalHoursUsed
        );
    }
}
