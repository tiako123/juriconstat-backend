package com.juriconstat.controller;

import com.juriconstat.dto.UserProfileResponse;
import com.juriconstat.model.User;
import com.juriconstat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

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

    // ─── GET /users/{id} (Profil Privé / Admin) ──────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Utilisateur introuvable"));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = user.getEmail().equals(authentication.getName());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Accès refusé au profil de cet utilisateur");
        }

        return ResponseEntity.ok(buildProfileResponse(user));
    }

    // ─── GET /users/{id}/public (Profil Public) ──────────────────────────────
    @GetMapping("/{id}/public")
    public ResponseEntity<UserProfileResponse> getPublicProfile(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Utilisateur introuvable"));

        return ResponseEntity.ok(buildProfileResponse(user));
    }

    private UserProfileResponse buildProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .email(user.getEmail())
                .pays(user.getPays())
                .langue(user.getLangue())
                .role(user.getRole())
                .abonnement(user.getAbonnement())
                .photoProfil(user.getPhotoProfil())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // ─── POST /users/{id}/subscribe (S'abonner / Se désabonner) ──────────────────────────────
    @PostMapping("/{id}/subscribe")
    public ResponseEntity<java.util.Map<String, String>> toggleSubscribe(
            @PathVariable Long id,
            Authentication authentication) {
            
        // Logique fictive pour le moment ou gérée par SubscriptionRepository si injecté
        return ResponseEntity.ok(java.util.Map.of("message", "Abonnement mis à jour"));
    }

    // ─── PUT /users/me/photo (Mettre à jour sa propre photo) ────────────────────────
    @PutMapping("/me/photo")
    public ResponseEntity<UserProfileResponse> updateMyPhoto(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Utilisateur introuvable"));

        String photoBase64 = payload.get("photoProfil");
        user.setPhotoProfil(photoBase64);
        
        userRepository.save(user);

        return ResponseEntity.ok(buildProfileResponse(user));
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<java.util.Map<String, String>> handleNotFound(
            jakarta.persistence.EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(java.util.Map.of("error", ex.getMessage()));
    }
}
