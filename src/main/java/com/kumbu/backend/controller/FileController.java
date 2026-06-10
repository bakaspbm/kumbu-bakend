package com.kumbu.backend.controller;



import com.kumbu.backend.service.StorageService;

import com.kumbu.backend.security.SecurityUtils;

import com.kumbu.backend.domain.entity.User;

import com.kumbu.backend.repository.UserRepository;

import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;



import java.util.Map;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/files")

@RequiredArgsConstructor

@Validated

public class FileController {



    private final StorageService storageService;

    private final UserRepository userRepository;

    private final SecurityUtils securityUtils;



    @PostMapping("/avatar")

    public Map<String, String> uploadAvatar(@RequestParam("file") @NotNull MultipartFile file) {

        UUID userId = securityUtils.currentUserId();

        String url = storageService.storeAvatar(userId, file);

        User user = userRepository.findById(userId).orElseThrow();

        user.setPhotoUrl(url);

        userRepository.save(user);

        return Map.of("url", url);

    }



    @PostMapping("/listing")

    public Map<String, String> uploadListingImage(@RequestParam("file") @NotNull MultipartFile file) {

        UUID userId = securityUtils.currentUserId();

        String url = storageService.storeListing(userId, file);

        return Map.of("url", url);

    }

}

