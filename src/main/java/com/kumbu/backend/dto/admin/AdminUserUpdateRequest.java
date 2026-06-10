package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminUserUpdateRequest {

    @Size(max = 120, message = "Nome demasiado longo")
    private String displayName;

    @Email(message = "Email inválido")
    private String email;

    @Size(max = 30, message = "Telefone inválido")
    private String phone;

    @Size(max = 500, message = "URL da foto demasiado longa")
    private String photoUrl;

    @Size(max = 120, message = "Cidade demasiado longa")
    private String city;

    @Size(max = 120, message = "Região demasiado longa")
    private String region;

    @Size(max = 120, message = "País demasiado longo")
    private String country;

    @Size(max = 40, message = "Género inválido")
    private String gender;

    private LocalDate birthDate;
}
