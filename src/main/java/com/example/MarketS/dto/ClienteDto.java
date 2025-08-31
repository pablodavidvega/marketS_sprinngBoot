package com.example.MarketS.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClienteDto {

    @NotBlank(message = "El nombre completo es obligatorio.")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres.")
    private String username;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Debe ingresar un correo electrónico válido.")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio.")
    @Pattern(
            regexp = "^[0-9]{7,15}$",
            message = "El teléfono debe contener entre 7 y 15 dígitos numéricos."
    )
    private String phone;

    @NotBlank(message = "La dirección es obligatoria.")
    @Size(max = 100, message = "La dirección no puede exceder los 100 caracteres.")
    private String address;

    // Getters y Setters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
