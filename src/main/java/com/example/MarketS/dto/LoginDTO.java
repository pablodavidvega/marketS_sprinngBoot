package com.example.MarketS.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class LoginDTO {
    @NotBlank(message = "Ingrese su correo electronico")
    public String email;

    @NotBlank(message = "Ingrese su contraseña")
    public String password;
}
