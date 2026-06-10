package com.kumbu.backend.domain.converter;

import com.kumbu.backend.domain.enums.AdminRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AdminRoleConverter implements AttributeConverter<AdminRole, String> {

    @Override
    public String convertToDatabaseColumn(AdminRole attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    @Override
    public AdminRole convertToEntityAttribute(String dbData) {
        if (dbData == null) return AdminRole.ADMIN;
        return AdminRole.valueOf(dbData.toUpperCase().replace("-", "_"));
    }
}
