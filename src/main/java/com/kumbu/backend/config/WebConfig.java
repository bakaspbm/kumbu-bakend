package com.kumbu.backend.config;

import com.kumbu.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final StorageService storageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/avatars/**", "/files/listings/**")
                .addResourceLocations("file:" + storageService.getBasePath().toAbsolutePath() + "/")
                .setCachePeriod(31_536_000);
    }
}
