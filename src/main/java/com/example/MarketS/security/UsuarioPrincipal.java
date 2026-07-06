package com.example.MarketS.security;

import com.example.MarketS.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

// Envuelve al usuario de dominio ya autenticado por el JWT, para que los controllers
// no tengan que volver a consultarlo en la base de datos (evita duplicar el round-trip
// que el filtro JWT ya hizo en CustomUserDetailsService).
public class UsuarioPrincipal extends org.springframework.security.core.userdetails.User {

    private final User usuario;

    public UsuarioPrincipal(User usuario) {
        super(usuario.getEmail(), usuario.getPassword(),
                List.of(new SimpleGrantedAuthority(usuario.getRol().getRolname())));
        this.usuario = usuario;
    }

    public User getUsuario() {
        return usuario;
    }

    // Devuelve el usuario de dominio ya autenticado, o null si no aplica (no autenticado,
    // token de otro tipo, etc.) — quien llama decide si eso es un error o un caso opcional.
    public static User from(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        return principal instanceof UsuarioPrincipal up ? up.getUsuario() : null;
    }
}
