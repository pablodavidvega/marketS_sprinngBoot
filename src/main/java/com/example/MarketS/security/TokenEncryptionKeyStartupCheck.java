package com.example.MarketS.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

// TokenCryptoConverter no es un bean de Spring (lo instancia el proveedor de persistencia, ver
// su comentario) y por eso no puede validar por sí mismo si está corriendo en el perfil prod:
// lee la clave con System.getenv y, si MP_TOKEN_ENCRYPTION_KEY falta, cae en una clave de
// desarrollo hardcodeada en el código, sin avisar. Este chequeo sí corre como bean de Spring
// (con acceso al Environment) y frena el arranque en prod si falta esa variable, para que el
// deploy quede visiblemente roto en vez de cifrar en silencio los tokens reales de MercadoPago
// de los vendedores con una clave que cualquiera puede leer en el repo.
@Configuration
public class TokenEncryptionKeyStartupCheck {

    private static final String ENV_VAR = "MP_TOKEN_ENCRYPTION_KEY";

    @Bean
    public CommandLineRunner verificarClaveCifradoMercadoPago(Environment env) {
        return args -> {
            boolean esProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
            String clave = System.getenv(ENV_VAR);
            if (esProd && (clave == null || clave.isBlank())) {
                throw new IllegalStateException(
                        "Falta la variable de entorno " + ENV_VAR + " en el perfil prod. Sin ella, " +
                        "TokenCryptoConverter usaría la clave de desarrollo hardcodeada en el código " +
                        "para cifrar los tokens reales de MercadoPago de los vendedores. " +
                        "Generá una con 'openssl rand -base64 32' y configurala en las variables de entorno de Render."
                );
            }
        };
    }
}
