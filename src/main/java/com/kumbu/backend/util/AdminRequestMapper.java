package com.kumbu.backend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AdminRequestMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private AdminRequestMapper() {
    }

    public static Map<String, Object> toPayload(Object request) {
        Map<String, Object> map = MAPPER.convertValue(request, new TypeReference<>() {});
        Map<String, Object> filtered = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (value != null) {
                filtered.put(key, value);
            }
        });
        return filtered;
    }
}
