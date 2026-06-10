package com.kumbu.backend.controller;

import com.kumbu.backend.dto.user.UserProfileResponse;
import com.kumbu.backend.service.AdminManagementService;
import com.kumbu.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AdminManagementService adminManagementService;

    @GetMapping("/me")
    public Map<String, Object> adminMe() {
        UserProfileResponse me = userService.me();
        return Map.of(
                "userId", me.getId().toString(),
                "email", me.getEmail() != null ? me.getEmail() : "",
                "role", adminManagementService.currentAdminRole()
        );
    }
}
