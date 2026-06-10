package com.kumbu.backend.dto.job;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CvCreateRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 120, message = "Título demasiado longo")
    private String title;

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(max = 120, message = "Nome demasiado longo")
    private String fullName;

    @Size(max = 120, message = "Profissão demasiado longa")
    private String profession;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @Size(max = 30, message = "Telefone inválido")
    private String phone;

    @Size(max = 120, message = "Cidade demasiado longa")
    private String city;

    @Size(max = 120, message = "Província demasiado longa")
    private String province;

    @Size(max = 4000, message = "Resumo demasiado longo")
    private String summary;

    private List<@NotBlank @Size(max = 80) String> skills;
    private List<@NotBlank @Size(max = 80) String> languages;
}
