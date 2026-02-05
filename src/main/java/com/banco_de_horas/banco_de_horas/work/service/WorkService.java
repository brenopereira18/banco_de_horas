package com.banco_de_horas.banco_de_horas.work.service;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.holiday.repository.HolidayRepository;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import com.banco_de_horas.banco_de_horas.utils.TimeFormatUtils;
import com.banco_de_horas.banco_de_horas.work.dto.MonthlyWorkItemDTO;
import com.banco_de_horas.banco_de_horas.work.dto.WorkRequestDTO;
import com.banco_de_horas.banco_de_horas.work.dto.WorkResponseDTO;
import com.banco_de_horas.banco_de_horas.work.entity.WorkEntity;
import com.banco_de_horas.banco_de_horas.work.repository.WorkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
            .description(dto.description())
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
            work.getDescription(),
            work.getStartDateTime(),
            work.getEndDateTime(),
            work.getHoursWorked(),
            work.getGeneratedTimeOff(),
            work.getDetailedCalculation(),
            work.getRegistrationDate()
        );
    }

    public WorkEntity update(Long id, WorkRequestDTO updated) {
        WorkEntity existing = workRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        TaxEntity tax = existing.getTaxEntity();
        tax.subtractHours(existing.getGeneratedTimeOff());

        existing.setDescription(updated.description());
        existing.setStartDateTime(updated.startDateTime());
        existing.setEndDateTime(updated.endDateTime());

        calculateAndApply(existing);
        return workRepository.save(existing);
    }

    /**
     * Calcula as horas geradas por serviço
     */
    private void calculateAndApply(WorkEntity work) {
        BigDecimal totalGeneratedMinutes = BigDecimal.ZERO;
        StringBuilder details = new StringBuilder();

        LocalDateTime current = work.getStartDateTime();
        LocalDateTime end = work.getEndDateTime();

        while (current.isBefore(end)) {

            LocalDateTime nextHour = current.plusHours(1);
            if (nextHour.isAfter(end)) {
                nextHour = end;
            }

            // Trabalha SEMPRE em minutos inteiros
            BigDecimal workedMinutes = BigDecimal.valueOf(
                Duration.between(current, nextHour).toMinutes()
            );

            BigDecimal multiplier = getMultiplier(current);

            // Aplica multiplicador em minutos
            BigDecimal generatedMinutes = workedMinutes.multiply(multiplier);

            totalGeneratedMinutes = totalGeneratedMinutes.add(generatedMinutes);

            // Apenas para exibição
            BigDecimal generatedHoursForDetail = generatedMinutes
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            details.append(formatDetail(
                current,
                nextHour,
                multiplier,
                generatedHoursForDetail
            ));

            current = nextHour;
        }

        // Converte para horas
        BigDecimal totalGeneratedHours = totalGeneratedMinutes
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        work.setGeneratedTimeOff(totalGeneratedHours);
        work.setDetailedCalculation(details.toString());

        // Atualiza saldo do fiscal
        TaxEntity tax = work.getTaxEntity();
        tax.addHours(totalGeneratedHours);
        taxRepository.save(tax);
    }

    /**
     * Pega o bônus conforme o dia do serviço
     */
    private BigDecimal getMultiplier(LocalDateTime dateTime) {

        boolean isHoliday = holidayRepository.existsByDate(dateTime.toLocalDate());
        DayOfWeek day = dateTime.getDayOfWeek();
        int hour = dateTime.getHour();

        // Domingo ou feriado
        if (day == DayOfWeek.SUNDAY || isHoliday) {
            if (hour >= 5 && hour < 22) {
                return BigDecimal.valueOf(2.0);      // 05h às 22h: +100%
            } else {
                return BigDecimal.valueOf(2.5);      // 22h às 00h e 00h às 05h: +150%
            }
        }

        // Segunda a sábado
        if (hour >= 5 && hour < 22) {
            return BigDecimal.valueOf(1.5);          // 05h às 22h: +50%
        } else {
            return BigDecimal.valueOf(1.87);         // 22h às 00h e 00h às 05h: +87%
        }
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

    /**
     * busca quantos serviços o fiscal fez no mês e quantas horas foram geradas
     */
    public BigDecimal getNumberOfHoursGenerated(TaxEntity tax) {
        return workRepository.sumAllHoursGeneratedByTax(tax);
    }

    /**
     * busca todos os serviços de um usuário ordenados por data de cadastro
     */
    public Page<MonthlyWorkItemDTO> getAllWorks(TaxEntity tax, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return workRepository
            .findByTaxEntityOrderByRegistrationDateDesc(tax, pageable)
            .map(work -> {
                boolean isHoliday = hasHoliday(
                    work.getStartDateTime(),
                    work.getEndDateTime()
                );

                return new MonthlyWorkItemDTO(
                    work.getId(),
                    work.getDescription(),
                    work.getStartDateTime(),
                    work.getEndDateTime(),
                    isHoliday,
                    work.getGeneratedTimeOff(),
                    TimeFormatUtils.formatHours(work.getGeneratedTimeOff())
                );
            });
    }

    private boolean hasHoliday(LocalDateTime start, LocalDateTime end) {
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            if (holidayRepository.existsByDate(currentDate)) {
                return true;
            }
            currentDate = currentDate.plusDays(1);
        }

        return false;
    }

    public void delete(Long id) {
        WorkEntity existing = workRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        TaxEntity tax = existing.getTaxEntity();
        tax.subtractHours(existing.getGeneratedTimeOff());
        taxRepository.save(tax);
        workRepository.delete(existing);
    }
}
