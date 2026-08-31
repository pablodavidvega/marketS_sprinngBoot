package com.example.MarketS.service;

import com.example.MarketS.model.Invitacion;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.InvitacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitacionService {

    private final InvitacionRepository repo;

    public Invitacion crear(String descripcion, Integer maxUsos, LocalDateTime fechaExpiracion, User admin) {
        Invitacion inv = new Invitacion();
        inv.setToken(UUID.randomUUID().toString().replace("-", ""));
        inv.setDescripcion(descripcion);
        inv.setMaxUsos(maxUsos);
        inv.setFechaExpiracion(fechaExpiracion);
        inv.setFechaCreacion(LocalDateTime.now());
        inv.setCreadaPor(admin);
        inv.setActivo(true);
        return repo.save(inv);
    }

    public Optional<Invitacion> buscarPorToken(String token) {
        return repo.findByToken(token);
    }

    public boolean esVigente(String token) {
        return repo.findByToken(token)
                .map(Invitacion::isVigente)
                .orElse(false);
    }

    // Valida y consume un uso de forma atómica — llamar solo durante el registro, ANTES de
    // crear la cuenta (ver AuthApiController/GoogleAuthApiController). Usar un UPDATE
    // condicionado en vez de leer-y-guardar evita que dos registros concurrentes con el mismo
    // link de un solo uso pasen ambos la validación antes de que cualquiera confirme el consumo.
    @Transactional
    public boolean usarToken(String token) {
        return repo.consumirUso(token) > 0;
    }

    // Invitaciones paginadas (30 por defecto, ver InvitacionAdminRestController), con filtro
    // opcional por el estado derivado que muestra el panel admin (activa/expirada/agotada/revocada).
    public Page<Invitacion> listar(String estado, Pageable pageable) {
        return repo.findAllFiltrado(estado, pageable);
    }

    // Conteos globales para las tarjetas de resumen — no varían con el filtro/página activos.
    public Map<String, Object> stats() {
        return Map.of(
                "total",     repo.count(),
                "activa",    repo.countActivas(),
                "expirada",  repo.countExpiradas(),
                "agotada",   repo.countAgotadas(),
                "revocada",  repo.countRevocadas(),
                "totalUsos", repo.sumUsosActuales()
        );
    }

    public void revocar(Long id) {
        repo.findById(id).ifPresent(inv -> {
            inv.setActivo(false);
            repo.save(inv);
        });
    }

    public void reactivar(Long id) {
        repo.findById(id).ifPresent(inv -> {
            inv.setActivo(true);
            repo.save(inv);
        });
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
