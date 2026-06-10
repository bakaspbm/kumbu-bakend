package com.kumbu.backend.dto.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OAuthPublicConfigResponse {
    boolean googleEnabled;
    boolean facebookEnabled;
    String googleClientId;
    String facebookAppId;
}
