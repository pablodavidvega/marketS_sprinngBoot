package com.example.MarketS.service.impl;

import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.service.UserService;

import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    public UserServiceImpl(UserRepository userRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registrarUsuario(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public Optional<User> buscarPorId(Long id){
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> buscarPorNombre(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> buscarPorEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> buscarPorDireccion(String address) {
        return userRepository.findByAddress(address);
    }

    @Override
    public List<User> listarPorRol(String rolname) {
        return userRepository.findByRolRolname(rolname);
    }

    @Override
    public List<User> buscarPorNombreOCorreo(String filtro) {
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(filtro, filtro);
    }

    @Override
    public List<User> listarTodos() {
        return userRepository.findAll();
    }

    @Override
    public boolean verificarPassword(String raw, String encoded) {
        return passwordEncoder.matches(raw, encoded);
    }

    @Override
    public String codificarPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public void guardar(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void actualizar(User user) {
        guardar(user); // delega en guardar
    }

    // 🔹 Nuevo método: cambiar rol
    @Override
    public void cambiarRol(Long userId, Long rolId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Rol nuevoRol = rolRepository.findById(rolId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado con id " + rolId));

        user.setRol(nuevoRol);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }


    // 🔹 Nuevo método: eliminar usuario
    @Override
    public void eliminarUsuario(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado con ID " + id);
        }
        userRepository.deleteById(id);
    }
}
