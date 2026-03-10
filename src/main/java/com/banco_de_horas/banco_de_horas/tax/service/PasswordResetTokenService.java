package com.banco_de_horas.banco_de_horas.tax.service;

import com.banco_de_horas.banco_de_horas.exceptions.CooldownActiveException;
import com.banco_de_horas.banco_de_horas.tax.entity.PasswordResetTokenEntity;
import com.banco_de_horas.banco_de_horas.tax.entity.TaxEntity;
import com.banco_de_horas.banco_de_horas.tax.repository.PasswordResetTokenRepository;
import com.banco_de_horas.banco_de_horas.tax.repository.TaxRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PasswordResetTokenService {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TaxRepository taxRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    // Rate limiting por IP
    private final Map<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------
    // PASSO 1 — Solicitar redefinição de senha
    // ---------------------------------------------------------------
    public void requestReset(String email, String ip) {

        if (!consumeRateLimit(ip)) {
            log.warn("Limite de tentativas atingido | IP: {}", ip);
            return;
        }

        Optional<TaxEntity> opt = taxRepository.findByEmail(email);

        if (opt.isEmpty()) {
            log.info("Redefinição solicitada para e-mail não cadastrado | IP: {}", ip);
            return;
        }

        TaxEntity tax = opt.get();

        passwordResetTokenRepository.findByTaxEntity(tax).ifPresent(existingToken -> {
            if (!existingToken.isUsed() &&
                existingToken.getGeneratedIn().isAfter(LocalDateTime.now().minusMinutes(5))) {
                log.info("Cooldown ativo para o usuário ID: {} | IP: {}", tax.getId(), ip);
                throw new CooldownActiveException();
            }
            passwordResetTokenRepository.delete(existingToken);
        });

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        PasswordResetTokenEntity newToken = PasswordResetTokenEntity.builder()
            .tokenHash(tokenHash)
            .expiryIn(LocalDateTime.now().plusHours(1))
            .generatedIn(LocalDateTime.now())
            .used(false)
            .taxEntity(tax)
            .build();

        passwordResetTokenRepository.save(newToken);

        sendEmailAsync(tax.getEmail(), tax.getFullName(), rawToken);

        log.info("Redefinição de senha solicitada | Usuário ID: {} | IP: {}", tax.getId(), ip);
    }

    // ---------------------------------------------------------------
    // PASSO 2 — Validar token (para decidir se exibe o formulário)
    // ---------------------------------------------------------------
    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return false;

        String tokenHash = DigestUtils.sha256Hex(rawToken);

        return passwordResetTokenRepository.findByTokenHash(tokenHash)
            .map(t -> !t.isUsed() &&
                t.getExpiryIn().isAfter(LocalDateTime.now()))
            .orElse(false);
    }

    public String validatePassword(String password, String confirmation) {
        if (!password.equals(confirmation))
            return "As senhas não coincidem.";
        if (password.length() < 7)
            return "A senha deve ter pelo menos 7 caracteres.";
        return null;
    }

    // ---------------------------------------------------------------
    // PASSO 3 — Redefinir a senha
    // ---------------------------------------------------------------
    public boolean resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) return false;

        String tokenHash = DigestUtils.sha256Hex(rawToken);

        Optional<PasswordResetTokenEntity> opt =
            passwordResetTokenRepository.findByTokenHash(tokenHash);

        if (opt.isEmpty()) {
            log.warn("Tentativa de redefinição com token inexistente");
            return false;
        }

        PasswordResetTokenEntity token = opt.get();

        if (token.isUsed()) {
            log.warn("Tentativa de reutilizar token já utilizado | Usuário ID: {}",
                token.getTaxEntity().getId());
            return false;
        }

        if (token.getExpiryIn().isBefore(LocalDateTime.now())) {
            log.warn("Tentativa de redefinição com token expirado | Usuário ID: {}",
                token.getTaxEntity().getId());
            return false;
        }

        TaxEntity tax = token.getTaxEntity();
        tax.setPassword(passwordEncoder.encode(newPassword));
        taxRepository.save(tax);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        log.info("Senha redefinida com sucesso | Usuário ID: {}", tax.getId());
        return true;
    }

    // ---------------------------------------------------------------
    // Envio de e-mail assíncrono
    // ---------------------------------------------------------------
    @Async
    public void sendEmailAsync(String email, String nome, String rawToken) {
        try {
            String link = baseUrl + "/dashboard/reset-senha?token=" + rawToken;

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("Redefinição de Senha — Banco de Horas");
            msg.setText(
                "Olá, " + nome + "!\n\n" +
                    "Recebemos uma solicitação para redefinir a senha da sua conta.\n\n" +
                    "Clique no link abaixo (válido por 1 hora):\n\n" +
                    link + "\n\n" +
                    "Se você não solicitou isso, ignore este e-mail.\n\n" +
                    "Prefeitura de Juiz de Fora — Banco de Horas"
            );
            mailSender.send(msg);
            log.info("E-mail de redefinição enviado para: {}", email);

        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de redefinição | Destinatário: {} | Erro: {}",
                email, e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Rate limiting — máximo 3 tentativas a cada 15 minutos por IP
    // ---------------------------------------------------------------
    private boolean consumeRateLimit(String ip) {
        Bucket bucket = bucketsByIp.computeIfAbsent(ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(3,
                    Refill.intervally(3, Duration.ofMinutes(15))))
                .build()
        );
        return bucket.tryConsume(1);
    }
}
