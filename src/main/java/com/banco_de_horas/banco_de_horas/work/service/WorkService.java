package com.banco_de_horas.banco_de_horas.work.service;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.holiday.repository.HolidayRepository;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import com.banco_de_horas.banco_de_horas.work.dto.MonthLyWorkSummaryDTO;
import com.banco_de_horas.banco_de_horas.work.dto.WorkRequestDTO;
import com.banco_de_horas.banco_de_horas.work.dto.WorkResponseDTO;
import com.banco_de_horas.banco_de_horas.work.entity.WorkEntity;
import com.banco_de_horas.banco_de_horas.work.repository.WorkRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class WorkService {

    @Autowired
    private WorkRepository workRepository;

    @Autowired
    private TaxRepository taxRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    public WorkResponseDTO create(WorkRequestDTO dto) {

        TaxEntity fiscal = taxRepository.findById(dto.taxId())
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        WorkEntity work = WorkEntity.builder()
            .taxEntity(fiscal)
            .startDateTime(dto.startDateTime())
            .endDateTime(dto.endDateTime())
            .build();

        calculateAndApply(work);

        WorkEntity saved = workRepository.save(work);

        return mapToResponse(saved);
    }

    private WorkResponseDTO mapToResponse(WorkEntity work) {
        return new WorkResponseDTO(
            work.getId(),
            work.getTaxEntity().getId(),
            work.getTaxEntity().getFullName(),
            work.getStartDateTime(),
            work.getEndDateTime(),
            work.getHoursWorked(),
            work.getGeneratedTimeOff(),
            work.getDetailedCalculation(),
            work.getRegistrationDate()
        );
    }

    public WorkEntity update(Long id, WorkEntity updated) {
        WorkEntity existing = workRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        existing.setStartDateTime(updated.getStartDateTime());
        existing.setEndDateTime(updated.getEndDateTime());
        existing.setTaxEntity(updated.getTaxEntity());

        calculateAndApply(existing);
        return workRepository.save(existing);
    }

    private void calculateAndApply(WorkEntity work) {

        BigDecimal totalGeneratedTimeOff = BigDecimal.ZERO;
        StringBuilder details = new StringBuilder();

        LocalDateTime current = work.getStartDateTime();
        LocalDateTime end = work.getEndDateTime();

        while (current.isBefore(end)) {

            LocalDateTime nextHour = current.plusHours(1);
            if (nextHour.isAfter(end)) {
                nextHour = end;
            }

            BigDecimal workedHours = BigDecimal.valueOf(
                Duration.between(current, nextHour).toMinutes()
            ).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            BigDecimal multiplier = getMultiplier(current);

            BigDecimal generated = workedHours.multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);

            totalGeneratedTimeOff = totalGeneratedTimeOff.add(generated);

            details.append(formatDetail(current, nextHour, multiplier, generated));

            current = nextHour;
        }

        work.setGeneratedTimeOff(totalGeneratedTimeOff);
        work.setDetailedCalculation(details.toString());

        // Atualiza saldo do fiscal
        TaxEntity fiscal = work.getTaxEntity();
        fiscal.addHours(totalGeneratedTimeOff);
        taxRepository.save(fiscal);
    }

    private BigDecimal getMultiplier(LocalDateTime dateTime) {

        boolean isHoliday = holidayRepository.existsByDate(dateTime.toLocalDate());
        DayOfWeek day = dateTime.getDayOfWeek();
        int hour = dateTime.getHour();

        // Segunda madrugada
        if (day == DayOfWeek.MONDAY && hour < 5) {
            return BigDecimal.valueOf(2.5);
        }

        // Domingo ou feriado
        if (day == DayOfWeek.SUNDAY || isHoliday) {
            if (hour < 5) return BigDecimal.valueOf(1.87);
            if (hour < 22) return BigDecimal.valueOf(2.0);
            return BigDecimal.valueOf(2.5);
        }

        // Segunda a sábado
        if (hour < 5) {
            return BigDecimal.valueOf(1.5); // regra padrão (se existir)
        }
        if (hour < 22) {
            return BigDecimal.valueOf(1.5);
        }
        return BigDecimal.valueOf(1.87);
    }

    private String formatDetail(
        LocalDateTime start,
        LocalDateTime end,
        BigDecimal multiplier,
        BigDecimal generated
    ) {
        return String.format(
            "%s às %s → x%s = %sh%n",
            start.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
            end.format(DateTimeFormatter.ofPattern("HH:mm")),
            multiplier,
            generated
        );
    }

    public MonthLyWorkSummaryDTO getMonthLySummary(TaxEntity tax) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth
            .atDay(1)
            .atStartOfDay();

        LocalDateTime end = currentMonth
            .atEndOfMonth()
            .atTime(23, 59, 59);

        Long totalServices = workRepository.countByTaxAndPeriod(tax, start, end);
        BigDecimal totalHours = workRepository.sumHoursGeneratedByTaxAndPeriod(tax, start, end);

        return new MonthLyWorkSummaryDTO(
            totalServices,
            totalHours
        );
    }
}
