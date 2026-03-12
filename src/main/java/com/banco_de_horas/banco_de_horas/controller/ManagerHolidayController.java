package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.holiday.dto.HolidayRequestDTO;
import com.banco_de_horas.banco_de_horas.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/banco_de_horas/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ManagerHolidayController {

    private final HolidayService holidayService;

    @PostMapping("/administrador/criar/feriado")
    public String addHoliday(
        @ModelAttribute HolidayRequestDTO holidayRequestDTO,
        RedirectAttributes redirectAttributes) {

        try {
            holidayService.create(holidayRequestDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                "Feriado " + holidayRequestDTO.description() + " cadastrado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao cadastrar feriado: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/administrador?tab=feriados";
    }

    @PostMapping("/editar/feriado")
    public String editHoliday(
        @ModelAttribute HolidayRequestDTO holidayRequestDTO,
        @RequestParam Long id,
        RedirectAttributes redirectAttributes) {

        try {
            holidayService.update(id, holidayRequestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Feriado atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao atualizar feriado: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/administrador?tab=feriados";
    }

    @PostMapping("/excluir/feriado/{id}")
    public String deleteHoliday(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes) {

        try {
            holidayService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Feriado excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao excluir feriado: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/administrador?tab=feriados";
    }
}
