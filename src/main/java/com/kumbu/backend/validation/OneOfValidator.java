package com.kumbu.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class OneOfValidator implements ConstraintValidator<OneOf, String> {

    private Set<String> allowed;
    private boolean ignoreCase;

    @Override
    public void initialize(OneOf annotation) {
        ignoreCase = annotation.ignoreCase();
        allowed = Arrays.stream(annotation.value())
                .map(v -> ignoreCase ? v.toLowerCase(Locale.ROOT) : v)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = ignoreCase ? value.trim().toLowerCase(Locale.ROOT) : value.trim();
        return allowed.contains(normalized);
    }
}
