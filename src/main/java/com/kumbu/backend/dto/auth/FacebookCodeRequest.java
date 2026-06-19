package com.kumbu.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FacebookCodeRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String redirectUri;
    @NotBlank
    private String signupSource = "WEB";
}
