package com.example.MarketS.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// A diferencia de RegistroVendedorDTO, NO lleva email/password: esos datos salen del
// pendingToken (verificado server-side en el paso 1), nunca de lo que mande el cliente.
@Getter
@Setter
public class GoogleCompletarRegistroDTO {
    @NotBlank(message = "Falta el pendingToken")
    private String pendingToken;

    @NotBlank(message = "El nombre no puede estar vacio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String username;

    @NotBlank(message = "El numero de telefono no puede estar vacio")
    private String phone;

    @NotBlank(message = "La direccion no puede estar vacia")
    private String address;

    private String city;         // obligatorio solo si tipoUsuario=VENDEDOR (validado a mano)
    private String invitacion;    // token de invitación (opcional según modo de registro)

    private String tipoUsuario;   // "CLIENTE" | "VENDEDOR"

    @AssertTrue(message = "Debes aceptar los Términos de Uso para continuar")
    private boolean aceptaTerminos;
}
