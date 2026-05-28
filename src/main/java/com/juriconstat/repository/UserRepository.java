package com.juriconstat.repository;

import com.juriconstat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository JPA pour l'entité User.
 * Fournit l'accès aux données utilisateurs via Spring Data.
 *
 * @author Borel
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trouve un utilisateur par son email.
     * Utilisé par AuthService lors de la connexion.
     *
     * @param email l'adresse email de l'utilisateur
     * @return l'utilisateur s'il existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Vérifie si un email est déjà enregistré.
     * Utilisé lors de l'inscription pour éviter les doublons.
     *
     * @param email l'adresse email à vérifier
     * @return true si l'email existe déjà
     */
    boolean existsByEmail(String email);
}
