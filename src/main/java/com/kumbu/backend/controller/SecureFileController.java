package com.kumbu.backend.controller;

import com.kumbu.backend.service.SecureFileAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class SecureFileController {

    private final SecureFileAccessService secureFileAccessService;

    @GetMapping("/chat/**")
    public ResponseEntity<Resource> downloadChatFile(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/api/v1/files/chat/";
        int index = uri.indexOf(prefix);
        String relative = index >= 0 ? uri.substring(index + prefix.length()) : "";
        return secureFileAccessService.serveChatFile(relative);
    }
}
