package com.juriconstat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service d'intégration avec l'API Google Gemini.
 *
 * Responsabilités :
 * - Construire un prompt juridique contextualisé (pays + langue).
 * - Appeler l'API Gemini (gemini-1.5-flash).
 * - Extraire et retourner le texte de la réponse générée.
 *
 * @author Brad
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ─── Méthode principale ────────────────────────────────────────────────────

    /**
     * Génère une réponse juridique à partir d'une requête textuelle et d'un média optionnel.
     *
     * @param requete       texte de la question juridique de l'utilisateur
     * @param pays          pays de référence pour le droit applicable
     * @param langue        langue de la réponse ("fr", "en", etc.)
     * @param mediaData     données du média encodées en Base64 (optionnel)
     * @param mediaMimeType type MIME du média (optionnel)
     * @return réponse juridique générée par Gemini IA
     */
    public String genererReponseJuridique(String requete, String pays, String langue, String mediaData, String mediaMimeType) {
        String prompt = construirePrompt(requete, pays, langue);
        String urlComplete = apiUrl + "?key=" + apiKey;

        // Construction de la liste des parties (parts) pour la requête
        List<Map<String, Object>> parts;
        if (mediaData != null && !mediaData.trim().isEmpty() && mediaMimeType != null && !mediaMimeType.trim().isEmpty()) {
            // Nettoyage de la base64 au cas où elle contient un préfixe (ex: "data:image/jpeg;base64,")
            String base64Cleaned = mediaData;
            if (mediaData.contains("base64,")) {
                base64Cleaned = mediaData.substring(mediaData.indexOf("base64,") + 7);
            }
            // Enlever les retours à la ligne indésirables
            base64Cleaned = base64Cleaned.replaceAll("[\\r\\n]", "");

            parts = List.of(
                    Map.of("text", prompt),
                    Map.of("inlineData", Map.of(
                            "mimeType", mediaMimeType,
                            "data", base64Cleaned
                    ))
            );
            log.info("Appel multimodal Gemini avec fichier type: {}", mediaMimeType);
        } else {
            parts = List.of(
                    Map.of("text", prompt)
            );
            log.info("Appel textuel pur Gemini");
        }

        // Corps de la requête Gemini
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", parts)
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 2048
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    urlComplete, HttpMethod.POST, entity, String.class
            );

            return extraireTexteReponse(response.getBody());

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à l'API Gemini : {}", e.getMessage(), e);
            return "⚠️ Le service d'assistance juridique est temporairement indisponible. "
                    + "Veuillez réessayer dans quelques instants ou contacter un avocat directement.";
        }
    }

    // ─── Helpers privés ────────────────────────────────────────────────────────

    /**
     * Construit un prompt juridique contextualisé pour Gemini.
     * Le prompt guide l'IA pour qu'elle agisse en tant qu'assistant juridique
     * spécialisé dans le pays et la langue spécifiés.
     */
    private String construirePrompt(String requete, String pays, String langue) {
        return String.format("""
                Tu es JuriConstat, un assistant juridique expert et professionnel.
                Tu spécialises en droit de %s.
                Tu réponds TOUJOURS en %s.
                
                Règles importantes :
                - Fournis des informations juridiques précises et pratiques.
                - Cite les articles de loi ou textes réglementaires applicables au %s si possible.
                - Recommande de consulter un avocat pour les cas complexes.
                - Reste neutre, objectif et professionnel.
                - Structure ta réponse clairement (situation légale, droits, actions recommandées).
                - Ne dépasse pas 500 mots.
                
                Question de l'utilisateur :
                %s
                """,
                pays, langue, pays, requete
        );
    }

    /**
     * Extrait le texte de la réponse JSON retournée par l'API Gemini.
     *
     * Structure JSON attendue :
     * { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
     */
    private String extraireTexteReponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        return root
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();
    }
}
