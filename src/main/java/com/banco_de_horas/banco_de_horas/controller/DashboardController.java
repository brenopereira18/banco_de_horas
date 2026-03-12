package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.holiday.service.HolidayService;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.service.TaxService;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.MonthlyTimeOffUsageItemDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.service.TimeOffUsageService;
import com.banco_de_horas.banco_de_horas.utils.TimeFormatUtils;
import com.banco_de_horas.banco_de_horas.work.dto.MonthlyWorkItemDTO;
import com.banco_de_horas.banco_de_horas.work.service.WorkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/banco_de_horas/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TaxService taxService;
    private final HolidayService holidayService;
    private final WorkService workService;
    private final TimeOffUsageService timeOffUsageService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/administrador")
    public String showDashboard(
        Model model,
        @RequestParam(defaultValue = "fiscais") String tab,
        Authentication authentication) {

        TaxEntity user = (TaxEntity) authentication.getPrincipal();

        model.addAttribute("fiscais", taxService.getAllTax());
        model.addAttribute("feriados", holidayService.listAll());
        model.addAttribute("activeTab", tab);
        model.addAttribute("user", user);
        return "dashboardManager";
    }

    @GetMapping("/fiscal/{id}")
    public String showFiscalDetails(
        @PathVariable Long id,
        Model model,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "0") int timeOffPage,
        Authentication authentication) {

        TaxEntity tax = taxService.findById(id);

        boolean isAdmin = authentication != null &&
            authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));

        Page<MonthlyWorkItemDTO> worksPage = workService.getAllWorks(tax, page, 10);
        Page<MonthlyTimeOffUsageItemDTO> timeOffUsagePage = timeOffUsageService.getAllTimeUsage(tax, timeOffPage, 10);

        // Atributos de serviços
        model.addAttribute("works", worksPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", worksPage.getTotalPages());
        model.addAttribute("hasNext", worksPage.hasNext());
        model.addAttribute("hasPrevious", worksPage.hasPrevious());

        // Atributos de folgas
        model.addAttribute("daysOffPerMonth", timeOffUsagePage.getContent());
        model.addAttribute("timeOffCurrentPage", timeOffPage);
        model.addAttribute("timeOffTotalPages", timeOffUsagePage.getTotalPages());
        model.addAttribute("timeOffHasNext", timeOffUsagePage.hasNext());
        model.addAttribute("timeOffHasPrevious", timeOffUsagePage.hasPrevious());

        // Outros atributos
        model.addAttribute("fiscal", tax);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("loggedRegistration", authentication != null ? authentication.getName() : "");
        model.addAttribute("formattedBalance", TimeFormatUtils.formatHours(tax.getBalanceOfHours()));
        model.addAttribute("generatedHours", TimeFormatUtils.formatHours(workService.getNumberOfHoursGenerated(tax)));
        model.addAttribute("hoursUsed", TimeFormatUtils.formatHours(timeOffUsageService.getHoursUsed(tax)));

        return "dashboardTax";
    }
}
