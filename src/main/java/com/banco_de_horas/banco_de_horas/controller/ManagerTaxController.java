package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.service.TaxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Slf4j
@Controller
@RequestMapping("/banco_de_horas/dashboard/administrador")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ManagerTaxController {

    private final TaxService taxService;

    @PostMapping("/criar/fiscal")
    public String addFiscal(
        @ModelAttribute TaxRequestDTO taxRequestDTO,
        RedirectAttributes redirectAttributes) {

        try {
            taxService.create(taxRequestDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                "Fiscal " + taxRequestDTO.fullName() + " cadastrado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao cadastrar fiscal: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/administrador";
    }

    @PostMapping("/deletar/fiscal")
    public String deleteFiscal(
        @RequestParam String registration,
        RedirectAttributes redirectAttrs) {

        try {
            taxService.delete(registration);
            redirectAttrs.addFlashAttribute("successMessage", "Fiscal removido com sucesso.");
        } catch (ResourceNotFoundException e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Fiscal não encontrado.");
        }
        return "redirect:/banco_de_horas/dashboard/administrador";
    }

    @PostMapping("/fiscal/{id}/add-hours")
    public String addHoursToFiscal(
        @PathVariable Long id,
        @RequestParam BigDecimal hours) {

        taxService.addHours(id, hours);
        return "redirect:/banco_de_horas/dashboard/administrador";
    }

    @PostMapping("/fiscal/{id}/revert-hours")
    public String revertHours(@PathVariable Long id) {
        taxService.revertLastAddedHours(id);
        return "redirect:/banco_de_horas/dashboard/administrador";
    }
}
