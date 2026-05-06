package com.garbo.core.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.garbo.core.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // Derived query (keeps existing behavior)
    Optional<User> findFirstByEmailAndPasswordOrderByEmpIdAsc(String email, String password);

    // Find by email case-insensitive
    Optional<User> findFirstByEmailIgnoreCase(String email);
}
