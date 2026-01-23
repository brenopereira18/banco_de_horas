package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.tax.dto.TaxRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.service.TaxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/banco_de_horas/dashboard")
public class DashboardController {

    @Autowired
    private TaxService taxService;

    @GetMapping("/administrador")
    public String showDashboard(Model model) {
        List<TaxEntity> fiscais = taxService.getAllTax();
        model.addAttribute("fiscais", fiscais);
        model.addAttribute("userName", "Eduardo");
        return "dashboardManager";
    }

    @PostMapping("/criar/fiscal")
    public String addFiscal(@ModelAttribute TaxRequestDTO taxRequestDTO,
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
}
