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
import java.util.UUID;

@Configuration
public class DataSeeder {

    // Solo crea un admin cuando la tabla users está completamente vacía (arranque en frío o
    // reset total de datos), nunca con un email/password fijos: así no queda una credencial
    // adivinable corriendo en cada arranque de producción, pero el sistema sigue pudiendo
    // arrancarse solo tras un reset (nadie más puede crear el primer admin, ya que crear
    // admins requiere ya ser admin).
    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository,
                                       RolRepository rolRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            Rol adminRol = rolRepository.findByRolname("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Rol newRol = new Rol();
                        newRol.setRolname("ROLE_ADMIN");
                        return rolRepository.save(newRol);
                    });

            String adminEmail = "admin@marketS.local";
            String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            User admin = new User();
            admin.setUsername("Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(tempPassword));
            admin.setPhone("000000000");
            admin.setAddress("");
            admin.setRol(adminRol);
            admin.setActive(true);
            admin.setAceptoTerminos(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());

            userRepository.save(admin);

            System.out.println("======================================================");
            System.out.println("Base de datos de usuarios vacía: se creó un admin inicial");
            System.out.println("Email:    " + adminEmail);
            System.out.println("Password: " + tempPassword);
            System.out.println("Iniciá sesión y cambiá esta contraseña de inmediato.");
            System.out.println("Este mensaje no vuelve a salir mientras exista un usuario.");
            System.out.println("======================================================");
        };
    }
}
