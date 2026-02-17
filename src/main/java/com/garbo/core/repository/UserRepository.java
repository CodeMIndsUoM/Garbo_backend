package com.garbo.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.garbo.core.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // Derived query (keeps existing behavior)
    Optional<User> findFirstByEmailAndPasswordOrderByEmpIdAsc(String email, String password);

    // Native query to fetch the single matching user (LIMIT 1)
    @Query(value = "SELECT * FROM users u WHERE u.email = :email AND u.password = :password ORDER BY u.emp_id ASC LIMIT 1", nativeQuery = true)
    Optional<User> findByEmailAndPasswordNative(@Param("email") String email, @Param("password") String password);

    // Native query to fetch all users (example usage)
    @Query(value = "SELECT * FROM users", nativeQuery = true)
    List<User> findAllNative();

    // Find by email case-insensitive
    Optional<User> findFirstByEmailIgnoreCase(String email);

    // Native case-insensitive single lookup (useful for debugging)
    @Query(value = "SELECT * FROM users u WHERE LOWER(u.email) = LOWER(:email) LIMIT 1", nativeQuery = true)
    Optional<User> findByEmailNative(@Param("email") String email);
}
