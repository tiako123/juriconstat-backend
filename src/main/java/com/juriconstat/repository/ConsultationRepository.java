package com.juriconstat.repository;

import com.juriconstat.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository JPA pour l'entité Consultation.
 * Gère la persistance et la récupération de l'historique des consultations.
 *
 * @author Brad
 */
@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    /**
     * Récupère toutes les consultations d'un utilisateur, triées par date décroissante.
     * Utilisé pour afficher l'historique.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return liste des consultations, de la plus récente à la plus ancienne
     */
    List<Consultation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Compte le nombre de consultations effectuées par un utilisateur après une certaine date.
     * Utilisé pour le contrôle de quota mensuel.
     */
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime date);

    /**
     * Compte le nombre total de consultations effectuées par un utilisateur.
     * Utilisé pour l'interface d'administration.
     */
    long countByUserId(Long userId);
}
