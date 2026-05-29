package com.juriconstat.controller;

import com.juriconstat.dto.AdminUserResponse;
import com.juriconstat.model.User;
import com.juriconstat.repository.ConsultationRepository;
import com.juriconstat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour les actions d'administration.
 * Tous les endpoints sont préfixés par /admin et requièrent le rôle ADMIN.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;

    /**
     * Récupère la liste de tous les utilisateurs inscrits avec leur nombre total de consultations.
     * Accessible uniquement pour les administrateurs.
     *
     * @return liste d'objets AdminUserResponse
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAdminUsersList() {
        List<User> users = userRepository.findAll();

        List<AdminUserResponse> response = users.stream()
                .map(user -> {
                    long count = consultationRepository.countByUserId(user.getId());
                    return AdminUserResponse.builder()
                            .id(user.getId())
                            .nom(user.getNom())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .abonnement(user.getAbonnement())
                            .createdAt(user.getCreatedAt())
                            .nombreConsultations(count)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
