package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.TimeOffUsageRequestDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.service.TimeOffUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Controller
@RequestMapping("/banco_de_horas/dashboard/fiscal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class TimeOffController {

    private final TimeOffUsageService timeOffUsageService;

    @PostMapping("/{taxId}/timeoff")
    public String createTimeOff(
        @PathVariable Long taxId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate solicitationDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) BigDecimal partialHours,
        RedirectAttributes redirectAttributes) {

        try {
            TimeOffUsageRequestDTO dto = new TimeOffUsageRequestDTO(taxId, solicitationDate, startDate, endDate, partialHours);
            timeOffUsageService.create(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Folga registrada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao registrar folga: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PostMapping("/{taxId}/editar/folga")
    public String editTimeOff(
        @PathVariable Long taxId,
        @RequestParam Long timeOffId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate solicitationDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) BigDecimal partialHours,
        RedirectAttributes redirectAttributes) {

        try {
            TimeOffUsageRequestDTO dto = new TimeOffUsageRequestDTO(taxId, solicitationDate, startDate, endDate, partialHours);
            timeOffUsageService.update(timeOffId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Folga atualizada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao atualizar folga: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PostMapping("/excluir/folga/{id}")
    public String deleteTimeOff(
        @PathVariable Long id,
        @RequestParam Long taxId,
        RedirectAttributes redirectAttributes) {

        try {
            timeOffUsageService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Folga excluída com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao excluir folga: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }
}
