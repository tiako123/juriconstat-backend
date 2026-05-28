package com.juriconstat.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utilitaire JWT : génération, validation et extraction des claims.
 * Utilise JJWT avec algorithme HS256 et clé secrète configurée en properties.
 *
 * @author Borel
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // ─── Génération ────────────────────────────────────────────────────────────

    /**
     * Génère un token JWT pour l'utilisateur donné.
     *
     * @param email l'email utilisé comme subject
     * @param role  le rôle (ROLE_USER / ROLE_ADMIN)
     * @return token JWT signé
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    // ─── Extraction ────────────────────────────────────────────────────────────

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // ─── Validation ────────────────────────────────────────────────────────────

    /**
     * Valide le token : signature correcte et non expiré.
     */
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ─── Helpers privés ────────────────────────────────────────────────────────

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
