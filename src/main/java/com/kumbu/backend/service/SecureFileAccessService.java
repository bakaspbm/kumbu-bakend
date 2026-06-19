package com.kumbu.backend.service;

import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.ChatMessageRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecureFileAccessService {

    private final StorageService storageService;
    private final StorageUrlValidator storageUrlValidator;
    private final ChatMessageRepository chatMessageRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> serveChatFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw ApiException.notFound("Ficheiro não encontrado");
        }
        String normalized = relativePath.replace('\\', '/').replaceFirst("^/+", "");
        if (!normalized.startsWith("chat/")) {
            throw ApiException.notFound("Ficheiro não encontrado");
        }

        UUID userId = securityUtils.currentUserId();
        assertChatFileAccess(userId, normalized);

        Path file = storageService.resolvePublicPath(normalized);
        Resource resource = new FileSystemResource(file);
        String contentType = storageService.probeContentType(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private void assertChatFileAccess(UUID userId, String relativePath) {
        String prefix = "chat/" + userId + "_";
        if (relativePath.startsWith(prefix)) {
            return;
        }
        String publicUrl = storageService.getPublicUrlForRelativePath(relativePath);
        boolean participant = chatMessageRepository.existsByAttachmentUrlAndParticipant(publicUrl, userId);
        if (participant) {
            return;
        }
        throw ApiException.forbidden("Sem acesso a este ficheiro");
    }
}
