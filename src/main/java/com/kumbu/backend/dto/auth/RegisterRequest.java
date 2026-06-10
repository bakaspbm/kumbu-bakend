package com.kumbu.backend.dto.auth;

import com.kumbu.backend.domain.enums.SignupSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Password é obrigatória")
    @Size(min = 8, max = 72, message = "Password deve ter entre 8 e 72 caracteres")
    private String password;

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    private String fullName;

    @Pattern(regexp = "^$|^[+]?[0-9]{8,15}$", message = "Telefone inválido")
    private String phone;

    private SignupSource signupSource = SignupSource.APP;
}
