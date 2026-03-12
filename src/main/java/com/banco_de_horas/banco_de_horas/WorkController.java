package com.banco_de_horas.banco_de_horas;

import com.banco_de_horas.banco_de_horas.work.dto.WorkRequestDTO;
import com.banco_de_horas.banco_de_horas.work.service.WorkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/banco_de_horas/dashboard/fiscal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class WorkController {

    private final WorkService workService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{id}/work/batch")
    public String createBatchWork(
        @PathVariable("id") Long taxId,
        @RequestParam("servicesJson") String servicesJson,
        RedirectAttributes redirectAttributes) {

        try {
            List<WorkRequestDTO> services = objectMapper.readValue(
                servicesJson,
                new TypeReference<List<WorkRequestDTO>>() {});

            workService.createBatch(taxId, services);
            redirectAttributes.addFlashAttribute("successMessage", "Serviços cadastrados com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao cadastrar serviços: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PostMapping("/{taxId}/editar/servico")
    public String editWork(
        @PathVariable Long taxId,
        @RequestParam Long workId,
        @RequestParam(required = false) String description,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime,
        RedirectAttributes redirectAttributes) {

        try {
            WorkRequestDTO dto = new WorkRequestDTO(taxId, description, startDateTime, endDateTime);
            workService.update(workId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Serviço atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao atualizar serviço: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }

    @PostMapping("/excluir/servico/{id}")
    public String deleteWork(
        @PathVariable Long id,
        @RequestParam Long taxId,
        RedirectAttributes redirectAttributes) {

        try {
            workService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Serviço excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erro ao excluir serviço: " + e.getMessage());
        }
        return "redirect:/banco_de_horas/dashboard/fiscal/" + taxId;
    }
}
