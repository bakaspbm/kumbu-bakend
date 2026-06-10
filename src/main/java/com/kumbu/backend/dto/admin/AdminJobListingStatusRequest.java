package com.kumbu.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminJobListingStatusRequest {

    @NotBlank
    @Pattern(regexp = "active|filled_hidden")
    private String status;
}
