package com.example.MarketS.seeders;

import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository,
                                       RolRepository rolRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            // Verificar si ya existe el rol ADMIN
            Rol adminRol = rolRepository.findByRolname("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Rol newRol = new Rol();
                        newRol.setRolname("ROLE_ADMIN");
                        return rolRepository.save(newRol);
                    });

            // Verificar si ya existe un admin con ese correo
            String adminEmail = "admin@hanaservizi.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setUsername("Administrador");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setPhone("123456789");
                admin.setAddress("cll201iw");
                admin.setRol(adminRol);
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());

                userRepository.save(admin);
                System.out.println("✅ Admin user creado: " + adminEmail);
            } else {
                System.out.println("ℹ️ Admin user ya existe: " + adminEmail);
            }
        };
    }
}
