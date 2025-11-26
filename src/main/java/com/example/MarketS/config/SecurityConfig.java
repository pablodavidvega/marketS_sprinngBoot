package com.example.MarketS.config;

import com.example.MarketS.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 🔹 Desactiva CSRF solo para las rutas de correo (para Postman)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/usuarios/registro", "/enviar-correo/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login", "/usuarios/registro", "/usuarios/registro/**",
                                "/CSS/**", "/JS/**", "/img/**", "/fondos/**", "/error",
                                "/redireccionar-por-rol", "/productos/**", "/uploads/**",
                                "/filtros", "/enviar-correo/**" // Permite acceso libre a correo
                        ).permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/vendedor/**").hasAnyAuthority("ROLE_VENDEDOR", "ROLE_ADMIN")
                        .requestMatchers("/cliente/**").hasAnyAuthority("ROLE_CLIENTE", "ROLE_VENDEDOR", "ROLE_ADMIN")
                        .requestMatchers("/productos", "/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")
                        .requestMatchers("/admin/reporte-hallazgos/pdf").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/pdf/**").permitAll()
                        .requestMatchers("/api/usuarios/estadisticas").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/redireccionar-por-rol", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(handling -> handling
                        .accessDeniedPage("/access_denied")
                );

        return http.build();
    }
}
