package com.juriconstat.service;

import com.juriconstat.dto.AuthResponse;
import com.juriconstat.dto.LoginRequest;
import com.juriconstat.dto.RegisterRequest;
import com.juriconstat.model.User;
import com.juriconstat.repository.UserRepository;
import com.juriconstat.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service d'authentification.
 * Gère l'inscription (BCrypt) et la connexion (vérification + JWT).
 *
 * @author Borel
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    // ─── Inscription ───────────────────────────────────────────────────────────

    /**
     * Enregistre un nouvel utilisateur.
     *
     * @param request données d'inscription validées
     * @return réponse avec token JWT, role et userId
     * @throws IllegalArgumentException si l'email est déjà utilisé
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé : " + request.getEmail());
        }

        User user = User.builder()
                .nom(request.getNom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .pays(request.getPays())
                .langue(request.getLangue())
                .role("ROLE_USER")
                .abonnement("GRATUIT")
                .build();

        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole());

        return AuthResponse.builder()
                .token(token)
                .role(saved.getRole())
                .userId(saved.getId())
                .nom(saved.getNom())
                .email(saved.getEmail())
                .build();
    }

    // ─── Connexion ─────────────────────────────────────────────────────────────

    /**
     * Authentifie un utilisateur par email + mot de passe.
     *
     * @param request données de connexion
     * @return réponse avec token JWT, role et userId
     * @throws IllegalArgumentException si les identifiants sont invalides
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Identifiants invalides"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Identifiants invalides");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole())
                .userId(user.getId())
                .nom(user.getNom())
                .email(user.getEmail())
                .build();
    }
}
