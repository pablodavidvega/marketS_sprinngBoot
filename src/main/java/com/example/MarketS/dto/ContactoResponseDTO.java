package com.example.MarketS.dto;

import com.example.MarketS.model.Contacto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ContactoResponseDTO {

    private Long id;
    private String nombre;
    private String email;
    private String tipoSolicitud;
    private String asunto;
    private String mensaje;
    private String estado;
    private String respuesta;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaRespuesta;
    private String usuarioUsername;

    public static ContactoResponseDTO from(Contacto c) {
        return ContactoResponseDTO.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .email(c.getEmail())
                .tipoSolicitud(c.getTipoSolicitud())
                .asunto(c.getAsunto())
                .mensaje(c.getMensaje())
                .estado(c.getEstado() != null ? c.getEstado().name() : null)
                .respuesta(c.getRespuesta())
                .fechaCreacion(c.getFechaCreacion())
                .fechaRespuesta(c.getFechaRespuesta())
                .usuarioUsername(c.getUser() != null ? c.getUser().getUsername() : null)
                .build();
    }
}
