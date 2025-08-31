package com.example.MarketS.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.MarketS.model.User;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByAddress(String address);
    List<User> findByRolRolname(String rolname);
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);

}
