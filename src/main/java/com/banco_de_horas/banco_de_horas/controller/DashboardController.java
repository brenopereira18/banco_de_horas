package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.holiday.dto.HolidayRequestDTO;
import com.banco_de_horas.banco_de_horas.holiday.entity.HolidayEntity;
import com.banco_de_horas.banco_de_horas.holiday.service.HolidayService;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.service.TaxService;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.MonthlyTimeOffSummaryDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.MonthlyTimeOffUsageItemDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.TimeOffUsageRequestDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.service.TimeOffUsageService;
import com.banco_de_horas.banco_de_horas.utils.TimeFormatUtils;
import com.banco_de_horas.banco_de_horas.work.dto.MonthlyWorkSummaryDTO;
import com.banco_de_horas.banco_de_horas.work.dto.MonthlyWorkItemDTO;
import com.banco_de_horas.banco_de_horas.work.dto.WorkRequestDTO;
import com.banco_de_horas.banco_de_horas.work.service.WorkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/banco_de_horas/dashboard")
public class DashboardController {

    @Autowired
    private TaxService taxService;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private WorkService workService;

    @Autowired
    private TimeOffUsageService timeOffUsageService;

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

    @GetMapping("/fiscal/{id}")
    public String showFiscalDetails(@PathVariable Long id, Model model) {
        try {
            TaxEntity tax = taxService.findById(id);
            MonthlyWorkSummaryDTO workSummary = workService.getMonthLySummary(tax);
            List<MonthlyWorkItemDTO> works = workService.getMonthlyWorks(tax);
            MonthlyTimeOffSummaryDTO timeOffSummary = timeOffUsageService.getMonthlySummary(tax);
            List<MonthlyTimeOffUsageItemDTO> timeOffUsage = timeOffUsageService.getMonthlyTimeUsage(tax);

            model.addAttribute("daysOffPerMonth", timeOffUsage);
            model.addAttribute("formattedBalance", TimeFormatUtils.formatHours(tax.getBalanceOfHours()));
            model.addAttribute("timeOffSummaryPerMonth", TimeFormatUtils.formatHours(timeOffSummary.totalHoursUsed()));
            model.addAttribute("NumberTimeOffSummaryIntheMonth", timeOffSummary.totalTimeOffs());
            model.addAttribute("workingHoursPerMonth", TimeFormatUtils.formatHours(workSummary.totalHoursGenerated()));
            model.addAttribute("numberOfServicesInTheMonth", workSummary.totalServices());
            model.addAttribute("fiscal", tax);
            model.addAttribute("works", works);
            model.addAttribute("userName", "Eduardo");

            return "dashboardTax";

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/fiscal/{taxId}/editar/servico")
    public String editWork(
        @RequestParam Long workId,
        @PathVariable Long taxId,
        @RequestParam(required = false) String description,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime
    ) {
        WorkRequestDTO dto = new WorkRequestDTO(
            taxId,
            description,
            startDateTime,
            endDateTime
        );
        workService.update(workId, dto);

        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PostMapping("/excluir/servico/{id}")
    public String deleteWork(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            workService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Feriado excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao excluir feriado: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/fiscal/" + id;
    }

    @PostMapping("/fiscal/{taxId}/timeoff")
    public String createTimeOff(@PathVariable Long taxId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) BigDecimal partialHours) {
        TimeOffUsageRequestDTO dto = new TimeOffUsageRequestDTO(taxId, startDate, endDate, partialHours);
        timeOffUsageService.create(dto);

        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PostMapping("/fiscal/{id}/work")
    public String addService(@PathVariable("id") Long taxId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime, @RequestParam String description, RedirectAttributes redirectAttributes) {
        try {
            WorkRequestDTO dto = new WorkRequestDTO(taxId, description, startDateTime, endDateTime);
            workService.create(dto);

            redirectAttributes.addFlashAttribute("successMessage",
                "Serviço cadastrado com sucesso!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao cadastrar serviço: " + e.getMessage());
        }

        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
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
