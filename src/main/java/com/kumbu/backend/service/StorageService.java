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



    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".pdf"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final long MAX_BYTES = 10L * 1024 * 1024;



    private static final Set<String> CHAT_CONTENT_TYPES = Set.of(

            "image/jpeg", "image/png", "image/webp", "image/gif",
            "image/heic", "image/heif", "application/pdf"

    );



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

        Files.createDirectories(basePath.resolve("chat"));

        Files.createDirectories(privateBasePath.resolve("identity"));

    }



    public String storeAvatar(UUID userId, MultipartFile file) {

        validateUpload(file);

        return store("avatars/" + userId + "_" + UUID.randomUUID() + safeExt(file), file);

    }



    public String storeListing(UUID userId, MultipartFile file) {

        validateUpload(file);

        return store("listings/" + userId + "_" + UUID.randomUUID() + safeExt(file), file);

    }



    public String storeChatAttachment(UUID userId, MultipartFile file) {

        validateChatUpload(file);

        String relative = "chat/" + userId + "_" + UUID.randomUUID() + safeExt(file);
        store(relative, file);
        return secureChatFileUrl(relative);

    }

    public String secureChatFileUrl(String relativePath) {
        String normalized = relativePath.replace('\\', '/').replaceFirst("^/+", "");
        String name = normalized.startsWith("chat/") ? normalized.substring("chat/".length()) : normalized;
        String apiBase = apiPublicBaseUrl();
        return apiBase + "/chat/" + name;
    }

    private String apiPublicBaseUrl() {
        String filesBase = properties.getStorage().getPublicBaseUrl();
        if (filesBase != null && filesBase.endsWith("/files")) {
            return filesBase.substring(0, filesBase.length() - "/files".length()) + "/api/v1/files";
        }
        if (filesBase != null && filesBase.contains("/files")) {
            return filesBase.replace("/files", "/api/v1/files");
        }
        return "http://localhost:8080/api/v1/files";
    }



    private void validateChatUpload(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw ApiException.badRequest("Ficheiro é obrigatório");

        }

        if (file.getSize() > MAX_BYTES) {

            throw ApiException.badRequest("Ficheiro demasiado grande (máx. 10MB)");

        }

        String contentType = file.getContentType();

        if (contentType == null || !CHAT_CONTENT_TYPES.contains(contentType.toLowerCase())) {

            throw ApiException.badRequest("Tipo não suportado. Use JPEG, PNG, WEBP, GIF ou PDF");

        }

    }



    /** Guarda documento de identidade em armazenamento privado (sem URL pública). */

    public String storeIdentity(UUID userId, String side, MultipartFile file) {

        validateUpload(file);

        String relative = "identity/" + userId + "/" + side + "_" + UUID.randomUUID() + safeExt(file);

        try {

            Path target = resolveWritablePrivatePath(relative);

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

            Path target = resolveWritablePublicPath(relativePath);

            Files.createDirectories(target.getParent());

            file.transferTo(target);

            return getPublicUrlForRelativePath(relativePath.replace('\\', '/'));

        } catch (IOException e) {

            throw ApiException.badRequest("Falha ao guardar ficheiro");

        }

    }

    public Path resolvePublicPath(String relativePath) {
        return resolveReadablePath(basePath, relativePath);
    }

    private Path resolveWritablePublicPath(String relativePath) {
        return resolveWritablePath(basePath, relativePath);
    }

    private Path resolveWritablePrivatePath(String relativePath) {
        return resolveWritablePath(privateBasePath, relativePath);
    }

    private Path resolveWritablePath(Path root, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw ApiException.badRequest("Caminho inválido");
        }
        Path resolved = root.resolve(relativePath.replace('\\', '/')).normalize();
        if (!resolved.startsWith(root)) {
            throw ApiException.badRequest("Caminho inválido");
        }
        return resolved;
    }

    private Path resolveReadablePath(Path root, String relativePath) {
        Path resolved = resolveWritablePath(root, relativePath);
        if (!Files.exists(resolved)) {
            throw ApiException.notFound("Ficheiro não encontrado");
        }
        return resolved;
    }

    public String getPublicUrlForRelativePath(String relativePath) {
        return properties.getStorage().getPublicBaseUrl() + "/" + relativePath.replace('\\', '/').replaceFirst("^/+", "");
    }

    private String safeExt(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            return ".jpg";
        }
        String ext = name.substring(name.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw ApiException.badRequest("Extensão de ficheiro não permitida");
        }
        if (ext.contains("/") || ext.contains("\\") || ext.contains("..")) {
            throw ApiException.badRequest("Nome de ficheiro inválido");
        }
        return ext;
    }

    public Path resolvePrivatePath(String relativePath) {
        return resolveReadablePath(privateBasePath, relativePath);
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

