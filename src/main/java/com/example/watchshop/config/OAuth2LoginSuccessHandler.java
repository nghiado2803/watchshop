package com.example.watchshop.config;

import com.example.watchshop.entity.User;
import com.example.watchshop.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Tạo mới user nếu chưa tồn tại
            user = new User();
            user.setEmail(email);
            user.setFullName(name);
            user.setRole("ROLE_USER");
            user.setEnabled(true);
            user.setPassword(UUID.randomUUID().toString());
            userRepository.save(user);
        } else {
            // ĐIỂM CHỐT CHẶN: User tồn tại, kiểm tra xem có bị khóa không
            if (!user.isEnabled()) {
                // Nếu bị khóa, hất văng về trang đăng nhập của Vue kèm tham số lỗi
                response.sendRedirect("http://localhost:5173/login?error=locked");
                return; // Dừng thực thi ngay lập tức, không cho đi tiếp
            }
        }

        // Nếu bình thường (hoặc mới tạo), redirect về trang chủ báo thành công
        response.sendRedirect("http://localhost:5173/?login_google=true&email=" + email);
    }
}