package com.example.MarketS.service;

import com.example.MarketS.model.Contacto;
import com.example.MarketS.model.EstadoContacto;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.ContactoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContactoService {

    @Autowired
    private ContactoRepository contactoRepository;

    public Contacto guardar(Contacto contacto) {
        return contactoRepository.save(contacto);
    }

    public List<Contacto> listarTodos() {
        return contactoRepository.findAllByOrderByFechaCreacionDesc();
    }

    public List<Contacto> listarPorUsuario(User user) {
        return contactoRepository.findByUserOrderByFechaCreacionDesc(user);
    }

    public Optional<Contacto> buscarPorId(Long id) {
        return contactoRepository.findById(id);
    }

    public long contarPorEstado(EstadoContacto estado) {
        return contactoRepository.countByEstado(estado);
    }

    public Contacto responder(Long id, String respuesta) {
        Contacto contacto = contactoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        contacto.setRespuesta(respuesta);
        contacto.setEstado(EstadoContacto.RESUELTO);
        contacto.setFechaRespuesta(LocalDateTime.now());
        return contactoRepository.save(contacto);
    }

    public Contacto cambiarEstado(Long id, EstadoContacto estado) {
        Contacto contacto = contactoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        contacto.setEstado(estado);
        return contactoRepository.save(contacto);
    }
}
