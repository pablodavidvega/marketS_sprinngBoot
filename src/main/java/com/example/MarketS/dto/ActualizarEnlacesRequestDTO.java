package com.example.MarketS.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ActualizarEnlacesRequestDTO {

    @Valid
    @Size(max = 6, message = "No puedes agregar más de 6 enlaces")
    private List<EnlaceItemDTO> enlaces;

    @Getter
    @Setter
    public static class EnlaceItemDTO {
        @NotBlank(message = "La etiqueta es obligatoria")
        @Size(max = 30, message = "La etiqueta no puede exceder 30 caracteres")
        private String etiqueta;

        @NotBlank(message = "La URL es obligatoria")
        @Size(max = 300, message = "La URL no puede exceder 300 caracteres")
        private String url;
    }
}
