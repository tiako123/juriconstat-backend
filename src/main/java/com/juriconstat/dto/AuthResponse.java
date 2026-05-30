package com.juriconstat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de la réponse d'authentification (login réussi).
 * Retourné sur POST /auth/login.
 *
 * @author Borel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Le token JWT à utiliser dans le header Authorization: Bearer <token> */
    private String token;

    /** Rôle de l'utilisateur : USER ou ADMIN */
    private String role;

    /** Identifiant de l'utilisateur connecté */
    private Long userId;

    /** Nom de l'utilisateur (pour l'affichage) */
    private String nom;

    /** Email de l'utilisateur (pour le profil) */
    private String email;
}
