package com.banco_de_horas.banco_de_horas.tax.service;

import com.banco_de_horas.banco_de_horas.exceptions.EntityAlreadyExists;
import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxManagementRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxResponseDTO;
import com.banco_de_horas.banco_de_horas.tax.dto.UpdateProfileRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import com.banco_de_horas.banco_de_horas.utils.TimeFormatUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class TaxService {

    @Autowired
    private TaxRepository taxRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public TaxEntity create(TaxRequestDTO dto) {
        if (taxRepository.existsByRegistration(dto.registration())) {
            throw new EntityAlreadyExists("Já existe fiscal com essa matrícula");
        }

        TaxEntity tax = TaxEntity.builder()
            .fullName(dto.fullName())
            .registration(dto.registration())
            .password(passwordEncoder.encode(dto.registration()))
            .userType(dto.userType())
            .balanceOfHours(BigDecimal.ZERO)
            .build();

        return taxRepository.save(tax);
    }

    @Transactional(readOnly = true)
    public List<TaxResponseDTO> getAllTax() {
        return this.taxRepository.findAll()
            .stream()
            .map(tax -> {
                return new TaxResponseDTO(
                    tax.getId(),
                    tax.getFullName(),
                    TimeFormatUtils.formatHours(tax.getBalanceOfHours())
                );
            }).toList();
    }

    public void updateProfile(Long id, UpdateProfileRequestDTO dto) {
        TaxEntity tax = taxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Validar se nova senha é diferente da atual
        if (passwordEncoder.matches(dto.password(), tax.getPassword())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual");
        }

        // Validar força da senha (mínimo 6 caracteres, por exemplo)
        if (dto.password().length() < 6) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 6 caracteres");
        }

        tax.setPassword(passwordEncoder.encode(dto.password()));
        taxRepository.save(tax);
    }

    public TaxEntity update(Long id, TaxManagementRequestDTO dto) {
        TaxEntity existing = taxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        existing.setFullName(dto.fullName());
        existing.setUserType(dto.userType());

        return taxRepository.save(existing);
    }

    public void delete(Long id) {
        TaxEntity existing = taxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        taxRepository.delete(existing);
    }

    public void addHours(Long taxId, BigDecimal hours) {
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Horas inválidas");
        }

        TaxEntity tax = taxRepository.findById(taxId)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        tax.addHours(hours);
        taxRepository.save(tax);
    }


    public TaxEntity findById(Long id) {
        TaxEntity existingTax = taxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        return existingTax;
    }
}
