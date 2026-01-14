package com.survey.service;

import com.survey.config.PrivacyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Serviço responsável por aplicar políticas de privacidade (LGPD) aos dados de sessão de resposta.
 * 
 * Funcionalidades:
 * - Anonimização de endereços IP (truncamento ou hash irreversível)
 * - Normalização de dados sensíveis
 * - Respeita configurações de privacidade da aplicação
 */
@Service
public class ResponseSessionPrivacyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSessionPrivacyService.class);
    private static final String UNKNOWN_VALUE = "unknown";
    private static final String IP_HASH_SALT_PREFIX = "survey-privacy-";

    private final boolean anonymizeIp;
    private final boolean audienceEnabled;
    private final String ipHashSalt;

    public ResponseSessionPrivacyService(
            @Value("${app.privacy.ip-anonymize:true}") boolean anonymizeIp,
            PrivacyConfig privacyConfig,
            @Value("${app.privacy.ip-hash-salt:}") String ipHashSalt) {
        this.anonymizeIp = anonymizeIp;
        this.audienceEnabled = privacyConfig.isAudienceCollectionEnabled();
        // Se não fornecido, usa um salt padrão baseado no nome da aplicação
        this.ipHashSalt = ipHashSalt != null && !ipHashSalt.isBlank() 
                ? ipHashSalt 
                : IP_HASH_SALT_PREFIX + "default-salt";
    }

    /**
     * Anonimiza um endereço IP de acordo com a política configurada.
     * 
     * Estratégias suportadas:
     * - Truncamento: Remove último octeto (IPv4) ou último bloco (IPv6) - padrão
     * - Hash: Aplica SHA-256 com salt para anonimização irreversível (se configurado)
     * 
     * @param ipAddress Endereço IP original (IPv4 ou IPv6)
     * @return IP anonimizado ou "unknown" se inválido
     */
    public String anonymizeIpAddress(String ipAddress) {
        if (!anonymizeIp) {
            return normalize(ipAddress);
        }

        String normalized = normalize(ipAddress);
        if (UNKNOWN_VALUE.equals(normalized)) {
            return normalized;
        }

        // Estratégia padrão: truncamento (menos agressivo, mantém geolocalização aproximada)
        return truncateIp(normalized);
    }

    /**
     * Anonimiza IP usando hash irreversível (SHA-256 com salt).
     * Útil quando se deseja anonimização completa sem possibilidade de reversão.
     * 
     * @param ipAddress Endereço IP original
     * @return Hash do IP (primeiros 16 caracteres hex) ou "unknown" se inválido
     */
    public String hashIpAddress(String ipAddress) {
        String normalized = normalize(ipAddress);
        if (UNKNOWN_VALUE.equals(normalized)) {
            return normalized;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedInput = ipHashSalt + normalized;
            byte[] hashBytes = digest.digest(saltedInput.getBytes(StandardCharsets.UTF_8));
            
            // Retorna primeiros 16 caracteres hex (64 bits) para manter compatibilidade com coluna de 50 chars
            String hashHex = Base64.getEncoder().encodeToString(hashBytes).substring(0, Math.min(16, Base64.getEncoder().encodeToString(hashBytes).length()));
            return "hashed:" + hashHex;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("Falha ao aplicar hash no IP, usando truncamento: {}", e.getMessage());
            return truncateIp(normalized);
        }
    }

    /**
     * Trunca o último octeto (IPv4) ou último bloco (IPv6) do endereço IP.
     * Mantém informações de geolocalização aproximada (país/região) mas remove identificação individual.
     * 
     * @param ip Endereço IP normalizado
     * @return IP truncado
     */
    private String truncateIp(String ip) {
        if (ip.contains(".")) {
            // IPv4: remove último octeto (192.168.1.100 -> 192.168.1.0)
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                parts[3] = "0";
                return String.join(".", parts);
            }
        }
        if (ip.contains(":")) {
            // IPv6: remove último bloco (2001:0db8:85a3:0000:0000:8a2e:0370:7334 -> 2001:0db8:85a3:0000:0000:8a2e:0370:0000)
            String[] parts = ip.split(":");
            if (parts.length > 0) {
                parts[parts.length - 1] = "0000";
                return String.join(":", parts);
            }
        }
        return UNKNOWN_VALUE;
    }

    /**
     * Normaliza um valor de string, removendo espaços e tratando valores nulos/vazios.
     * 
     * @param value Valor a normalizar
     * @return Valor normalizado ou "unknown" se inválido
     */
    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_VALUE;
        }
        return value.trim();
    }

    /**
     * Normaliza e limita o tamanho de um user-agent para evitar armazenamento excessivo.
     * 
     * @param userAgent User-agent original
     * @param maxLength Tamanho máximo permitido (padrão: 500)
     * @return User-agent normalizado e truncado se necessário
     */
    public String normalizeUserAgent(String userAgent, int maxLength) {
        String normalized = normalize(userAgent);
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength - 3) + "...";
        }
        return normalized;
    }

    /**
     * Verifica se a coleta de dados de audiência está habilitada.
     * 
     * @return true se a coleta está habilitada
     */
    public boolean isAudienceCollectionEnabled() {
        return audienceEnabled;
    }

    /**
     * Verifica se a anonimização de IP está habilitada.
     * 
     * @return true se a anonimização está habilitada
     */
    public boolean isIpAnonymizationEnabled() {
        return anonymizeIp;
    }
}
