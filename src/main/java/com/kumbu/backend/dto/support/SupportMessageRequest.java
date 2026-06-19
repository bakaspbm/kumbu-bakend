package com.kumbu.backend.dto.support;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportMessageRequest {
    @Size(max = 4000)
    private String body;

    @Size(max = 2048)
    private String attachmentUrl;
}
