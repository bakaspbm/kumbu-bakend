package com.kumbu.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableConfigurationProperties(KumbuCacheProperties.class)
@RequiredArgsConstructor
public class CacheConfig {

    private final KumbuCacheProperties properties;

    private static final List<String> ALL_CACHES = List.of(
            CacheNames.CATALOG_CATEGORIES,
            CacheNames.CATALOG_SUBCATEGORIES,
            CacheNames.CATALOG_SEARCH,
            CacheNames.CATALOG_FEATURED,
            CacheNames.CATALOG_LISTING,
            CacheNames.RECOMMENDATIONS,
            CacheNames.PLATFORM,
            CacheNames.MONETIZATION,
            CacheNames.JOBS,
            CacheNames.ADMIN_STATS
    );

    @Bean
    @Primary
    @ConditionalOnProperty(name = "kumbu.cache.redis-enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(ALL_CACHES);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.getDefaultTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(10_000));
        manager.registerCustomCache(CacheNames.CATALOG_CATEGORIES, Caffeine.newBuilder()
                .expireAfterWrite(properties.getPlatformTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(100)
                .build());
        manager.registerCustomCache(CacheNames.CATALOG_SUBCATEGORIES, Caffeine.newBuilder()
                .expireAfterWrite(properties.getPlatformTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(500)
                .build());
        manager.registerCustomCache(CacheNames.PLATFORM, Caffeine.newBuilder()
                .expireAfterWrite(properties.getPlatformTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(200)
                .build());
        manager.registerCustomCache(CacheNames.ADMIN_STATS, Caffeine.newBuilder()
                .expireAfterWrite(properties.getAdminTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(200)
                .build());
        return manager;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "kumbu.cache.redis-enabled", havingValue = "true")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(properties.getDefaultTtlSeconds()))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        Duration platformTtl = Duration.ofSeconds(properties.getPlatformTtlSeconds());
        Duration adminTtl = Duration.ofSeconds(properties.getAdminTtlSeconds());

        perCache.put(CacheNames.CATALOG_CATEGORIES, defaults.entryTtl(platformTtl));
        perCache.put(CacheNames.CATALOG_SUBCATEGORIES, defaults.entryTtl(platformTtl));
        perCache.put(CacheNames.PLATFORM, defaults.entryTtl(platformTtl));
        perCache.put(CacheNames.ADMIN_STATS, defaults.entryTtl(adminTtl));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
