package com.example.MarketS.repository;

import com.example.MarketS.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.MarketS.model.User;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
    Optional<User> findByResetTokenAndResetTokenExpiraAfter(
            String token,
            LocalDateTime fecha);
    List<User> findByAddress(String address);
    List<User> findByRolRolname(String rolname);
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);
    long countByRol(Rol rol);
    long countByRolRolname(String rolname);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
