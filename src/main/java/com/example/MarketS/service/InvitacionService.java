package com.example.MarketS.service;

import com.example.MarketS.model.Invitacion;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.InvitacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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

    // Valida y consume un uso — llamar solo durante el registro
    public boolean usarToken(String token) {
        Optional<Invitacion> opt = repo.findByToken(token);
        if (opt.isEmpty()) return false;
        Invitacion inv = opt.get();
        if (!inv.isVigente()) return false;
        inv.setUsosActuales(inv.getUsosActuales() + 1);
        if (inv.getMaxUsos() != null && inv.getUsosActuales() >= inv.getMaxUsos()) {
            inv.setActivo(false);
        }
        repo.save(inv);
        return true;
    }

    public List<Invitacion> listar() {
        return repo.findAll();
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
