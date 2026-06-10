package com.kumbu.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OAuthProfileHint {

    @Size(max = 64)
    private String facebookId;

    @Size(max = 64)
    private String googleSub;

    @Email
    @Size(max = 320)
    private String email;

    @Size(max = 200)
    private String name;

    @Size(max = 2048)
    private String photoUrl;
}
