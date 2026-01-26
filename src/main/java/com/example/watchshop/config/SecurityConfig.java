package com.example.watchshop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- FIX LỖI Ở BEAN NÀY ---
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // 1. Tạo đối tượng bằng constructor rỗng
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        // 2. Set Service để lấy thông tin user (Load user by username)
        authProvider.setUserDetailsService(userDetailsService);

        // 3. Set Encoder để mã hóa và so sánh mật khẩu
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        // Cho phép truy cập file tĩnh (ảnh, css, js) để không bị lỗi giao diện
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()

                        // Phân quyền cho Admin (Lưu ý: trong DB role phải là "ADMIN" hoặc "ROLE_ADMIN")
                        .requestMatchers("/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // Các trang cần đăng nhập mới vào được
                        .requestMatchers("/checkout/**", "/cart/**", "/orders/**", "/account/profile").authenticated()

                        // Còn lại cho phép truy cập tất cả
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/account/login")
                        .loginProcessingUrl("/account/login") // Link submit form login
                        .usernameParameter("email")           // Tên ô input email
                        .passwordParameter("password")        // Tên ô input password
                        .defaultSuccessUrl("/", false)        // false: login xong quay lại trang đang xem dở
                        .failureUrl("/account/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }
}