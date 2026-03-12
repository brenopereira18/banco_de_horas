package com.banco_de_horas.banco_de_horas.timeOffUsage.service;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.holiday.repository.HolidayRepository;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.MonthlyTimeOffUsageItemDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.TimeOffUsageRequestDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.entity.TimeOffUsageEntity;
import com.banco_de_horas.banco_de_horas.timeOffUsage.repository.TimeOffUsageRepository;
import com.banco_de_horas.banco_de_horas.utils.TimeFormatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TimeOffUsageService {

    private final TimeOffUsageRepository timeOffUsageRepository;
    private final TaxRepository taxRepository;
    private final HolidayRepository holidayRepository;

    public TimeOffUsageEntity create(TimeOffUsageRequestDTO dto) {
        TaxEntity tax = taxRepository.findById(dto.taxId())
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        BigDecimal hoursToDebit = calculateHours(
            dto.startDate(),
            dto.endDate(),
            dto.fractionalHours(),
            tax
        );

        TimeOffUsageEntity usage = TimeOffUsageEntity.builder()
            .taxEntity(tax)
            .solicitationDate(dto.solicitationDate())
            .startDate(dto.startDate())
            .endDate(dto.endDate())
            .fractionalHours(dto.fractionalHours())
            .hoursUsed(hoursToDebit)
            .build();

        applyBusinessRules(usage);
        TimeOffUsageEntity saved = timeOffUsageRepository.save(usage);
        log.info("Folga criada | Fiscal ID: {} | Horas: {}", tax.getId(), hoursToDebit);
        return saved;
    }

    public TimeOffUsageEntity update(Long id, TimeOffUsageRequestDTO dto) {
        TimeOffUsageEntity existing = timeOffUsageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Uso de folga não encontrado"));

        restoreTaxBalance(existing);

        existing.setSolicitationDate(dto.solicitationDate());
        existing.setStartDate(dto.startDate());
        existing.setEndDate(dto.endDate());
        existing.setFractionalHours(dto.fractionalHours());

        BigDecimal hoursToDebit = calculateHours(
            dto.startDate(),
            dto.endDate(),
            dto.fractionalHours(),
            existing.getTaxEntity()
        );
        existing.setHoursUsed(hoursToDebit);
        applyBusinessRules(existing);

        TimeOffUsageEntity updated = timeOffUsageRepository.save(existing);
        log.info("Folga atualizada | ID: {} | Novas horas: {}", id, hoursToDebit);
        return updated;
    }

    public void delete(Long id) {
        TimeOffUsageEntity existing = timeOffUsageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Folga não encontrada"));

        // Devolve as horas da folga ao saldo do fiscal antes de deletar
        TaxEntity tax = existing.getTaxEntity();
        tax.addHours(existing.getHoursUsed());
        taxRepository.save(tax);

        timeOffUsageRepository.delete(existing);
        log.info("Folga deletada | ID: {} | Horas devolvidas: {}", id, existing.getHoursUsed());
    }

    @Transactional(readOnly = true)
    public BigDecimal getHoursUsed(TaxEntity tax) {
        return timeOffUsageRepository.sumAllHoursUsedByTax(tax);
    }

    @Transactional(readOnly = true)
    public Page<MonthlyTimeOffUsageItemDTO> getAllTimeUsage(TaxEntity tax, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return timeOffUsageRepository
            .findByTaxEntityOrderByRegistrationDateDesc(tax, pageable)
            .map(usage -> new MonthlyTimeOffUsageItemDTO(
                usage.getId(),
                usage.getSolicitationDate(),
                usage.getStartDate(),
                usage.getEndDate(),
                usage.getHoursUsed(),
                usage.getFractionalHours(),
                TimeFormatUtils.formatHours(usage.getHoursUsed()),
                calculateBusinessDays(usage.getStartDate(), usage.getEndDate())
            ));
    }

    // Horas fracionadas sem período de dias retornam apenas as horas fracionadas
    private BigDecimal calculateHours(LocalDate start, LocalDate end, BigDecimal fractional,
                                      TaxEntity tax) {
        if (start == null) {
            throw new IllegalArgumentException("Data inicial obrigatória");
        }

        if (end == null && fractional != null && fractional.compareTo(BigDecimal.ZERO) > 0) {
            return fractional;
        }

        BigDecimal hoursPerDay = getDailyLimit(tax);
        LocalDate finalDate = (end != null) ? end : start;

        if (finalDate.isBefore(start)) {
            throw new IllegalArgumentException("Data final inválida");
        }

        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = start;

        while (!current.isAfter(finalDate)) {
            if (isBusinessDay(current)) {
                total = total.add(hoursPerDay);
            }
            current = current.plusDays(1);
        }

        // Horas fracionadas somadas ao total apenas quando há período de dias
        if (fractional != null && fractional.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(fractional);
        }

        return total;
    }

    private boolean isBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        return !holidayRepository.existsByDate(date);
    }

    private long calculateBusinessDays(LocalDate start, LocalDate end) {
        if (start == null) return 0;

        LocalDate finalDate = (end != null) ? end : start;
        long count = 0;
        LocalDate current = start;

        while (!current.isAfter(finalDate)) {
            if (isBusinessDay(current)) {
                count++;
            }
            current = current.plusDays(1);
        }

        return count;
    }

    // Fiscal tem jornada de 6h, supervisor e administrador de 8h
    private BigDecimal getDailyLimit(TaxEntity tax) {
        return switch (tax.getUserType()) {
            case FISCAL -> BigDecimal.valueOf(6);
            case SUPERVISOR, ADMINISTRADOR -> BigDecimal.valueOf(8);
        };
    }

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

    // Restaura o saldo de horas adicionadas
    private void restoreTaxBalance(TimeOffUsageEntity usage) {
        BigDecimal hoursUsed = usage.getHoursUsed();

        if (hoursUsed != null && hoursUsed.compareTo(BigDecimal.ZERO) > 0) {
            TaxEntity tax = usage.getTaxEntity();
            tax.addHours(hoursUsed);
            taxRepository.save(tax);
        }
    }
}
