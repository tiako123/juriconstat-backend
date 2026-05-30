package com.juriconstat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OcrRequest {
    @NotBlank(message = "L'image (base64) est obligatoire pour l'OCR")
    private String base64Image;
    
    @NotBlank(message = "Le type MIME de l'image est obligatoire")
    private String mimeType;
}
