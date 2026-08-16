package com.example.MarketS.exception;

// El usuario tiene datos asociados (productos publicados, pedidos, etc.) que impiden
// borrarlo directamente sin romper la integridad referencial de esas otras tablas.
public class UsuarioConRelacionesException extends RuntimeException {
    public UsuarioConRelacionesException(String mensaje) {
        super(mensaje);
    }
}
