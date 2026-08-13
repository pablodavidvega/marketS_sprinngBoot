package com.example.MarketS.service.impl;

// Valida el tipo real de un archivo por sus magic bytes, no por la extensión o el
// Content-Type declarado (ambos los controla quien sube el archivo y no dicen nada del
// contenido real). Solo cubre los formatos que la app efectivamente sirve — el mismo set que
// CargaMasivaVendedorService.detectarContentType usa para la carga masiva por ZIP.
final class ArchivoMultimediaValidator {

    private ArchivoMultimediaValidator() {
    }

    static boolean esImagenOVideoValido(byte[] bytes) {
        return esImagen(bytes) || esVideo(bytes);
    }

    private static boolean esImagen(byte[] b) {
        return esJpeg(b) || esPng(b) || esGif(b) || esWebp(b);
    }

    private static boolean esVideo(byte[] b) {
        return esMp4OMov(b) || esAvi(b);
    }

    private static boolean esJpeg(byte[] b) {
        return coincide(b, 0, 0xFF, 0xD8, 0xFF);
    }

    private static boolean esPng(byte[] b) {
        return coincide(b, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
    }

    private static boolean esGif(byte[] b) {
        return coincide(b, 0, 'G', 'I', 'F', '8', '7', 'a') || coincide(b, 0, 'G', 'I', 'F', '8', '9', 'a');
    }

    private static boolean esWebp(byte[] b) {
        // Contenedor RIFF con FourCC "WEBP" en el offset 8
        return coincide(b, 0, 'R', 'I', 'F', 'F') && coincide(b, 8, 'W', 'E', 'B', 'P');
    }

    private static boolean esAvi(byte[] b) {
        // Contenedor RIFF con FourCC "AVI " en el offset 8
        return coincide(b, 0, 'R', 'I', 'F', 'F') && coincide(b, 8, 'A', 'V', 'I', ' ');
    }

    private static boolean esMp4OMov(byte[] b) {
        // ISO Base Media (mp4) y QuickTime (mov) comparten estructura de "atoms/boxes": el
        // tamaño del box (4 bytes) seguido del nombre del box (4 bytes) en el offset 4.
        return coincide(b, 4, 'f', 't', 'y', 'p')
                || coincide(b, 4, 'm', 'o', 'o', 'v')
                || coincide(b, 4, 'm', 'd', 'a', 't')
                || coincide(b, 4, 'w', 'i', 'd', 'e')
                || coincide(b, 4, 'f', 'r', 'e', 'e')
                || coincide(b, 4, 's', 'k', 'i', 'p');
    }

    private static boolean coincide(byte[] datos, int offset, int... esperado) {
        if (datos == null || datos.length < offset + esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if ((datos[offset + i] & 0xFF) != (esperado[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
