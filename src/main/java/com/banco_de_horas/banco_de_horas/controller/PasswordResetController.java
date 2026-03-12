package com.banco_de_horas.banco_de_horas.controller;

import com.banco_de_horas.banco_de_horas.exceptions.CooldownActiveException;
import com.banco_de_horas.banco_de_horas.tax.service.PasswordResetTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/banco_de_horas/dashboard")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetTokenService passwordResetTokenService;

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
            redirectAttrs.addFlashAttribute("errorMessage",
                "Você já solicitou um link recentemente. Aguarde 5 minutos.");
            return "redirect:/banco_de_horas/dashboard/esqueci-senha";
        }

        redirectAttrs.addFlashAttribute("successMessage",
            "Se este e-mail estiver cadastrado, você receberá as instruções em breve.");
        return "redirect:/banco_de_horas/dashboard/esqueci-senha";
    }

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

    @PostMapping("/reset-senha")
    public String processResetPassword(
        @RequestParam String token,
        @RequestParam String newPassword,
        @RequestParam String passwordConfirmation,
        RedirectAttributes redirectAttrs) {

        String validationError = passwordResetTokenService.validatePassword(newPassword, passwordConfirmation);
        if (validationError != null) {
            redirectAttrs.addFlashAttribute("errorMessage", validationError);
            return "redirect:/banco_de_horas/dashboard/reset-senha?token=" + token;
        }

        if (passwordResetTokenService.resetPassword(token, newPassword)) {
            redirectAttrs.addFlashAttribute("successMessage",
                "Senha redefinida com sucesso! Faça login com sua nova senha.");
            return "redirect:/banco_de_horas/dashboard/login";
        }

        redirectAttrs.addFlashAttribute("errorMessage",
            "Link inválido ou expirado. Solicite um novo link.");
        return "redirect:/banco_de_horas/dashboard/esqueci-senha";
    }

    // Respeita proxy reverso (nginx, etc.)
    private String getRealIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
