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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;
    private final TaxRepository taxRepository;
    private final HolidayRepository holidayRepository;

    public WorkResponseDTO create(WorkRequestDTO dto) {
        TaxEntity fiscal = taxRepository.findById(dto.taxId())
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        validateDateTimes(dto.startDateTime(), dto.endDateTime());

        WorkEntity work = WorkEntity.builder()
            .taxEntity(fiscal)
            .startDateTime(dto.startDateTime())
            .endDateTime(dto.endDateTime())
            .description(dto.description())
            .build();

        calculateAndApply(work);

        WorkEntity saved = workRepository.save(work);
        log.info("Serviço criado | Fiscal ID: {} | Horas geradas: {}",
            fiscal.getId(), saved.getGeneratedTimeOff());
        return mapToResponse(saved);
    }

    public void createBatch(Long taxId, List<WorkRequestDTO> services) {
        for (WorkRequestDTO dto : services) {
            WorkRequestDTO normalized = new WorkRequestDTO(
                taxId,
                dto.description(),
                dto.startDateTime(),
                dto.endDateTime()
            );
            create(normalized);
        }
        log.info("Lote de serviços criado | Fiscal ID: {} | Quantidade: {}",
            taxId, services.size());
    }

    public WorkEntity update(Long id, WorkRequestDTO updated) {
        WorkEntity existing = workRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        validateDateTimes(updated.startDateTime(), updated.endDateTime());

        // Reverte horas antigas antes de recalcular
        TaxEntity tax = existing.getTaxEntity();
        tax.subtractHours(existing.getGeneratedTimeOff());
        taxRepository.save(tax);

        existing.setDescription(updated.description());
        existing.setStartDateTime(updated.startDateTime());
        existing.setEndDateTime(updated.endDateTime());

        calculateAndApply(existing);
        WorkEntity saved = workRepository.save(existing);
        log.info("Serviço atualizado | ID: {} | Novas horas geradas: {}",
            id, saved.getGeneratedTimeOff());
        return saved;
    }

    public void delete(Long id) {
        WorkEntity existing = workRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        // Reverte as horas geradas pelo serviço ao deletar
        TaxEntity tax = existing.getTaxEntity();
        tax.subtractHours(existing.getGeneratedTimeOff());
        taxRepository.save(tax);

        workRepository.delete(existing);
        log.info("Serviço deletado | ID: {} | Horas revertidas: {}",
            id, existing.getGeneratedTimeOff());
    }

    @Transactional(readOnly = true)
    public BigDecimal getNumberOfHoursGenerated(TaxEntity tax) {
        return workRepository.sumAllHoursGeneratedByTax(tax);
    }

    @Transactional(readOnly = true)
    public Page<MonthlyWorkItemDTO> getAllWorks(TaxEntity tax, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return workRepository
            .findByTaxEntityOrderByRegistrationDateDesc(tax, pageable)
            .map(this::mapToWorkItem);
    }

    // Multiplica horas trabalhadas pelo bônus do horário — trabalha em minutos para evitar arredondamento
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

            BigDecimal workedMinutes = BigDecimal.valueOf(
                Duration.between(current, nextHour).toMinutes()
            );

            BigDecimal multiplier = getMultiplier(current);
            BigDecimal generatedMinutes = workedMinutes.multiply(multiplier);
            totalGeneratedMinutes = totalGeneratedMinutes.add(generatedMinutes);

            BigDecimal generatedHoursForDetail = generatedMinutes
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            details.append(formatDetail(current, nextHour, multiplier, generatedHoursForDetail));

            current = nextHour;
        }

        BigDecimal totalGeneratedHours = totalGeneratedMinutes
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        work.setGeneratedTimeOff(totalGeneratedHours);
        work.setDetailedCalculation(details.toString());

        TaxEntity tax = work.getTaxEntity();
        tax.addHours(totalGeneratedHours);
        taxRepository.save(tax);
    }

    // Domingo e feriado têm bônus maior — regra definida pela prefeitura
    private BigDecimal getMultiplier(LocalDateTime dateTime) {
        boolean isHoliday = holidayRepository.existsByDate(dateTime.toLocalDate());
        DayOfWeek day = dateTime.getDayOfWeek();
        int hour = dateTime.getHour();

        if (day == DayOfWeek.SUNDAY || isHoliday) {
            return hour >= 5 && hour < 22
                ? BigDecimal.valueOf(2.0)    // 05h às 22h: +100%
                : BigDecimal.valueOf(2.5);   // 22h às 00h e 00h às 05h: +150%
        }

        return hour >= 5 && hour < 22
            ? BigDecimal.valueOf(1.5)        // 05h às 22h: +50%
            : BigDecimal.valueOf(1.87);      // 22h às 00h e 00h às 05h: +87%
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

    private void validateDateTimes(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        if (start.isAfter(now))
            throw new IllegalArgumentException("Data de início não pode ser no futuro");
        if (end.isAfter(now))
            throw new IllegalArgumentException("Data de fim não pode ser no futuro");
        if (!end.isAfter(start))
            throw new IllegalArgumentException("Data de fim deve ser após o início");
    }

    private MonthlyWorkItemDTO mapToWorkItem(WorkEntity work) {
        boolean isHoliday = hasHoliday(work.getStartDateTime(), work.getEndDateTime());
        return new MonthlyWorkItemDTO(
            work.getId(),
            work.getDescription(),
            work.getStartDateTime(),
            work.getEndDateTime(),
            isHoliday,
            work.getGeneratedTimeOff(),
            TimeFormatUtils.formatHours(work.getGeneratedTimeOff())
        );
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
}
