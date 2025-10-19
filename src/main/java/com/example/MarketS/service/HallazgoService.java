package com.example.MarketS.service;

import com.example.MarketS.model.Hallazgo;
import com.example.MarketS.repository.HallazgoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HallazgoService {

    @Autowired
    private HallazgoRepository repository;

    public void registrarHallazgo(String prueba, String tipo, boolean exito, String descripcion) {
        Hallazgo h = new Hallazgo();
        h.setPrueba(prueba);
        h.setTipo(tipo);
        h.setResultado(exito ? "Aprobada" : "Fallida");
        h.setDescripcion(descripcion);
        h.setFecha(LocalDateTime.now());
        repository.save(h);
    }

    public List<Hallazgo> obtenerTodos() {
        return repository.findAll();
    }
}
