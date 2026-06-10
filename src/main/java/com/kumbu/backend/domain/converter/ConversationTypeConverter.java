package com.kumbu.backend.domain.converter;

import com.kumbu.backend.domain.enums.ConversationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ConversationTypeConverter implements AttributeConverter<ConversationType, String> {

    @Override
    public String convertToDatabaseColumn(ConversationType attribute) {
        return attribute == null ? ConversationType.MARKETPLACE.name().toLowerCase()
                : attribute.name().toLowerCase();
    }

    @Override
    public ConversationType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return ConversationType.MARKETPLACE;
        }
        return ConversationType.valueOf(dbData.trim().toUpperCase());
    }
}
