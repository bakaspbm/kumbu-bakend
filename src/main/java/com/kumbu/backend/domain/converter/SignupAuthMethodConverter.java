package com.kumbu.backend.domain.converter;

import com.kumbu.backend.domain.enums.SignupAuthMethod;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SignupAuthMethodConverter implements AttributeConverter<SignupAuthMethod, String> {

    @Override
    public String convertToDatabaseColumn(SignupAuthMethod attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public SignupAuthMethod convertToEntityAttribute(String dbData) {
        return dbData == null ? SignupAuthMethod.UNKNOWN : SignupAuthMethod.valueOf(dbData.toUpperCase());
    }
}
