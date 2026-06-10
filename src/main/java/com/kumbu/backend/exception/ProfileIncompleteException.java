package com.kumbu.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class ProfileIncompleteException extends ApiException {

    private final List<String> missingFields;

    private ProfileIncompleteException(String message, List<String> missingFields) {
        super(HttpStatus.FORBIDDEN, "PROFILE_INCOMPLETE", message);
        this.missingFields = missingFields;
    }

    public static ProfileIncompleteException of(List<String> missingFields, String message) {
        return new ProfileIncompleteException(message, missingFields);
    }
}
