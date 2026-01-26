package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.holiday.dto.HolidayRequestDTO;
import com.banco_de_horas.banco_de_horas.holiday.entity.HolidayEntity;
import com.banco_de_horas.banco_de_horas.holiday.service.HolidayService;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.service.TaxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/banco_de_horas/dashboard")
public class DashboardController {

    @Autowired
    private TaxService taxService;

    @Autowired
    private HolidayService holidayService;

    @GetMapping("/administrador")
    public String showDashboard(Model model,  @RequestParam(defaultValue = "fiscais") String tab) {
        List<TaxEntity> fiscais = taxService.getAllTax();
        List<HolidayEntity> feriados = holidayService.listAll();

        model.addAttribute("fiscais", fiscais);
        model.addAttribute("feriados", feriados);
        model.addAttribute("activeTab", tab);
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

    @PostMapping("/criar/feriado")
    public String addHoliday(@ModelAttribute HolidayRequestDTO holidayRequestDTO,
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
    public String editHoliday(@ModelAttribute HolidayRequestDTO holidayRequestDTO,
                              @RequestParam Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            holidayService.update(id, holidayRequestDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                "Feriado atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao atualizar feriado: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/administrador?tab=feriados";
    }

    @PostMapping("/excluir/feriado/{id}")
    public String deleteHoliday(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            holidayService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Feriado excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao excluir feriado: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/administrador?tab=feriados";
    }
}
