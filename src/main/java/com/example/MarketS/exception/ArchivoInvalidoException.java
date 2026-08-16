package com.example.MarketS.exception;

// El archivo subido no es una imagen o video real (magic bytes no coinciden con ningún
// formato soportado), sin importar qué extensión o Content-Type haya declarado el cliente.
public class ArchivoInvalidoException extends SolicitudInvalidaException {
    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
