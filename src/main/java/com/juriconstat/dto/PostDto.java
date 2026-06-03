package com.juriconstat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private Long id;
    private Long auteurId;
    private String auteurNom;
    private String auteurPhoto;
    private String titre;
    private String contenu;
    private String imageUri;
    private int likes;
    private int commentsCount;
    private LocalDateTime createdAt;
}
