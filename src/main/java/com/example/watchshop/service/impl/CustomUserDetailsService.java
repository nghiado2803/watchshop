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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(getRoles(user))
                .disabled(!user.isEnabled())
                .build();
    }

    private String getRoles(User user) {
        String role = user.getRole();
        if (role != null && role.startsWith("ROLE_")) {
            return role.substring(5);
        }
        return "USER";
    }
}