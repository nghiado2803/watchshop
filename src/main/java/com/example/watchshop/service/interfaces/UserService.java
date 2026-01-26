package com.example.watchshop.service.interfaces;

import com.example.watchshop.entity.User;
import java.util.Optional;

public interface UserService {
    Optional<User> findByEmail(String email);
    User save(User user);
    boolean existsByEmail(String email);
}