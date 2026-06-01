package com.juriconstat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juriconstat.dto.OcrResponse;
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

        // Liste des endpoints de modèles à tester (premier marche, sinon fallback)
        List<String> modelUrls = List.of(
                apiUrl + "?key=" + apiKey,
                apiUrl.replace("/gemini-2.5-flash:", "/gemini-2.5-flash-lite:") + "?key=" + apiKey
        );

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

        Exception lastException = null;
        for (String url : modelUrls) {
            try {
                log.info("Tentative d'appel Gemini avec l'URL: {}", url.replaceAll("key=.*", "key=HIDDEN"));
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class
                );
                return extraireTexteReponse(response.getBody());
            } catch (Exception e) {
                lastException = e;
                log.warn("Échec de l'appel Gemini pour l'URL {}: {}", url.replaceAll("key=.*", "key=HIDDEN"), e.getMessage());
            }
        }

        log.error("Tous les modèles Gemini ont échoué. Dernière erreur : {}", lastException.getMessage(), lastException);
        return "⚠️ Le service d'assistance juridique est temporairement indisponible. "
                + "Veuillez réessayer dans quelques instants ou contacter un avocat directement.";
    }

    /**
     * Analyse une image (carte grise ou attestation d'assurance) pour extraire
     * les informations structurées via OCR assisté par l'IA Gemini.
     *
     * @param mediaData     données de l'image en Base64
     * @param mediaMimeType type MIME (ex: image/jpeg)
     * @return OcrResponse contenant nom, immatriculation, assurance, police
     */
    public OcrResponse extraireOcrDepuisImage(String mediaData, String mediaMimeType) {
        String prompt = "Extrait de manière précise les informations suivantes du document fourni (qui est soit une carte grise, soit une attestation d'assurance) et retourne-les EXACTEMENT sous ce format JSON strict sans balises supplémentaires : { \"nom\": \"Prénom et Nom du propriétaire/assuré\", \"immatriculation\": \"Plaque d'immatriculation (formaté XX-000-XX ou 0000 XX 00)\", \"assurance\": \"Nom de la compagnie d'assurance (si présent)\", \"police\": \"Numéro de police d'assurance (si présent)\" }. Si une information n'est pas trouvée, mets null au lieu de la clé. N'invente rien.";

        List<String> modelUrls = List.of(
                apiUrl + "?key=" + apiKey,
                apiUrl.replace("/gemini-2.5-flash:", "/gemini-2.5-flash-lite:") + "?key=" + apiKey
        );

        String base64Cleaned = mediaData;
        if (mediaData.contains("base64,")) {
            base64Cleaned = mediaData.substring(mediaData.indexOf("base64,") + 7);
        }
        base64Cleaned = base64Cleaned.replaceAll("[\\r\\n]", "");

        List<Map<String, Object>> parts = List.of(
                Map.of("text", prompt),
                Map.of("inlineData", Map.of(
                        "mimeType", mediaMimeType,
                        "data", base64Cleaned
                ))
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", parts)
                ),
                "generationConfig", Map.of(
                        "temperature", 0.0, // Faible température pour éviter les hallucinations
                        "maxOutputTokens", 500
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        Exception lastException = null;
        for (String url : modelUrls) {
            try {
                log.info("Tentative OCR avec l'URL: {}", url.replaceAll("key=.*", "key=HIDDEN"));
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class
                );

                String jsonText = extraireTexteReponse(response.getBody());
                // Nettoyage au cas où Gemini renvoie du markdown comme ```json ... ```
                if (jsonText.startsWith("```json")) {
                    jsonText = jsonText.substring(7);
                }
                if (jsonText.endsWith("```")) {
                    jsonText = jsonText.substring(0, jsonText.length() - 3);
                }
                if (jsonText.startsWith("```")) {
                    jsonText = jsonText.substring(3);
                }
                jsonText = jsonText.trim();

                return objectMapper.readValue(jsonText, OcrResponse.class);

            } catch (Exception e) {
                lastException = e;
                log.warn("Échec de l'OCR pour l'URL {}: {}", url.replaceAll("key=.*", "key=HIDDEN"), e.getMessage());
            }
        }

        log.error("Tous les modèles OCR ont échoué. Dernière erreur : {}", lastException.getMessage(), lastException);
        // Retourner un objet vide en cas d'erreur
        return new OcrResponse("", "", "", "");
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
