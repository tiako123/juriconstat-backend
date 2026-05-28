package com.juriconstat.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant une consultation juridique.
 * Chaque consultation est soumise par un utilisateur, traitée par Gemini IA,
 * et stockée pour constituer l'historique.
 *
 * Relations :
 *  - Un User possède plusieurs Consultations (N..1)
 *
 * @author Brad (adapté par Borel)
 */
@Entity
@Table(name = "consultations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation {

    // ===== Identifiant =====

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Relation vers l'utilisateur =====

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ===== Contenu de la consultation =====

    /**
     * Requête textuelle soumise par l'utilisateur.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String requete;

    /**
     * Réponse juridique générée par Gemini IA.
     */
    @Column(name = "reponse_ia", nullable = false, columnDefinition = "TEXT")
    private String reponseIa;

    // ===== Contexte juridique =====

    /**
     * Pays cible de la consultation (ex : "CM", "SN", "FR")
     * Utilisé pour contextualiser la réponse IA.
     */
    @Column(nullable = false, length = 100)
    private String pays;

    /**
     * Langue de la réponse (ex : "fr", "en")
     */
    @Column(nullable = false, length = 10)
    private String langue;

    // ===== Audit =====

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
