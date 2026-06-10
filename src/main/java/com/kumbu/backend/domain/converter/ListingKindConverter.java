package com.kumbu.backend.domain.converter;

import com.kumbu.backend.domain.enums.ListingKind;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ListingKindConverter implements AttributeConverter<ListingKind, String> {

    @Override
    public String convertToDatabaseColumn(ListingKind attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public ListingKind convertToEntityAttribute(String dbData) {
        return dbData == null ? ListingKind.GENERAL : ListingKind.valueOf(dbData.toUpperCase());
    }
}
