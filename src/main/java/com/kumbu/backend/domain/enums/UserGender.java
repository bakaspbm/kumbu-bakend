package com.kumbu.backend.domain.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum UserGender {
    MALE("male", "masculino"),
    FEMALE("female", "feminino"),
    OTHER("other", "outro"),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "prefere_nao_dizer");

    private final String code;
    private final String alias;

    UserGender(String code, String alias) {
        this.code = code;
        this.alias = alias;
    }

    public String getCode() {
        return code;
    }

    public static Optional<UserGender> fromValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(g -> g.code.equals(normalized) || g.alias.equals(normalized))
                .findFirst();
    }
}
