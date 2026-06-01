package com.juriconstat.service;

import com.juriconstat.dto.ConsultationRequest;
import com.juriconstat.dto.ConsultationResponse;
import com.juriconstat.model.Consultation;
import com.juriconstat.model.User;
import com.juriconstat.repository.ConsultationRepository;
import com.juriconstat.repository.UserRepository;
import com.juriconstat.exception.QuotaExceededException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de consultation juridique.
 *
 * Responsabilités :
 * - Coordonner la création d'une consultation (requête → IA → BDD).
 * - Récupérer l'historique des consultations d'un utilisateur.
 * - Appliquer la règle de sécurité : un utilisateur ne peut accéder qu'à ses propres consultations.
 *
 * @author Brad
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    // ─── Créer une consultation ────────────────────────────────────────────────

    /**
     * Traite une nouvelle consultation juridique.
     *
     * Étapes :
     * 1. Récupère l'utilisateur par son email (depuis le JWT).
     * 2. Détermine le pays et la langue (depuis la requête ou le profil utilisateur).
     * 3. Applique la règle de quota mensuel pour les abonnements GRATUIT.
     * 4. Appelle Gemini IA pour obtenir une réponse juridique.
     * 5. Sauvegarde la consultation en base de données.
     * 6. Retourne la consultation enrichie.
     *
     * @param request données de la requête juridique
     * @param userEmail email de l'utilisateur authentifié (extrait du JWT)
     * @return ConsultationResponse avec la réponse de l'IA
     */
    public ConsultationResponse creerConsultation(ConsultationRequest request, String userEmail) {
        // 1. Récupérer l'utilisateur connecté
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + userEmail));

        // 2. Vérifier le quota de consultations pour les utilisateurs gratuits (max 5 par mois)
        if ("GRATUIT".equals(user.getAbonnement())) {
            LocalDateTime startOfMonth = LocalDateTime.now()
                    .withDayOfMonth(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
            long consultationsCeMois = consultationRepository.countByUserIdAndCreatedAtAfter(user.getId(), startOfMonth);
            if (consultationsCeMois >= 20) {
                log.warn("Quota atteint pour l'utilisateur {} ({} consultations ce mois-ci)", userEmail, consultationsCeMois);
                throw new QuotaExceededException("Limite mensuelle de consultations gratuite atteinte");
            }
        }

        // 3. Déterminer pays et langue (la requête peut surcharger le profil)
        String pays   = (request.getPays()   != null && !request.getPays().isBlank())
                ? request.getPays()   : user.getPays();
        String langue = (request.getLangue() != null && !request.getLangue().isBlank())
                ? request.getLangue() : user.getLangue();

        // Valeurs par défaut si le profil n'en a pas
        if (pays   == null || pays.isBlank())   pays   = "Cameroun";
        if (langue == null || langue.isBlank()) langue = "fr";

        log.info("Nouvelle consultation pour {} | pays={} | langue={}", userEmail, pays, langue);

        // 4. Appeler Gemini IA avec les paramètres de média
        String reponseIa = geminiService.genererReponseJuridique(
                request.getRequete(), pays, langue, request.getMediaData(), request.getMediaMimeType()
        );

        // 5. Sauvegarder en base
        Consultation consultation = Consultation.builder()
                .user(user)
                .requete(request.getRequete())
                .reponseIa(reponseIa)
                .pays(pays)
                .langue(langue)
                .mediaData(request.getMediaData())
                .mediaMimeType(request.getMediaMimeType())
                .build();

        Consultation saved = consultationRepository.save(consultation);

        log.info("Consultation #{} sauvegardée avec succès", saved.getId());

        // 6. Retourner le DTO de réponse
        return toResponse(saved);
    }

    // ─── Historique des consultations ──────────────────────────────────────────

    /**
     * Récupère l'historique des consultations d'un utilisateur.
     * Un utilisateur ne peut voir que son propre historique.
     * Un ROLE_ADMIN peut voir l'historique de n'importe quel utilisateur.
     *
     * @param userId       ID de l'utilisateur dont on veut l'historique
     * @param callerEmail  email de l'utilisateur qui fait la demande (extrait du JWT)
     * @param isAdmin      true si l'appelant est ROLE_ADMIN
     * @return liste des consultations triées par date décroissante
     */
    @Transactional(readOnly = true)
    public List<ConsultationResponse> getHistorique(Long userId, String callerEmail, boolean isAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable avec l'id : " + userId));

        // Vérification d'accès
        if (!isAdmin && !user.getEmail().equals(callerEmail)) {
            throw new AccessDeniedException("Vous ne pouvez pas accéder à l'historique d'un autre utilisateur");
        }

        return consultationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Mapping entité → DTO ──────────────────────────────────────────────────

    private ConsultationResponse toResponse(Consultation c) {
        return ConsultationResponse.builder()
                .id(c.getId())
                .requete(c.getRequete())
                .reponseIa(c.getReponseIa())
                .pays(c.getPays())
                .langue(c.getLangue())
                .createdAt(c.getCreatedAt())
                .userId(c.getUser().getId())
                .mediaData(c.getMediaData())
                .mediaMimeType(c.getMediaMimeType())
                .build();
    }
}
