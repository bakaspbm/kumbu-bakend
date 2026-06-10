package com.kumbu.backend.service;



import com.kumbu.backend.config.KumbuProperties;

import com.kumbu.backend.exception.ApiException;

import jakarta.annotation.PostConstruct;

import lombok.Getter;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.Path;

import java.util.Set;

import java.util.UUID;



@Service

@RequiredArgsConstructor

public class StorageService {



    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(

            "image/jpeg", "image/png", "image/webp", "image/gif"

    );

    private static final long MAX_BYTES = 10L * 1024 * 1024;



    private final KumbuProperties properties;



    @Getter
    private Path basePath;

    @Getter
    private Path privateBasePath;



    @PostConstruct

    void init() throws IOException {

        basePath = Path.of(properties.getStorage().getBasePath()).toAbsolutePath().normalize();

        privateBasePath = Path.of(properties.getStorage().getPrivatePath()).toAbsolutePath().normalize();

        Files.createDirectories(basePath);

        Files.createDirectories(basePath.resolve("avatars"));

        Files.createDirectories(basePath.resolve("listings"));

        Files.createDirectories(privateBasePath.resolve("identity"));

    }



    public String storeAvatar(UUID userId, MultipartFile file) {

        validateUpload(file);

        return store("avatars/" + userId + "_" + UUID.randomUUID() + ext(file), file);

    }



    public String storeListing(UUID userId, MultipartFile file) {

        validateUpload(file);

        return store("listings/" + userId + "_" + UUID.randomUUID() + ext(file), file);

    }



    /** Guarda documento de identidade em armazenamento privado (sem URL pública). */

    public String storeIdentity(UUID userId, String side, MultipartFile file) {

        validateUpload(file);

        String relative = "identity/" + userId + "/" + side + "_" + UUID.randomUUID() + ext(file);

        try {

            Path target = privateBasePath.resolve(relative);

            Files.createDirectories(target.getParent());

            file.transferTo(target);

            return relative.replace('\\', '/');

        } catch (IOException e) {

            throw ApiException.badRequest("Falha ao guardar documento");

        }

    }



    private void validateUpload(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw ApiException.badRequest("Ficheiro é obrigatório");

        }

        if (file.getSize() > MAX_BYTES) {

            throw ApiException.badRequest("Ficheiro demasiado grande (máx. 10MB)");

        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {

            throw ApiException.badRequest("Tipo de ficheiro não suportado. Use JPEG, PNG, WEBP ou GIF");

        }

    }



    private String store(String relativePath, MultipartFile file) {

        try {

            Path target = basePath.resolve(relativePath);

            Files.createDirectories(target.getParent());

            file.transferTo(target);

            return properties.getStorage().getPublicBaseUrl() + "/" + relativePath.replace('\\', '/');

        } catch (IOException e) {

            throw ApiException.badRequest("Falha ao guardar ficheiro");

        }

    }



    private String ext(MultipartFile file) {

        String name = file.getOriginalFilename();

        if (name == null || !name.contains(".")) return ".jpg";

        return name.substring(name.lastIndexOf('.'));

    }

    public Path resolvePrivatePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw ApiException.badRequest("Caminho inválido");
        }
        Path resolved = privateBasePath.resolve(relativePath.replace('\\', '/')).normalize();
        if (!resolved.startsWith(privateBasePath)) {
            throw ApiException.badRequest("Caminho inválido");
        }
        if (!Files.exists(resolved)) {
            throw ApiException.notFound("Ficheiro não encontrado");
        }
        return resolved;
    }

    public String probeContentType(Path path) {
        try {
            String type = Files.probeContentType(path);
            return type != null ? type : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

}

