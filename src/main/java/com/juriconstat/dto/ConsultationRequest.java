package com.juriconstat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de la requête de consultation juridique.
 * Envoyé par le frontend via POST /consultations.
 *
 * Si pays et langue ne sont pas fournis, ceux du profil utilisateur sont utilisés.
 *
 * @author Brad
 */
@Data
public class ConsultationRequest {

    /**
     * Texte de la requête juridique de l'utilisateur.
     * Exemple : "J'ai eu un accident de voiture à Douala, que faire ?"
     */
    @NotBlank(message = "La requête ne peut pas être vide")
    @Size(min = 5, max = 5000, message = "La requête doit faire entre 5 et 5000 caractères")
    private String requete;

    /**
     * Pays (optionnel) — surcharge le pays du profil utilisateur.
     * Exemples : "Cameroun", "Sénégal", "France"
     */
    private String pays;

    /**
     * Langue (optionnel) — surcharge la langue du profil utilisateur.
     * Exemples : "fr", "en"
     */
    private String langue;

    /**
     * Données du média encodées en Base64 (optionnel).
     */
    private String mediaData;

    /**
     * Type MIME du média (ex: image/jpeg, audio/mp3, video/mp4) (optionnel).
     */
    private String mediaMimeType;
}
