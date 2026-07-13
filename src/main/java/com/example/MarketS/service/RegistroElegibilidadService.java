package com.example.MarketS.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Punto único de verdad para decidir si un email puede registrarse, según el modo de
// registro configurado (ABIERTO/DOMINIO/INVITACION/DOMINIO_O_INVITACION). Lo usan tanto el
// registro clásico (email+password) como el alta vía Google, que necesita evaluar la
// elegibilidad en dos momentos: antes de pedir el código de invitación (tokenInvitacion=null)
// y de nuevo con el código ya provisto.
@Service
@RequiredArgsConstructor
public class RegistroElegibilidadService {

    public enum Resultado { PERMITIDO, REQUIERE_INVITACION, DENEGADO }

    public record Evaluacion(Resultado resultado, String mensajeError) {
        public boolean permitido() {
            return resultado == Resultado.PERMITIDO;
        }
    }

    private final ConfiguracionAppService configService;
    private final InvitacionService invitacionService;

    public Evaluacion evaluar(String email, String tokenInvitacion) {
        String modo = configService.getModoRegistro();

        if (ConfiguracionAppService.MODO_ABIERTO.equals(modo)) {
            return new Evaluacion(Resultado.PERMITIDO, null);
        }

        String dominio = configService.getDominioPermitido();

        boolean dominioOk = false;
        if ((ConfiguracionAppService.MODO_DOMINIO.equals(modo) ||
             ConfiguracionAppService.MODO_DOMINIO_O_INVITACION.equals(modo))
             && !dominio.isBlank()) {
            String emailDomain = email != null && email.contains("@")
                    ? email.substring(email.indexOf('@') + 1).toLowerCase()
                    : "";
            dominioOk = emailDomain.equalsIgnoreCase(dominio.trim());
        }

        boolean tokenProvisto = tokenInvitacion != null && !tokenInvitacion.isBlank();
        boolean invitacionOk = false;
        if ((ConfiguracionAppService.MODO_INVITACION.equals(modo) ||
             ConfiguracionAppService.MODO_DOMINIO_O_INVITACION.equals(modo))
             && tokenProvisto) {
            invitacionOk = invitacionService.esVigente(tokenInvitacion);
        }

        return switch (modo) {
            case ConfiguracionAppService.MODO_DOMINIO -> dominioOk
                    ? new Evaluacion(Resultado.PERMITIDO, null)
                    : new Evaluacion(Resultado.DENEGADO, "Solo pueden registrarse usuarios con correo @" + dominio);

            case ConfiguracionAppService.MODO_INVITACION -> {
                if (invitacionOk) yield new Evaluacion(Resultado.PERMITIDO, null);
                yield tokenProvisto
                        ? new Evaluacion(Resultado.DENEGADO, "El registro requiere un link de invitación válido")
                        : new Evaluacion(Resultado.REQUIERE_INVITACION, "El registro requiere un link de invitación válido");
            }

            case ConfiguracionAppService.MODO_DOMINIO_O_INVITACION -> {
                if (dominioOk || invitacionOk) yield new Evaluacion(Resultado.PERMITIDO, null);
                String mensaje = "Acceso restringido: se requiere correo @" + dominio + " o link de invitación";
                yield tokenProvisto
                        ? new Evaluacion(Resultado.DENEGADO, mensaje)
                        : new Evaluacion(Resultado.REQUIERE_INVITACION, mensaje);
            }

            default -> new Evaluacion(Resultado.PERMITIDO, null);
        };
    }
}
