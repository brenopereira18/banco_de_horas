package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.exceptions.CooldownActiveException;
import com.banco_de_horas.banco_de_horas.holiday.dto.HolidayRequestDTO;
import com.banco_de_horas.banco_de_horas.holiday.entity.HolidayEntity;
import com.banco_de_horas.banco_de_horas.holiday.service.HolidayService;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxResponseDTO;
import com.banco_de_horas.banco_de_horas.tax.dto.UpdateProfileRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.service.PasswordResetTokenService;
import com.banco_de_horas.banco_de_horas.tax.service.TaxService;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.MonthlyTimeOffUsageItemDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.dto.TimeOffUsageRequestDTO;
import com.banco_de_horas.banco_de_horas.timeOffUsage.service.TimeOffUsageService;
import com.banco_de_horas.banco_de_horas.utils.TimeFormatUtils;
import com.banco_de_horas.banco_de_horas.work.dto.MonthlyWorkItemDTO;
import com.banco_de_horas.banco_de_horas.work.dto.WorkRequestDTO;
import com.banco_de_horas.banco_de_horas.work.service.WorkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

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
    private PasswordResetTokenService passwordResetTokenService;

    @Autowired
    private TimeOffUsageService timeOffUsageService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/administrador")
    public String showDashboard(Model model,  @RequestParam(defaultValue = "fiscais") String tab, Authentication authentication) {
        List<TaxResponseDTO> fiscais = taxService.getAllTax();
        List<HolidayEntity> feriados = holidayService.listAll();
        TaxEntity user = (TaxEntity) authentication.getPrincipal();

        model.addAttribute("fiscais", fiscais);
        model.addAttribute("feriados", feriados);
        model.addAttribute("activeTab", tab);
        model.addAttribute("user", user);
        return "dashboardManager";
    }

    @GetMapping("/fiscal/{id}")
    public String showFiscalDetails(@PathVariable Long id, Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "0") int timeOffPage) {
        try {
            TaxEntity tax = taxService.findById(id);
            BigDecimal generatedHours = workService.getNumberOfHoursGenerated(tax);

            Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

            boolean isAdmin = auth != null &&
                auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
            model.addAttribute("isAdmin", isAdmin);

            // Paginação de serviços
            Page<MonthlyWorkItemDTO> worksPage = workService.getAllWorks(tax, page, 10);

            // Paginação de folgas
            Page<MonthlyTimeOffUsageItemDTO> timeOffUsage = timeOffUsageService.getAllTimeUsage(tax, timeOffPage, 10);

            BigDecimal hoursUsed = timeOffUsageService.getHoursUsed(tax);

            // Atributos de serviços
            model.addAttribute("works", worksPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", worksPage.getTotalPages());
            model.addAttribute("hasNext", worksPage.hasNext());
            model.addAttribute("hasPrevious", worksPage.hasPrevious());

            // Atributos de folgas
            model.addAttribute("daysOffPerMonth", timeOffUsage.getContent());
            model.addAttribute("timeOffCurrentPage", timeOffPage);
            model.addAttribute("timeOffTotalPages", timeOffUsage.getTotalPages());
            model.addAttribute("timeOffHasNext", timeOffUsage.hasNext());
            model.addAttribute("timeOffHasPrevious", timeOffUsage.hasPrevious());

            // Outros atributos
            model.addAttribute("formattedBalance", TimeFormatUtils.formatHours(tax.getBalanceOfHours()));
            model.addAttribute("hoursUsed", TimeFormatUtils.formatHours(hoursUsed));
            model.addAttribute("generatedHours", TimeFormatUtils.formatHours(generatedHours));
            model.addAttribute("fiscal", tax);
            model.addAttribute("userName", "Eduardo");

            return "dashboardTax";

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/esqueci-senha")
    public String showForgotPasswordForm() {
        return "forget-password";
    }

    @PostMapping("/esqueci-senha")
    public String processForgotPassword(
        @RequestParam String email,
        HttpServletRequest request,
        RedirectAttributes redirectAttrs) {
        try {
            passwordResetTokenService.requestReset(email, getRealIp(request));
        } catch (CooldownActiveException e) {
            redirectAttrs.addFlashAttribute("erro",
                "Você já solicitou um link recentemente. Aguarde 5 minutos.");
            return "redirect:/banco_de_horas/dashboard/esqueci-senha";
        }

        redirectAttrs.addFlashAttribute("sucesso",
            "Se este e-mail estiver cadastrado, você receberá as instruções em breve.");
        return "redirect:/banco_de_horas/dashboard/esqueci-senha";
    }

    // PASSO 3 — Exibir formulário de redefinição (link do e-mail)
    @GetMapping("/reset-senha")
    public String showResetPasswordForm(
        @RequestParam(required = false) String token,
        Model model) {

        if (!passwordResetTokenService.isTokenValid(token)) {
            model.addAttribute("tokenInvalido", true);
            return "reset-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    // ---------------------------------------------------------------
    // PASSO 4 — Salvar nova senha
    // ---------------------------------------------------------------
    @PostMapping("/reset-senha")
    public String processResetPassword(
        @RequestParam String token,
        @RequestParam String newPassword,
        @RequestParam String passwordConfirmation,
        RedirectAttributes redirectAttrs) {

        String validationError = passwordResetTokenService.validatePassword(newPassword, passwordConfirmation);
        if (validationError != null) {
            redirectAttrs.addFlashAttribute("erro", validationError);
            return "redirect:/banco_de_horas/dashboard/reset-senha?token=" + token;
        }

        boolean success = passwordResetTokenService.resetPassword(token, newPassword);

        if (success) {
            redirectAttrs.addFlashAttribute("sucesso",
                "Senha redefinida com sucesso! Faça login com sua nova senha.");
            return "redirect:/banco_de_horas/dashboard/login";
        }

        redirectAttrs.addFlashAttribute("erro",
            "Link inválido ou expirado. Solicite um novo link.");
        return "redirect:/banco_de_horas/dashboard/esqueci-senha";
    }

    // Respeita proxy reverso (nginx, etc.) — responsabilidade HTTP
    private String getRealIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/alterar-senha")
    public String changePassword(
        @ModelAttribute UpdateProfileRequestDTO dto,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {

        try {
            TaxEntity user = (TaxEntity) authentication.getPrincipal();

            taxService.updateProfile(
                user.getId(),
                dto
            );

            redirectAttributes.addFlashAttribute("successMessage",
                "Senha alterada com sucesso!");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        TaxEntity user = (TaxEntity) authentication.getPrincipal();
        return "redirect:/banco_de_horas/dashboard/fiscal/" + user.getId();
    }

    @PostMapping("/administrador/fiscal/{id}/add-hours")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String addHoursToFiscal(
        @PathVariable Long id,
        @RequestParam BigDecimal hours
    ) {
        taxService.addHours(id, hours);
        return "redirect:/banco_de_horas/dashboard/administrador";
    }

    @PostMapping("/administrador/fiscal/{id}/revert-hours")
    public String revertHours(@PathVariable Long id) {
        taxService.revertLastAddedHours(id);
        return "redirect:/banco_de_horas/dashboard/administrador";
    }


    @PreAuthorize("hasRole('ADMINISTRADOR')")
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

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/excluir/servico/{id}")
    public String deleteWork(@PathVariable Long id, RedirectAttributes redirectAttributes, @RequestParam Long taxId) {
        try {
            workService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Feriado excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao excluir feriado: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/fiscal/{taxId}/timeoff")
    public String createTimeOff(@PathVariable Long taxId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate solicitationDate, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required = false) BigDecimal partialHours) {
        TimeOffUsageRequestDTO dto = new TimeOffUsageRequestDTO(taxId, solicitationDate, startDate, endDate, partialHours);
        timeOffUsageService.create(dto);

        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/fiscal/{taxId}/editar/folga")
    public String editTimeOff(
        @PathVariable Long taxId,
        @RequestParam Long timeOffId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate solicitationDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) BigDecimal partialHours) {

        TimeOffUsageRequestDTO dto = new TimeOffUsageRequestDTO(taxId, solicitationDate, startDate, endDate, partialHours);
        timeOffUsageService.update(timeOffId, dto);

        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/excluir/folga/{id}")
    public String deleteTimeOff(@PathVariable Long id, @RequestParam Long taxId, RedirectAttributes redirectAttributes) {
        try {
            timeOffUsageService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Feriado excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao excluir feriado: " + e.getMessage());
        }

        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
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

    @PostMapping("/fiscal/{id}/work/batch")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String createBatchWork(
        @PathVariable("id") Long taxId,
        @RequestParam("servicesJson") String servicesJson
    ) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        List<WorkRequestDTO> services =
            mapper.readValue(
                servicesJson,
                new TypeReference<List<WorkRequestDTO>>() {}
            );

        workService.createBatch(taxId, services);
        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/administrador/criar/fiscal")
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

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/administrador/criar/feriado")
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

    @PreAuthorize("hasRole('ADMINISTRADOR')")
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

    @PreAuthorize("hasRole('ADMINISTRADOR')")
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
