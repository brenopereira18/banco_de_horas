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
public class TimeOffUsageService {

    @Autowired
    private TimeOffUsageRepository timeOffUsageRepository;

    @Autowired
    private TaxRepository taxRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    /**
     * Cria folga
     */
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
        return timeOffUsageRepository.save(usage);
    }

    /**
     * Atualiza folga
     */
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

        return timeOffUsageRepository.save(existing);
    }

    /**
     * Calcula horas usadas na folga
     */
    private BigDecimal calculateHours(LocalDate start, LocalDate end, BigDecimal fractional,
        TaxEntity tax) {
        if (start == null) {
            throw new IllegalArgumentException("Data inicial obrigatória");
        }

        BigDecimal total = BigDecimal.ZERO;

        // Se só tem horas fracionadas (sem período de dias), retorna só elas
        if (end == null && fractional != null && fractional.compareTo(BigDecimal.ZERO) > 0) {
            return fractional;
        }

        BigDecimal hoursPerDay = getDailyLimit(tax);
        LocalDate finalDate = (end != null) ? end : start;

        if (finalDate.isBefore(start)) {
            throw new IllegalArgumentException("Data final inválida");
        }

        LocalDate current = start;

        while (!current.isAfter(finalDate)) {
            if (isBusinessDay(current)) {
                total = total.add(hoursPerDay);
            }
            current = current.plusDays(1);
        }

        // horas extras (só soma se tiver período de dias também)
        if (fractional != null && fractional.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(fractional);
        }

        return total;
    }

    /**
     * Verifica dia da semana
     */
    private boolean isBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // fim de semana
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // feriado
        return !holidayRepository.existsByDate(date);
    }

    /**
     * conta quantos dias úteis foram de folga
     */
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

    /**
     * limite de horas por dia
     */
    private BigDecimal getDailyLimit(TaxEntity tax) {
        return switch (tax.getUserType()) {
            case FISCAL -> BigDecimal.valueOf(6);
            case SUPERVISOR, ADMINISTRADOR -> BigDecimal.valueOf(8);
        };
    }

    /**
     * Aplica regras e debita saldo
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
     * Reverte saldo
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
     * Horas usadas em folga
     */
    public BigDecimal getHoursUsed(TaxEntity tax) {
        return timeOffUsageRepository.sumAllHoursUsedByTax(tax);
    }

    /**
     * busca folgas do usuário por ordem de cadastro
     */
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

    public void delete(Long id) {
        TimeOffUsageEntity existing = timeOffUsageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Folga não encontrada"));

        TaxEntity tax = existing.getTaxEntity();
        tax.addHours(existing.getHoursUsed());
        taxRepository.save(tax);
        timeOffUsageRepository.delete(existing);
    }
}
