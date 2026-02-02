package com.banco_de_horas.banco_de_horas.timeOffUsage.service;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.MonthlyTimeOffSummaryDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.MonthlyTimeOffUsageItemDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.TimeOffUsageRequestDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.entity.TimeOffUsageEntity;
import com.banco_de_horas.banco_de_horas.timeOffUsage.repository.TimeOffUsageRepository;
import com.banco_de_horas.banco_de_horas.utils.TimeFormatUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class TimeOffUsageService {

    @Autowired
    private TimeOffUsageRepository timeOffUsageRepository;

    @Autowired
    private TaxRepository taxRepository;

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
        BigDecimal hoursPerDay = getDailyLimit(tax);

        // dias
        if (end != null) {
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("Data final inválida");
            }

            long days = ChronoUnit.DAYS.between(start, end) + 1;

            total = total.add(
                hoursPerDay.multiply(BigDecimal.valueOf(days))
            );
        }

        // horas extras
        if (fractional != null && fractional.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(fractional);
        }

        return total;
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
     * Conta quantas folgas e horas foram usadas no mês
     */
    public MonthlyTimeOffSummaryDTO getMonthlySummary(TaxEntity tax) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        Long totalTimeOffs =
            timeOffUsageRepository.countByTaxAndPeriod(tax, start, end);

        BigDecimal totalHoursUsed =
            timeOffUsageRepository.sumHoursUsedByTaxAndPeriod(tax, start, end);

        return new MonthlyTimeOffSummaryDTO(
            totalTimeOffs,
            totalHoursUsed
        );
    }

    /**
     * busca folgas do mês de um usuário
     */
    public List<MonthlyTimeOffUsageItemDTO> getMonthlyTimeUsage(TaxEntity tax) {
        YearMonth month = YearMonth.now();
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        return timeOffUsageRepository
            .findByTaxEntityAndRegistrationDateBetweenOrderByRegistrationDateDesc(
                tax, start, end
            )
            .stream()
            .map(usage -> new MonthlyTimeOffUsageItemDTO(
                usage.getId(),
                usage.getStartDate(),
                usage.getEndDate(),
                usage.getHoursUsed(),
                usage.getFractionalHours(),
                TimeFormatUtils.formatHours(usage.getHoursUsed())
            ))
            .toList();
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
