package com.kumbu.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kumbu.cache")
public class KumbuCacheProperties {

    /** Redis em produção; Caffeine em memória local quando false. */
    private boolean redisEnabled = false;

    private int defaultTtlSeconds = 60;
    private int platformTtlSeconds = 300;
    private int adminTtlSeconds = 120;
}
