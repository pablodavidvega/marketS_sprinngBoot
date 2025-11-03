package com.example.MarketS.factory;

import com.example.MarketS.model.Correo;
import java.util.List;

public class CorreoFactory {

    public static Correo crearCorreoPrueba(String para) {
        return new Correo.Builder()
                .asunto("Correo de prueba desde MarketS")
                .mensaje("Hola, este es un correo de prueba enviado desde MarketS.")
                .destinatarios(List.of(para))
                .build();
    }

    public static Correo crearCorreoMasivo(List<String> destinatarios, String asunto, String mensaje) {
        return new Correo.Builder()
                .asunto(asunto)
                .mensaje(mensaje)
                .destinatarios(destinatarios)
                .build();
    }
}
