package com.example.MarketS.exception;

import org.springframework.http.HttpStatus;

// Datos de la solicitud inválidos o incompletos según una regla de negocio (no un error de
// bean validation de @Valid, que ya maneja MethodArgumentNotValidException aparte). Equivalente
// de negocio al viejo "IllegalArgumentException" atrapado ad-hoc en cada controller.
public class SolicitudInvalidaException extends NegocioException {
    public SolicitudInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
