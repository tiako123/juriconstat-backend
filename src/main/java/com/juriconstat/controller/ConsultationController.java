package com.juriconstat.controller;

import com.juriconstat.dto.ConsultationRequest;
import com.juriconstat.dto.ConsultationResponse;
import com.juriconstat.service.ConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour les consultations juridiques.
 *
 * Endpoints :
 *   POST /consultations                      → Créer une nouvelle consultation (IA)
 *   GET  /consultations/user/{userId}        → Récupérer l'historique d'un utilisateur
 *
 * Tous les endpoints nécessitent un JWT valide (authentification obligatoire).
 *
 * @author Brad
 */
@RestController
@RequestMapping("/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    // ─── POST /consultations ───────────────────────────────────────────────────

    /**
     * Soumet une nouvelle requête juridique à l'IA Gemini.
     * La consultation est sauvegardée dans l'historique de l'utilisateur.
     *
     * @param request        corps JSON avec le texte de la requête
     * @param authentication contexte de sécurité (email extrait du JWT)
     * @return 201 Created + ConsultationResponse avec la réponse de l'IA
     */
    @PostMapping
    public ResponseEntity<ConsultationResponse> creerConsultation(
            @Valid @RequestBody ConsultationRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();
        ConsultationResponse response = consultationService.creerConsultation(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── GET /consultations/user/{userId} ──────────────────────────────────────

    /**
     * Récupère l'historique des consultations d'un utilisateur.
     * Un utilisateur ne peut accéder qu'à son propre historique.
     * Un admin peut accéder à l'historique de tous les utilisateurs.
     *
     * @param userId         ID de l'utilisateur
     * @param authentication contexte de sécurité
     * @return liste des consultations triées de la plus récente à la plus ancienne
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConsultationResponse>> getHistorique(
            @PathVariable Long userId,
            Authentication authentication) {

        String callerEmail = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<ConsultationResponse> historique =
                consultationService.getHistorique(userId, callerEmail, isAdmin);

        return ResponseEntity.ok(historique);
    }

    // ─── Gestion des erreurs ───────────────────────────────────────────────────

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            jakarta.persistence.EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }
}
