package com.kumbu.backend.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class AdminBanUserRequest {
    @Size(max = 500)
    private String reason;

    private Instant until;
}
