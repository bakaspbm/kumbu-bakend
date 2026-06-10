package com.kumbu.backend.dto.auth;

import com.kumbu.backend.domain.enums.SignupSource;
import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthRequest {

    @NotBlank(message = "Access token é obrigatório")
    private String accessToken;

    /** Perfil obtido no browser (fallback quando o servidor não alcança graph.facebook.com). */
    private OAuthProfileHint profile;

    @OneOf(value = {"app", "web", "unknown"}, message = "Origem de registo inválida")
    private String signupSource = SignupSource.APP.name().toLowerCase();
}
