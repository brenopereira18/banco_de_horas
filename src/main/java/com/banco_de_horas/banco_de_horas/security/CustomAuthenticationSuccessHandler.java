package com.banco_de_horas.banco_de_horas.security;

import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        TaxEntity user = (TaxEntity) authentication.getPrincipal();

        String redirectUrl = switch (user.getUserType()) {
            case ADMINISTRADOR -> "/banco_de_horas/dashboard/administrador";
            case FISCAL, SUPERVISOR -> "/banco_de_horas/dashboard/fiscal/" + user.getId();
        };

        response.sendRedirect(redirectUrl);
    }
}
