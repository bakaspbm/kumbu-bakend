package com.kumbu.backend.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportMessageRequest {
    @NotBlank
    @Size(max = 4000)
    private String body;
}
