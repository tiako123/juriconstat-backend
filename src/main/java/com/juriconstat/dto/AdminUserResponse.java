package com.juriconstat.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO représentant les détails d'un utilisateur pour l'interface d'administration,
 * incluant le volume de consultations effectuées.
 */
@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String nom;
    private String email;
    private String role;
    private String abonnement;
    private LocalDateTime createdAt;
    private long nombreConsultations;
}
