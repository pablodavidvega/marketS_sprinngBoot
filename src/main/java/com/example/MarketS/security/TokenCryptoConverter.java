package com.example.MarketS.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

// Cifra en reposo credenciales sensibles (access_token/refresh_token de MercadoPago que cada
// vendedor autoriza — ver VendedorMercadoPago) con AES/GCM. Se lee la clave con System.getenv
// en vez de @Value porque los AttributeConverter de JPA los instancia el proveedor de
// persistencia con su propio ciclo de vida, no siempre pasan por el contenedor de Spring.
//
// La clave (MP_TOKEN_ENCRYPTION_KEY) debe ser 32 bytes en Base64, ej. generada con:
// openssl rand -base64 32
@Converter
public class TokenCryptoConverter implements AttributeConverter<String, String> {

    private static final String ENV_VAR = "MP_TOKEN_ENCRYPTION_KEY";
    // Clave de desarrollo únicamente (32 bytes) — nunca usar en producción; ahí es obligatorio
    // configurar MP_TOKEN_ENCRYPTION_KEY como variable de entorno real en Render.
    private static final String CLAVE_DEV_POR_DEFECTO = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    @Override
    public String convertToDatabaseColumn(String valorPlano) {
        if (valorPlano == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, claveSecreta(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cifrado = cipher.doFinal(valorPlano.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(cifrado, 0, resultado, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar el token de MercadoPago", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String valorCifrado) {
        if (valorCifrado == null) return null;
        try {
            byte[] datos = Base64.getDecoder().decode(valorCifrado);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(datos, 0, iv, 0, IV_LENGTH_BYTES);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, claveSecreta(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] textoPlano = cipher.doFinal(datos, IV_LENGTH_BYTES, datos.length - IV_LENGTH_BYTES);
            return new String(textoPlano, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo descifrar el token de MercadoPago", e);
        }
    }

    private SecretKeySpec claveSecreta() {
        String claveBase64 = System.getenv(ENV_VAR);
        if (claveBase64 == null || claveBase64.isBlank()) {
            claveBase64 = CLAVE_DEV_POR_DEFECTO;
        }
        byte[] clave = Base64.getDecoder().decode(claveBase64);
        return new SecretKeySpec(clave, "AES");
    }
}
