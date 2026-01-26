package com.example.watchshop.service.impl;

import com.example.watchshop.entity.User;
import com.example.watchshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Tìm user trong DB bằng email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + email));

        // 2. Trả về đối tượng UserDetails cho Spring Security xử lý
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword()) // Password này là mã Hash trong DB
                .roles(getRoles(user))
                .build();
    }

    // Hàm phụ: Cắt bỏ tiền tố "ROLE_" nếu có (VD: ROLE_ADMIN -> ADMIN)
    private String getRoles(User user) {
        String role = user.getRole();
        if (role != null && role.startsWith("ROLE_")) {
            return role.substring(5);
        }
        return "USER"; // Mặc định nếu không có role
    }
}