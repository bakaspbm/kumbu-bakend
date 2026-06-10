package com.kumbu.backend.dto.user;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    private String fullName;

    @Size(max = 32, message = "Telefone demasiado longo")
    private String phone;

    @Size(max = 120, message = "Cidade demasiado longa")
    private String city;

    @Size(max = 120, message = "Província demasiado longa")
    private String region;

    @Size(max = 80, message = "País demasiado longo")
    private String country;

    @Size(max = 500, message = "URL da foto demasiado longa")
    private String photoUrl;

    @Size(max = 40, message = "Género inválido")
    private String gender;

    @Past(message = "Data de nascimento deve ser no passado")
    private LocalDate birthDate;
}
