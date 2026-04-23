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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
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
                .csrf(csrf -> csrf
                        // Cookie-based CSRF: el token vive en el browser, no en la sesión del server.
                        // Esto resuelve el problema en Render donde la sesión se pierde al reiniciar.
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/enviar-correo/**")
                )
                
                .authorizeHttpRequests(auth -> auth

                        //PÚBLICOS
                        .requestMatchers(
                                "/", "/login",
                                "/usuarios/registro", "/usuarios/registro/**",
                                "/CSS/**", "/JS/**", "/img/**", "/fondos/**",
                                "/error",
                                "/redireccionar-por-rol",
                                "/productos/**",
                                "/uploads/**",
                                "/filtros",
                                "/servicio-cliente",
                                "/servicio-cliente/enviar",
                                "/enviar-correo/**",
                                "/api/pdf/**",
                                "/api/usuarios/estadisticas",
                                "/login",
                                "/forgot-password",
                                "/reset-password"
                        ).permitAll()

                        // ROLES
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/vendedor/**").hasAnyAuthority("ROLE_VENDEDOR", "ROLE_ADMIN")
                        .requestMatchers("/cliente/**").hasAnyAuthority("ROLE_CLIENTE", "ROLE_VENDEDOR", "ROLE_ADMIN")

                        // 🛒 PEDIDOS Y CARRITO
                        .requestMatchers("/pedido/**", "/carrito/**")
                        .authenticated()

                        // SIEMPRE AL FINAL
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        // NO forzar siempre la URL por defecto: permitir que Spring redirija a la petición original (savedRequest)
                        // No forzar la URL por defecto: permitir redirección a la request guardada, si no existe ir a /redireccionar-por-rol
                        .defaultSuccessUrl("/redireccionar-por-rol", false)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .exceptionHandling(handling ->
                        handling.accessDeniedPage("/access_denied")
                );

        return http.build();
    }

}
