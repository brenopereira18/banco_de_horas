package com.banco_de_horas.banco_de_horas.tax.service;

import com.banco_de_horas.banco_de_horas.exceptions.EntityAlreadyExists;
import com.banco_de_horas.banco_de_horas.exceptions.ResourceNotFoundException;
import com.banco_de_horas.banco_de_horas.tax.dto.TaxRequestDTO;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
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

    public TaxEntity create(TaxRequestDTO dto) {
        if (taxRepository.existsByRegistration(dto.registration())) {
            throw new EntityAlreadyExists("Já existe fiscal com essa matrícula");
        }

        TaxEntity tax = TaxEntity.builder()
            .fullName(dto.fullName())
            .registration(dto.registration())
            .userType(dto.userType())
            .balanceOfHours(BigDecimal.ZERO)
            .build();

        return taxRepository.save(tax);
    }

    @Transactional(readOnly = true)
    public List<TaxEntity> getAllTax() {
        return this.taxRepository.findAll();
    }

    public TaxEntity update(Long id, TaxRequestDTO dto) {
        TaxEntity existing = taxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        // Se trocar matrícula, valida duplicidade
        if (!existing.getRegistration().equals(dto.registration())
            && taxRepository.existsByRegistration(dto.registration())) {
            throw new IllegalStateException("Já existe fiscal com essa matrícula");
        }

        existing.setFullName(dto.fullName());
        existing.setRegistration(dto.registration());
        existing.setUserType(dto.userType());

        return taxRepository.save(existing);
    }

    public void delete(Long id) {
        TaxEntity existing = taxRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fiscal não encontrado"));

        taxRepository.delete(existing);
    }
}
