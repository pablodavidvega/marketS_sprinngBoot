//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@Configuration
//public class AdminInitConfig {
//
//    @Bean
//    CommandLineRunner initAdmin(UserRepository userRepo, PasswordEncoder encoder) {
//        return args -> {
//            if (userRepo.findByEmail("admin@correo.com") == null) {
//                User admin = new User();
//                admin.setEmail("admin@correo.com");
//                admin.setPassword(encoder.encode("123456")); // contraseña encriptada
//                admin.setRole("ADMIN"); // tu campo de rol
//                userRepo.save(admin);
//                System.out.println("✔ Admin creado: admin@correo.com / 123456");
//            }
//        };
//    }
//}
