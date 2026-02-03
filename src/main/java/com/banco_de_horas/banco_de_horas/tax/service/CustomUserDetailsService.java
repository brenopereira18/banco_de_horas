package com.banco_de_horas.banco_de_horas.tax.service;

import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private TaxRepository taxRepository;

    @Override
    public UserDetails loadUserByUsername(String registration) throws ResourceNotFoundException {
        return taxRepository.findByRegistration(registration)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + registration));
    }
}
