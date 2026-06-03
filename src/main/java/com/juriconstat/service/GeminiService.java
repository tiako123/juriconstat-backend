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
    public String genererReponseJuridique(String requete, String pays, String langue, String mediaData, String mediaMimeType, boolean isEmergency) {
        String prompt = construirePrompt(requete, pays, langue, isEmergency);

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

        List<Map<String, String>> safetySettings = List.of(
                Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE")
        );

        // Corps de la requête Gemini
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", parts)
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 8192
                ),
                "safetySettings", safetySettings
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
                
                String rawJson = response.getBody();
                log.info("RAW GEMINI RESPONSE: {}", rawJson);
                
                return extraireTexteReponse(rawJson);
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

        List<Map<String, String>> safetySettings = List.of(
                Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE")
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", parts)
                ),
                "generationConfig", Map.of(
                        "temperature", 0.0, // Faible température pour éviter les hallucinations
                        "maxOutputTokens", 500
                ),
                "safetySettings", safetySettings
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
    private String construirePrompt(String requete, String pays, String langue, boolean isEmergency) {
        if (isEmergency) {
            return String.format("""
                    Tu es JuriConstat, un assistant juridique pratique et direct.
                    Pays de référence : %s. Langue de réponse : %s.
                    
                    RÈGLES STRICTES POUR SITUATION D'URGENCE :
                    1. Commence TOUJOURS par les ACTIONS CONCRÈTES à faire immédiatement (numérotées).
                    2. Sois DIRECT et PRATIQUE : dis quoi faire, pas la théorie juridique.
                    3. Dans la base légale, sois EXTRÊMEMENT PRÉCIS : cite l'Article exact, l'alinéa, le nom complet du Code (ex: Code civil, Code pénal), le numéro de la Loi ou l'article de la Constitution. Ne dis jamais "selon la loi" de façon vague.
                    4. Maximum 250 mots. Pas de blabla. Pas de répétitions.
                    5. Utilise le format Markdown : **gras** pour les actions importantes, des listes numérotées.
                    6. Termine par un conseil pratique court (emoji 💡).
                    
                    FORMAT OBLIGATOIRE :
                    ## ⚡ Actions immédiates
                    1. [Action concrète]
                    2. [Action concrète]
                    ...
                    
                    ## 📋 Base légale
                    - **[Nom du Code / Numéro de Loi]**, Article [Numéro], Alinéa [Numéro] : [Brève explication de 2-3 lignes max]
                    
                    💡 **Conseil** : [conseil pratique court]
                    
                    Question de l'utilisateur :
                    %s
                    """,
                    pays, langue, requete
            );
        } else {
            return String.format("""
                    Tu es JuriConstat, un expert juridique pédagogique et structuré.
                    Pays de référence : %s. Langue de réponse : %s.
                    
                    RÈGLES STRICTES POUR L'EXPLICATION JURIDIQUE :
                    1. Réponds de manière claire, structurée et pédagogique.
                    2. Sois EXTRÊMEMENT PRÉCIS dans tes sources : cite toujours l'Article exact, l'alinéa, le nom complet du Code (ex: Code civil, Code pénal, Code du travail), le numéro de la Loi ou l'article de la Constitution applicables au %s. Ne dis jamais "selon la loi" de façon vague.
                    3. Rédige TOUJOURS ta réponse en suivant le format de section obligatoire ci-dessous, même s'il ne s'agit pas d'une urgence.
                    4. Maximum 300 mots. Utilise le format Markdown.
                    
                    FORMAT OBLIGATOIRE :
                    ## ⚡ Actions immédiates
                    1. [Action concrète à faire]
                    2. [Action concrète à faire]
                    ...
                    
                    ## 📋 Loi concernée
                    - **[Nom du Code / Numéro de Loi]**, Article [Numéro], Alinéa [Numéro] : [Explication claire et précise]
                    
                    💡 **Conseil** : [conseil ou note récapitulative courte]
                    
                    Question de l'utilisateur :
                    %s
                    """,
                    pays, langue, pays, requete
            );
        }
    }

    /**
     * Extrait le texte de la réponse JSON retournée par l'API Gemini.
     *
     * Structure JSON attendue :
     * { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
     */
    private String extraireTexteReponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode partsNode = root.path("candidates").get(0).path("content").path("parts");
        
        StringBuilder sb = new StringBuilder();
        if (partsNode.isArray()) {
            for (JsonNode part : partsNode) {
                if (part.has("text")) {
                    sb.append(part.path("text").asText());
                }
            }
        }
        return sb.toString();
    }
}
