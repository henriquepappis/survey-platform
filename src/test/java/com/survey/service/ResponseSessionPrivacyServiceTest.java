package com.survey.service;

import com.survey.config.PrivacyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseSessionPrivacyServiceTest {

    @Mock
    private PrivacyConfig privacyConfig;

    private ResponseSessionPrivacyService privacyService;

    @BeforeEach
    void setUp() {
        when(privacyConfig.isAudienceCollectionEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("Deve anonimizar IPv4 truncando último octeto quando habilitado")
    void anonymizeIpAddress_ipv4_shouldTruncateLastOctet() {
        privacyService = new ResponseSessionPrivacyService(true, privacyConfig, "");
        
        String result = privacyService.anonymizeIpAddress("192.168.1.100");
        
        assertThat(result).isEqualTo("192.168.1.0");
    }

    @Test
    @DisplayName("Deve anonimizar IPv6 truncando último bloco quando habilitado")
    void anonymizeIpAddress_ipv6_shouldTruncateLastBlock() {
        privacyService = new ResponseSessionPrivacyService(true, privacyConfig, "");
        
        String result = privacyService.anonymizeIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        
        assertThat(result).isEqualTo("2001:0db8:85a3:0000:0000:8a2e:0370:0000");
    }

    @Test
    @DisplayName("Não deve anonimizar IP quando desabilitado")
    void anonymizeIpAddress_disabled_shouldReturnOriginal() {
        privacyService = new ResponseSessionPrivacyService(false, privacyConfig, "");
        
        String result = privacyService.anonymizeIpAddress("192.168.1.100");
        
        assertThat(result).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Deve retornar 'unknown' para IP inválido")
    void anonymizeIpAddress_invalidIp_shouldReturnUnknown() {
        privacyService = new ResponseSessionPrivacyService(true, privacyConfig, "");
        
        String result = privacyService.anonymizeIpAddress(null);
        
        assertThat(result).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Deve normalizar valores nulos ou vazios")
    void normalize_nullOrBlank_shouldReturnUnknown() {
        privacyService = new ResponseSessionPrivacyService(true, privacyConfig, "");
        
        assertThat(privacyService.normalize(null)).isEqualTo("unknown");
        assertThat(privacyService.normalize("")).isEqualTo("unknown");
        assertThat(privacyService.normalize("   ")).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Deve normalizar user-agent truncando se muito longo")
    void normalizeUserAgent_tooLong_shouldTruncate() {
        privacyService = new ResponseSessionPrivacyService(true, privacyConfig, "");
        
        String longUserAgent = "A".repeat(600);
        String result = privacyService.normalizeUserAgent(longUserAgent, 500);
        
        assertThat(result).hasSize(500);
        assertThat(result).endsWith("...");
    }

    @Test
    @DisplayName("Deve hash IP quando solicitado")
    void hashIpAddress_validIp_shouldReturnHash() {
        privacyService = new ResponseSessionPrivacyService(true, privacyConfig, "test-salt");
        
        String result = privacyService.hashIpAddress("192.168.1.100");
        
        assertThat(result).isNotEqualTo("192.168.1.100");
        assertThat(result).startsWith("hashed:");
        assertThat(result.length()).isLessThanOrEqualTo(50); // Limite da coluna
    }

    @Test
    @DisplayName("Deve verificar se anonimização está habilitada")
    void isIpAnonymizationEnabled_shouldReturnCorrectValue() {
        privacyService = new ResponseSessionPrivacyService(true, privacyConfig, "");
        assertThat(privacyService.isIpAnonymizationEnabled()).isTrue();

        privacyService = new ResponseSessionPrivacyService(false, privacyConfig, "");
        assertThat(privacyService.isIpAnonymizationEnabled()).isFalse();
    }

    @Test
    @DisplayName("Deve verificar se coleta de audiência está habilitada")
    void isAudienceCollectionEnabled_shouldReturnCorrectValue() {
        when(privacyConfig.isAudienceCollectionEnabled()).thenReturn(true);
        privacyService = new ResponseSessionPrivacyService(true, privacyConfig, "");
        assertThat(privacyService.isAudienceCollectionEnabled()).isTrue();

        when(privacyConfig.isAudienceCollectionEnabled()).thenReturn(false);
        privacyService = new ResponseSessionPrivacyService(true, privacyConfig, "");
        assertThat(privacyService.isAudienceCollectionEnabled()).isFalse();
    }
}
