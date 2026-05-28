package com.juriconstat.controller;

import com.juriconstat.dto.UserProfileResponse;
import com.juriconstat.model.User;
import com.juriconstat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des utilisateurs.
 *
 * GET /users/{id}    → 200 OK  + profil de l'utilisateur
 * (Accès restreint : un utilisateur ne peut voir que son propre profil,
 *  un admin peut voir n'importe quel profil.)
 *
 * @author Borel
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // ─── GET /users/{id} ───────────────────────────────────────────────────────

    /**
     * Récupère le profil d'un utilisateur par son ID.
     * Un utilisateur ne peut accéder qu'à son propre profil.
     * Un ROLE_ADMIN peut accéder à tous les profils.
     *
     * @param id             ID de l'utilisateur
     * @param authentication contexte de sécurité injecté par Spring
     * @return profil utilisateur ou 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Utilisateur introuvable avec l'id : " + id));

        // Vérification d'accès : seul l'utilisateur lui-même ou un admin peut voir le profil
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isOwner = user.getEmail().equals(authentication.getName());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Accès refusé au profil de cet utilisateur");
        }

        UserProfileResponse profile = UserProfileResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .email(user.getEmail())
                .pays(user.getPays())
                .langue(user.getLangue())
                .role(user.getRole())
                .abonnement(user.getAbonnement())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(profile);
    }

    // ─── Gestion des erreurs ───────────────────────────────────────────────────

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<java.util.Map<String, String>> handleNotFound(
            jakarta.persistence.EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(java.util.Map.of("error", ex.getMessage()));
    }
}
