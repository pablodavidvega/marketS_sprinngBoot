package com.example.MarketS.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Arma los links que van dentro de los correos de notificación, apuntando al mismo lugar
// exacto que la campanita de notificaciones abre en el frontend (ver
// NotificacionesBell.tsx#destinoDe) — así el usuario cae directo en la pantalla relevante en
// vez de en el home. Si no tiene sesión abierta, ProtectedRoute lo manda a /login guardando
// esta URL como destino y el login lo redirige de vuelta acá al terminar.
@Component
public class FrontendLinkBuilder {

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public String pedido(boolean esVendedor, Long pedidoId) {
        return frontendUrl + rutaPedidos(esVendedor) + "?pedido=" + pedidoId;
    }

    public String pedidoConFoco(boolean esVendedor, Long pedidoId, Long vendedorId, String foco) {
        return frontendUrl + rutaPedidos(esVendedor) + "?pedido=" + pedidoId + "&vendedor=" + vendedorId + "&foco=" + foco;
    }

    public String producto(Long productoId) {
        return frontendUrl + "/productos/" + productoId + "?foco=preguntas";
    }

    public String mercadoPagoConectado(boolean exito) {
        return frontendUrl + "/vendedor/pagos?conectado=" + exito;
    }

    public String conectarMercadoPago() {
        return frontendUrl + "/vendedor/pagos";
    }

    // Destino del correo de bienvenida: el panel propio de cada rol (mismo mapeo que
    // EditarPerfilPage#VOLVER_POR_ROL). ADMIN/MODERADOR no se autoregistran hoy (ver
    // AuthApiController/GoogleAuthApiController: tipoUsuario solo admite CLIENTE/VENDEDOR),
    // pero se contemplan por si ese registro se habilita más adelante.
    public String bienvenida(String rolname) {
        String ruta = switch (rolname) {
            case "ROLE_VENDEDOR" -> "/vendedor";
            case "ROLE_ADMIN" -> "/admin/dashboard";
            case "ROLE_MODERADOR" -> "/moderador/dashboard";
            default -> "/cliente/dashboard";
        };
        return frontendUrl + ruta;
    }

    private String rutaPedidos(boolean esVendedor) {
        return esVendedor ? "/vendedor/pedidos" : "/cliente/mis-pedidos";
    }
}
