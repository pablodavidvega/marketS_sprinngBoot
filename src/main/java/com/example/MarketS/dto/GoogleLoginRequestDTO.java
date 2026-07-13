package com.example.MarketS.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequestDTO {
    @NotBlank(message = "Falta el id_token de Google")
    private String idToken;
}
