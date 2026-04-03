package com.example.watchshop.repository;

import com.example.watchshop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<User> findByRole(String role);
    long countByCreatedAtAfter(LocalDateTime date);
}