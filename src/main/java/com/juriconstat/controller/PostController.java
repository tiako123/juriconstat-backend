package com.juriconstat.controller;

import com.juriconstat.dto.PostDto;
import com.juriconstat.dto.PostRequest;
import com.juriconstat.model.Post;
import com.juriconstat.model.User;
import com.juriconstat.repository.PostRepository;
import com.juriconstat.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<PostDto>> getAllPosts() {
        List<PostDto> posts = postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(post -> PostDto.builder()
                        .id(post.getId())
                        .auteurId(post.getAuteur().getId())
                        .auteurNom(post.getAuteur().getNom())
                        .auteurPhoto(post.getAuteur().getPhotoProfil())
                        .titre(post.getTitre())
                        .contenu(post.getContenu())
                        .imageUri(post.getImageUri())
                        .likes(post.getLikes())
                        .commentsCount(post.getCommentsCount())
                        .createdAt(post.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(posts);
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(
            @Valid @RequestBody PostRequest request,
            Authentication authentication) {
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        // Vérification du rôle
        String userRole = user.getRole();
        if (!userRole.equals("ROLE_PROFESSIONNEL") && !userRole.equals("ROLE_PARTENAIRE") && !userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Post post = Post.builder()
                .auteur(user)
                .titre(request.getTitre())
                .contenu(request.getContenu())
                .imageUri(request.getImageUri())
                .likes(0)
                .commentsCount(0)
                .build();

        Post savedPost = postRepository.save(post);

        PostDto dto = PostDto.builder()
                .id(savedPost.getId())
                .auteurId(savedPost.getAuteur().getId())
                .auteurNom(savedPost.getAuteur().getNom())
                .auteurPhoto(savedPost.getAuteur().getPhotoProfil())
                .titre(savedPost.getTitre())
                .contenu(savedPost.getContenu())
                .imageUri(savedPost.getImageUri())
                .likes(savedPost.getLikes())
                .commentsCount(savedPost.getCommentsCount())
                .createdAt(savedPost.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
