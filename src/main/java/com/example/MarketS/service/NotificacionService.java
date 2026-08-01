package com.example.MarketS.service;

import com.example.MarketS.model.Notificacion;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repo;

    public void crear(User usuario, String mensaje, String tipo, Long pedidoId) {
        crear(usuario, mensaje, tipo, pedidoId, null, null);
    }

    public void crear(User usuario, String mensaje, String tipo, Long pedidoId, Long productoId) {
        crear(usuario, mensaje, tipo, pedidoId, productoId, null);
    }

    // vendedorId: solo para notificaciones de chat/encuentro, permite al frontend abrir
    // directamente el hilo correcto cuando un pedido tiene varios vendedores.
    public void crear(User usuario, String mensaje, String tipo, Long pedidoId, Long productoId, Long vendedorId) {
        crear(usuario, mensaje, tipo, pedidoId, productoId, vendedorId, null);
    }

    // rolDestino ("COMPRADOR"/"VENDEDOR"): usar cuando el mismo tipo de notificación se manda
    // indistintamente a cualquiera de las dos partes de un pedido (chat, encuentro) — ver
    // comentario en Notificacion.rolDestino. Para el resto de los tipos, que ya son
    // inequívocos, no hace falta pasarlo (queda null y el frontend deduce el rol del tipo).
    public void crear(User usuario, String mensaje, String tipo, Long pedidoId, Long productoId, Long vendedorId, String rolDestino) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setPedidoId(pedidoId);
        n.setProductoId(productoId);
        n.setVendedorId(vendedorId);
        n.setRolDestino(rolDestino);
        n.setFechaCreacion(LocalDateTime.now());
        repo.save(n);
    }

    public List<Notificacion> listarRecientes(Long usuarioId) {
        return repo.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId, PageRequest.of(0, 20));
    }

    public long contarNoLeidas(Long usuarioId) {
        return repo.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    public Page<Notificacion> listarPaginado(Long usuarioId, boolean soloNoLeidas, Pageable pageable) {
        return soloNoLeidas
                ? repo.findByUsuarioIdAndLeida(usuarioId, false, pageable)
                : repo.findByUsuarioId(usuarioId, pageable);
    }

    @Transactional
    public void marcarLeida(Long id, Long usuarioId) {
        repo.findById(id).ifPresent(n -> {
            if (n.getUsuario().getId().equals(usuarioId)) {
                n.setLeida(true);
                repo.save(n);
            }
        });
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        repo.marcarTodasLeidasByUsuarioId(usuarioId);
    }
}
