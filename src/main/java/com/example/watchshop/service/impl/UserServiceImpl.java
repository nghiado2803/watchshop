package com.example.watchshop.service.impl;

import com.example.watchshop.entity.User;
import com.example.watchshop.repository.UserRepository;
import com.example.watchshop.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        // Lưu ý: Thực tế chỗ này cần mã hóa mật khẩu trước khi lưu (passwordEncoder.encode(...))
        // Nhưng tạm thời để nguyên để test luồng chạy trước.
        return userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}