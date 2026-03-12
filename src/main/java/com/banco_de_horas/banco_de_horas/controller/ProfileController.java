package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.dto.UpdateProfileRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.service.TaxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/banco_de_horas/dashboard")
@RequiredArgsConstructor
public class ProfileController {

    private final TaxService taxService;

    @PostMapping("/alterar-senha")
    public String changePassword(
        @ModelAttribute UpdateProfileRequestDTO dto,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {

        TaxEntity user = (TaxEntity) authentication.getPrincipal();

        String validationError = taxService.validateUpdateProfile(user.getId(), dto);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("errorMessage", validationError);
            return "redirect:/banco_de_horas/dashboard/fiscal/" + user.getId();
        }

        try {
            taxService.updateProfile(user.getId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Senha alterada com sucesso!");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/banco_de_horas/dashboard/fiscal/" + user.getId();
    }

    @PostMapping("/alterar-email")
    public String changeEmail(
        @ModelAttribute UpdateProfileRequestDTO dto,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {

        TaxEntity user = (TaxEntity) authentication.getPrincipal();

        String validationError = taxService.validateUpdateProfile(user.getId(), dto);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("errorMessage", validationError);
            return "redirect:/banco_de_horas/dashboard/fiscal/" + user.getId();
        }

        try {
            taxService.updateProfile(user.getId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "E-mail alterado com sucesso!");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/banco_de_horas/dashboard/fiscal/" + user.getId();
    }
}
