package com.juriconstat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO du profil utilisateur.
 * Retourné sur GET /users/{id}.
 *
 * @author Borel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String nom;
    private String email;
    private String pays;
    private String langue;
    private String role;
    private String abonnement;
    private String photoProfil;
    private java.time.LocalDateTime createdAt;
}
