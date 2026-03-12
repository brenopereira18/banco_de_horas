package com.banco_de_horas.banco_de_horas.tax.service;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final TaxRepository taxRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String registration) throws UsernameNotFoundException {
        return taxRepository.findByRegistration(registration)
            .orElseThrow(() -> {
                log.warn("Tentativa de login com matrícula não encontrada: {}", registration);
                return new UsernameNotFoundException("Usuário não encontrado: " + registration);
            });
    }
}
