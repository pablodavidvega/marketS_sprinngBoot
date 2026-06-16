package com.example.MarketS.service;

import com.example.MarketS.model.Notificacion;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repo;

    public void crear(User usuario, String mensaje, String tipo, Long pedidoId) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setPedidoId(pedidoId);
        n.setFechaCreacion(LocalDateTime.now());
        repo.save(n);
    }

    public List<Notificacion> listarRecientes(Long usuarioId) {
        return repo.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId, PageRequest.of(0, 20));
    }

    public long contarNoLeidas(Long usuarioId) {
        return repo.countByUsuarioIdAndLeidaFalse(usuarioId);
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
