package com.juriconstat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration des beans techniques partagés.
 * - RestTemplate pour les appels HTTP vers l'API Gemini.
 * - ObjectMapper pour la sérialisation/désérialisation JSON.
 *
 * @author Borel / Brad
 */
@Configuration
public class AppConfig {

    /**
     * Client HTTP générique utilisé par GeminiService.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
