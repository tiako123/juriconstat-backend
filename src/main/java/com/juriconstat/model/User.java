package com.juriconstat.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un utilisateur de JuriConstat.
 * Correspond à la classe User du diagramme de conception.
 *
 * Relations :
 *  - Un User possède plusieurs Consultations (1..*)
 *  - Un User est géré par AuthService
 *
 * @author Borel
 */
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // ===== Identifiant =====

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Informations personnelles =====

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;  // Stocké en BCrypt

    @Column(length = 100)
    private String pays;

    @Column(length = 10)
    private String langue;  // "fr", "en", etc.

    // ===== Rôle et abonnement =====

    /**
     * Rôle de l'utilisateur : USER ou ADMIN
     * Utilisé par Spring Security via AuthService.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "USER";

    /**
     * Type d'abonnement : GRATUIT ou PREMIUM
     * Détermine le quota de consultations.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String abonnement = "GRATUIT";

    // ===== Audit =====

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
