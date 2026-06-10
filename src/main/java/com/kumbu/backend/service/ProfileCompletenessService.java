package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.UserGender;
import com.kumbu.backend.exception.ProfileIncompleteException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProfileCompletenessService {

    public static final int MIN_AGE_TO_PUBLISH = 18;

    public Map<String, Object> assess(User user) {
        List<String> missing = findMissingFields(user);
        boolean complete = missing.isEmpty();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile_complete", complete);
        result.put("can_publish", complete && user.isActive());
        result.put("missing_fields", missing);
        result.put("missing_labels", missing.stream().map(this::fieldLabel).toList());
        result.put("age", computeAge(user.getBirthDate()));
        result.put("message", complete
                ? "Perfil completo — pode publicar anúncios"
                : "Complete o perfil antes de publicar: " + String.join(", ", missing.stream().map(this::fieldLabel).toList()));
        return result;
    }

    public void assertCanPublish(User user) {
        if (!user.isActive()) {
            throw ProfileIncompleteException.of(
                    List.of("account"),
                    "Conta indisponível para publicar anúncios");
        }
        List<String> missing = findMissingFields(user);
        if (!missing.isEmpty()) {
            throw ProfileIncompleteException.of(missing,
                    "Complete o perfil antes de publicar. Campos em falta: "
                            + String.join(", ", missing.stream().map(this::fieldLabel).toList()));
        }
    }

    List<String> findMissingFields(User user) {
        List<String> missing = new ArrayList<>();
        if (isBlank(user.getDisplayName()) || user.getDisplayName().trim().length() < 2) {
            missing.add("display_name");
        }
        if (isBlank(user.getPhone()) || normalizePhone(user.getPhone()).length() < 9) {
            missing.add("phone");
        }
        if (isBlank(user.getCity())) {
            missing.add("city");
        }
        if (isBlank(user.getRegion())) {
            missing.add("region");
        }
        if (isBlank(user.getCountry())) {
            missing.add("country");
        }
        if (UserGender.fromValue(user.getGender()).isEmpty()) {
            missing.add("gender");
        }
        if (!hasValidAge(user.getBirthDate())) {
            missing.add("birth_date");
        }
        if (!hasDeliveryStreet(user.getDeliveryAddress())) {
            missing.add("delivery_street");
        }
        return missing;
    }

    static boolean hasDeliveryStreet(Map<String, String> deliveryAddress) {
        if (deliveryAddress == null || deliveryAddress.isEmpty()) {
            return false;
        }
        return hasText(deliveryAddress.get("street")) || hasText(deliveryAddress.get("line1"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasText(Object value) {
        return value != null && !value.toString().isBlank();
    }

    public static boolean hasValidAge(LocalDate birthDate) {
        Integer age = computeAge(birthDate);
        return age != null && age >= MIN_AGE_TO_PUBLISH && age <= 120;
    }

    public static Integer computeAge(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public static void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Data de nascimento é obrigatória");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de nascimento inválida");
        }
        Integer age = computeAge(birthDate);
        if (age == null || age > 120) {
            throw new IllegalArgumentException("Data de nascimento inválida");
        }
        if (age < MIN_AGE_TO_PUBLISH) {
            throw new IllegalArgumentException("Deve ter pelo menos " + MIN_AGE_TO_PUBLISH + " anos para publicar");
        }
    }

    public static String normalizeGender(String gender) {
        return UserGender.fromValue(gender)
                .map(UserGender::getCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Género inválido. Use: male, female, other ou prefer_not_to_say"));
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case "display_name" -> "Nome completo";
            case "phone" -> "Telefone";
            case "city" -> "Cidade";
            case "region" -> "Província";
            case "country" -> "País";
            case "gender" -> "Género";
            case "birth_date" -> "Data de nascimento";
            case "delivery_street" -> "Morada";
            case "account" -> "Conta activa";
            default -> field;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizePhone(String phone) {
        return phone.replaceAll("\\D", "");
    }
}
