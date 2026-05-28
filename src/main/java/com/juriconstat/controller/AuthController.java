package com.juriconstat.controller;

import com.juriconstat.dto.AuthResponse;
import com.juriconstat.dto.LoginRequest;
import com.juriconstat.dto.RegisterRequest;
import com.juriconstat.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur REST d'authentification.
 *
 * POST /auth/register → 201 Created + {token, role, userId, nom}
 * POST /auth/login    → 200 OK    + {token, role, userId, nom}
 *
 * @author Borel
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─── POST /auth/register ───────────────────────────────────────────────────

    /**
     * Inscription d'un nouvel utilisateur.
     *
     * @param request corps JSON validé
     * @return 201 Created avec le token JWT
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── POST /auth/login ──────────────────────────────────────────────────────

    /**
     * Connexion d'un utilisateur existant.
     *
     * @param request corps JSON validé
     * @return 200 OK avec le token JWT
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ─── Gestion des erreurs métier ────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
