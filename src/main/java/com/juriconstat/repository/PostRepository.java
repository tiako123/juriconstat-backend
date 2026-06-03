package com.juriconstat.repository;

import com.juriconstat.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // Récupérer les posts triés par date décroissante
    List<Post> findAllByOrderByCreatedAtDesc();
}
