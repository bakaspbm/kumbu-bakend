package com.kumbu.backend.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailActionResponse {
    private String message;
    private String emailActionLink;
}
