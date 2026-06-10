package com.kumbu.backend.domain.converter;

import com.kumbu.backend.domain.enums.DealStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DealStatusConverter implements AttributeConverter<DealStatus, String> {

    @Override
    public String convertToDatabaseColumn(DealStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public DealStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? DealStatus.OPEN : DealStatus.valueOf(dbData.toUpperCase());
    }
}
