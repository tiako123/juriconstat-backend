package com.juriconstat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de la réponse d'une consultation juridique.
 * Retourné sur POST /consultations et GET /consultations/user/{userId}.
 *
 * @author Brad
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponse {

    /** Identifiant unique de la consultation. */
    private Long id;

    /** Texte original soumis par l'utilisateur. */
    private String requete;

    /**
     * Réponse juridique générée par Gemini IA.
     * Peut contenir des articles de loi, des recommandations, etc.
     */
    private String reponseIa;

    /** Pays de la consultation. */
    private String pays;

    /** Langue de la consultation. */
    private String langue;

    /** Date et heure de création de la consultation. */
    private LocalDateTime createdAt;

    /** ID de l'utilisateur auteur de la consultation. */
    private Long userId;
}
