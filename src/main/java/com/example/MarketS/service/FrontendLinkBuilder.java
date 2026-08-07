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

    private String rutaPedidos(boolean esVendedor) {
        return esVendedor ? "/vendedor/pedidos" : "/cliente/mis-pedidos";
    }
}
