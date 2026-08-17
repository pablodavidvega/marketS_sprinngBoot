package com.example.MarketS.service.impl;

import com.example.MarketS.exception.RecursoNoEncontradoException;
import com.example.MarketS.exception.UsuarioConRelacionesException;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.ResenaCompradorRepository;
import com.example.MarketS.repository.ResenaRepository;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.service.UserService;

import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final ResenaRepository resenaRepository;
    private final ResenaCompradorRepository resenaCompradorRepository;

    public UserServiceImpl(UserRepository userRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           ProductoRepository productoRepository,
                           PedidoRepository pedidoRepository,
                           ResenaRepository resenaRepository,
                           ResenaCompradorRepository resenaCompradorRepository) {
        this.userRepository = userRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.resenaRepository = resenaRepository;
        this.resenaCompradorRepository = resenaCompradorRepository;
    }

    @Override
    public User registrarUsuario(User user) {
        // Codificar contraseña solo si es nueva o está siendo cambiada
        if (user.getId() == null && user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Establecer campos para nuevo usuario
        if (user.getId() == null) {
            user.setActive(true); // Usar setActive() no setIsActive()
            user.setCreatedAt(LocalDateTime.now());
        }

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public Optional<User> buscarPorId(Long id) {
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
    public long contarTodos() {
        return userRepository.count();
    }

    @Override
    public long contarPorRol(String rolnombre) {
        return userRepository.countByRolRolname(rolnombre);
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
        // Para usuarios nuevos, establecer campos por defecto
        if (user.getId() == null) {
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
        }

        // Codificar contraseña si se proporcionó una nueva
        if (user.getPassword() != null &&
                !user.getPassword().isEmpty() &&
                !user.getPassword().startsWith("$2a$")) { // Verifica si ya está codificada (bcrypt)
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void actualizar(User user) {
        // Verificar que el usuario existe antes de actualizar
        if (user.getId() != null && userRepository.existsById(user.getId())) {
            // Mantener la contraseña existente si no se proporciona una nueva
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                User existingUser = userRepository.findById(user.getId()).orElse(null);
                if (existingUser != null) {
                    user.setPassword(existingUser.getPassword());
                }
            } else {
                // Codificar la nueva contraseña
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            // Mantener created_at y is_active del usuario existente
            User existingUser = userRepository.findById(user.getId()).orElse(null);
            if (existingUser != null) {
                user.setCreatedAt(existingUser.getCreatedAt());
                user.setActive(existingUser.isActive()); // Usar isActive() no getActive()
            }

            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        } else {
            throw new RecursoNoEncontradoException("Usuario no encontrado para actualizar");
        }
    }

    @Override
    public void cambiarRol(Long userId, Long rolId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Rol nuevoRol = rolRepository.findById(rolId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con id " + rolId));

        user.setRol(nuevoRol);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void eliminarUsuario(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado con ID " + id);
        }

        List<String> motivos = new ArrayList<>();
        if (productoRepository.countByVendedorId(id) > 0) {
            motivos.add("tiene productos publicados");
        }
        if (pedidoRepository.existsByUsuarioId(id)) {
            motivos.add("tiene pedidos como comprador");
        }
        if (pedidoRepository.existsByProductoVendedorId(id)) {
            motivos.add("tiene ventas registradas");
        }
        if (resenaRepository.existsByCompradorId(id)) {
            motivos.add("dejó reseñas de productos");
        }
        if (resenaCompradorRepository.countByCompradorId(id) > 0 || resenaCompradorRepository.existsByVendedorId(id)) {
            motivos.add("tiene calificaciones de reputación (como comprador o vendedor)");
        }

        if (!motivos.isEmpty()) {
            throw new UsuarioConRelacionesException(
                    "No se puede eliminar el usuario: " + String.join(", ", motivos) + "."
            );
        }

        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> buscarPorToken(String token) {
        return userRepository.findByResetToken(token);
    }

    // 🔹 MÉTODO ADICIONAL: Para activar/desactivar usuario
    public void cambiarEstado(Long userId, boolean activo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        user.setActive(activo);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}