package com.banco_de_horas.banco_de_horas.tax.service;

import com.banco_de_horas.banco_de_horas.exceptions.EntityAlreadyExists;
import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxManagementRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxResponseDTO;
import com.banco_de_horas.banco_de_horas.tax.dto.UpdateProfileRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.repository.PasswordResetTokenRepository;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public TaxEntity create(TaxRequestDTO dto) {
        if (taxRepository.existsByRegistration(dto.registration())) {
            throw new EntityAlreadyExists("Já existe fiscal com essa matrícula");
        }

        TaxEntity tax = TaxEntity.builder()
            .fullName(dto.fullName())
            .registration(dto.registration())
            .email(dto.email())
            .password(passwordEncoder.encode(dto.registration()))
            .userType(dto.userType())
            .balanceOfHours(BigDecimal.ZERO)
            .build();

        return taxRepository.save(tax);
    }

    @Transactional(readOnly = true)
    public List<TaxResponseDTO> getAllTax() {
        return this.taxRepository.findAllByOrderByFullNameAsc()
            .stream()
            .map(tax -> {
                return new TaxResponseDTO(
                    tax.getId(),
                    tax.getFullName(),
                    TimeFormatUtils.formatHours(tax.getBalanceOfHours()),
                    tax.getLastAddedHours()
                );
            }).toList();
    }

    public String validateUpdateProfile(Long id, UpdateProfileRequestDTO dto) {
        TaxEntity tax = taxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (dto.email() != null && !dto.email().isBlank()) {
            if (taxRepository.findByEmail(dto.email()).isPresent())
                return "Este e-mail já está em uso.";
        }

        if (dto.password() != null && !dto.password().isBlank()) {
            if (!dto.password().equals(dto.passwordConfirmation()))
                return "As senhas não coincidem.";
            if (dto.password().length() < 7)
                return "A senha deve ter pelo menos 7 caracteres.";
            if (passwordEncoder.matches(dto.password(), tax.getPassword()))
                return "A nova senha deve ser diferente da senha atual.";
        }

        return null;
    }

    public void updateProfile(Long id, UpdateProfileRequestDTO dto) {
        TaxEntity tax = taxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (dto.email() != null && !dto.email().isBlank())
            tax.setEmail(dto.email());

        if (dto.password() != null && !dto.password().isBlank())
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

    public void delete(String registration) {
        TaxEntity existing = taxRepository.findByRegistration(registration)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        passwordResetTokenRepository.findByTaxEntity(existing)
            .ifPresent(passwordResetTokenRepository::delete);

        taxRepository.delete(existing);
    }

    public void addHours(Long taxId, BigDecimal hours) {
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Horas inválidas");
        }

        TaxEntity tax = taxRepository.findById(taxId)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        tax.addHours(hours);
        tax.setLastAddedHours(hours);
        taxRepository.save(tax);
    }

    @Transactional
    public void revertLastAddedHours(Long taxId) {

        TaxEntity tax = taxRepository.findById(taxId)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        BigDecimal lastAdded = tax.getLastAddedHours();

        if (lastAdded == null || lastAdded.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Não há horas para reverter");
        }
        tax.subtractHours(lastAdded);
        tax.setLastAddedHours(null);

        taxRepository.save(tax);
    }

    public TaxEntity findById(Long id) {
        TaxEntity existingTax = taxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        return existingTax;
    }
}
