package com.example.MarketS.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Sin esto, un fallo de @Valid en un DTO (ej. contraseña con menos de 8 caracteres) cae en el
// whitelabel error de Spring, que devuelve {"error": "Bad Request"} (la frase HTTP genérica) en
// vez del mensaje del @Size/@NotBlank — el frontend termina mostrando "Bad Request" en vez de
// explicar qué está mal.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Datos inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", mensaje));
    }

    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<?> handleArchivoInvalido(ArchivoInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UsuarioConRelacionesException.class)
    public ResponseEntity<?> handleUsuarioConRelaciones(UsuarioConRelacionesException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    // Red de seguridad para cualquier borrado/operación que rompa una foreign key y que no
    // haya sido detectada de antemano por una validación explícita (ver
    // UserServiceImpl.eliminarUsuario) — evita que se cuele como 500 genérico sin explicación.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleIntegridadReferencial(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad referencial: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "No se puede completar la operación: el registro tiene datos asociados en otra parte del sistema."
        ));
    }

    // Body del request ilegible (JSON malformado, tipo equivocado, etc.) — es un error del
    // cliente, no del servidor; sin este handler caía en el catch-all de abajo como 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleMensajeIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Solicitud inválida"));
    }

    // Red de seguridad final: cualquier excepción no contemplada arriba. El mensaje real de la
    // excepción (que puede filtrar detalles internos — nombres de tabla/columna en errores SQL,
    // rutas de archivo, stack traces) nunca se manda al cliente; queda solo en el log del
    // servidor. Complementa a server.error.include-message=never en prod (esto cubre lo mismo
    // desde el lado de Spring MVC, para no depender solo de esa propiedad).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenerico(Exception ex) {
        log.error("Excepción no manejada en un endpoint de la API", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno del servidor"));
    }
}
