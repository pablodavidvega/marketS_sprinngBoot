package com.example.MarketS.service;


import com.example.MarketS.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User registrarUsuario(User user);

    Optional<User> buscarPorId(Long id);

    Optional<User> buscarPorNombre(String username);

    Optional<User> buscarPorEmail(String email);

    List<User> buscarPorDireccion(String address);

    List<User> listarPorRol(String rolnombre);

    List<User> buscarPorNombreOCorreo(String filtro);

    List<User> listarTodos();

    boolean verificarPassword(String raw, String encoded);

    String codificarPassword(String password);

    void guardar(User user); // guarda cambios y actualiza updatedAt

    void actualizar(User user); // opcionalmente igual a guardar()

    void cambiarRol(Long userId, Long rolId);

    void eliminarUsuario(Long id);
}

