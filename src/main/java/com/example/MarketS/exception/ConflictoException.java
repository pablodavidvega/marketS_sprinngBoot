package com.example.MarketS.exception;

import org.springframework.http.HttpStatus;

// La operación choca con el estado actual de un recurso: duplicados, relaciones que impiden
// borrar (ver UsuarioConRelacionesException), una acción que ya se realizó antes, etc.
public class ConflictoException extends NegocioException {
    public ConflictoException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
