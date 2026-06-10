package com.kumbu.backend.domain.converter;

import com.kumbu.backend.domain.enums.SignupSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SignupSourceConverter implements AttributeConverter<SignupSource, String> {

    @Override
    public String convertToDatabaseColumn(SignupSource attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public SignupSource convertToEntityAttribute(String dbData) {
        return dbData == null ? SignupSource.UNKNOWN : SignupSource.valueOf(dbData.toUpperCase());
    }
}
