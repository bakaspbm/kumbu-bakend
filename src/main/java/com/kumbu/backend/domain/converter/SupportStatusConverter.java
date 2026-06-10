package com.kumbu.backend.domain.converter;

import com.kumbu.backend.domain.enums.SupportStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SupportStatusConverter implements AttributeConverter<SupportStatus, String> {

    @Override
    public String convertToDatabaseColumn(SupportStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public SupportStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return SupportStatus.valueOf(dbData.trim().toUpperCase());
    }
}
