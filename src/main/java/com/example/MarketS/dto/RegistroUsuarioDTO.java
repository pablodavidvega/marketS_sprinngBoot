package com.example.MarketS.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroUsuarioDTO {
    //Validaciones//
    @NotBlank(message = "El nombre no puede estar vacio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String username;

    @NotBlank(message = "El correo electronico no puede estar vacio")
    @Email(message = "Debe ser un correo valido")
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacia")
    @Size(min = 8,message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
    @NotBlank(message = "Confirmar contraseña")
    private String confirmPassword;

    @NotBlank(message = "El numero de telefono no puede estar vacio")
    private String phone;

    @NotBlank(message = "La direccion no puede estar vacia")
    private String address;


    private String tipoUsuario;
}
