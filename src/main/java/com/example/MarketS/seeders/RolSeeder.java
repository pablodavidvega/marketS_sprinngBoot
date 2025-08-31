package com.example.MarketS.seeders;

import com.example.MarketS.model.Rol;
import com.example.MarketS.repository.RolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RolSeeder implements CommandLineRunner {
    private final RolRepository rolRepository;

    public RolSeeder(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public void run(String... args) {
        crearRolSiNoExiste("ROLE_ADMIN");
        crearRolSiNoExiste("ROLE_CLIENTE");
        crearRolSiNoExiste("ROLE_VENDEDOR");
    }

    public void crearRolSiNoExiste(String nombreRol) {
        if (!rolRepository.existsByRolname(nombreRol)) {
            Rol rol = new Rol();
            rol.setRolname(nombreRol);
            rolRepository.save(rol);
            System.out.println("Rol creado: " + nombreRol);
        }
    }
}