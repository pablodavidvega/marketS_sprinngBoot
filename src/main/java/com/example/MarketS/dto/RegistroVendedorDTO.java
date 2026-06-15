package com.example.MarketS.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroVendedorDTO extends RegistroUsuarioDTO {
    private String city;
    private String invitacion; // token de invitación (opcional según configuración)
}
