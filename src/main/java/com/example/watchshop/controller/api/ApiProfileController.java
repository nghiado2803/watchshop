package com.example.watchshop.controller.api;

import com.example.watchshop.entity.User;
import com.example.watchshop.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class ApiProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private String cleanEmail(String email) {
        if (email == null) return null;
        return email.replace("\"", "").trim();
    }

    private String extractEmail(Authentication auth, String paramEmail) {
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            if (auth.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
                return oauthUser.getAttribute("email");
            }
            return auth.getName();
        }
        return paramEmail;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestParam(required = false) String email, Authentication auth) {
        String targetEmail = cleanEmail(extractEmail(auth, email));

        if (targetEmail == null || targetEmail.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        User user = userRepository.findByEmail(targetEmail).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy người dùng: " + targetEmail));
        }

        ProfileDTO dto = new ProfileDTO();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setRole(user.getRole());

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileDTO payload, @RequestParam(required = false) String email, Authentication auth) {
        String targetEmail = cleanEmail(extractEmail(auth, email));

        if (targetEmail == null || targetEmail.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        User user = userRepository.findByEmail(targetEmail).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy người dùng"));
        }

        user.setFullName(payload.getFullName());
        user.setPhoneNumber(payload.getPhoneNumber());
        user.setAddress(payload.getAddress());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin thành công!"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordDTO payload, @RequestParam(required = false) String email, Authentication auth) {
        String targetEmail = cleanEmail(extractEmail(auth, email));

        if (targetEmail == null || targetEmail.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        User user = userRepository.findByEmail(targetEmail).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy người dùng"));
        }

        if (!passwordEncoder.matches(payload.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("error", "Mật khẩu hiện tại không đúng!"));
        }

        user.setPassword(passwordEncoder.encode(payload.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
    }

    @Data
    public static class ProfileDTO {
        private String fullName;
        private String email;
        private String phoneNumber;
        private String address;
        private String role;
    }

    @Data
    public static class PasswordDTO {
        private String currentPassword;
        private String newPassword;
    }
}