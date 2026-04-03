package com.example.watchshop.controller.api;

import com.example.watchshop.entity.User;
import com.example.watchshop.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ApiAuthController {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    private final JavaMailSender mailSender;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            return ResponseEntity.ok(Map.of(
                    "message", "Đăng nhập thành công!",
                    "user", email,
                    "roles", authentication.getAuthorities()
            ));
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tài khoản của bạn đã bị khóa bởi quản trị viên!"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Sai email hoặc mật khẩu!"));
        }
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkStatus(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "user", auth.getName(),
                    "roles", auth.getAuthorities()
            ));
        }
        return ResponseEntity.ok(Map.of("authenticated", false));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Đã đăng xuất!");
    }

    // 1. API ĐĂNG KÝ VÀ GỬI MÃ OTP
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Validation cơ bản
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Mật khẩu không được để trống!"));
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user != null) {
            if (user.isEnabled()) {
                return ResponseEntity.status(400).body(Map.of("error", "Email này đã được đăng ký!"));
            }
        } else {
            user = new User();
            user.setEmail(request.getEmail());
        }

        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");
        user.setEnabled(false);

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setVerificationCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Mã xác thực tài khoản WatchShop");
            message.setText("Chào " + user.getFullName() + ",\n\n"
                    + "Mã xác thực OTP của bạn là: " + otp + "\n"
                    + "Mã này sẽ hết hạn sau 5 phút.");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("LỖI GỬI MAIL: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi gửi mail: Vui lòng kiểm tra lại địa chỉ email hoặc cấu hình."));
        }

        return ResponseEntity.ok(Map.of("message", "Mã xác thực đã được gửi vào email của bạn."));
    }

    // 2. API XÁC NHẬN MÃ OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy tài khoản!"));
        }
        if (user.isEnabled()) {
            return ResponseEntity.status(400).body(Map.of("error", "Tài khoản đã được kích hoạt rồi!"));
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body(Map.of("error", "Mã OTP đã hết hạn. Vui lòng đăng ký lại!"));
        }
        if (!request.getOtp().equals(user.getVerificationCode())) {
            return ResponseEntity.status(400).body(Map.of("error", "Mã OTP không chính xác!"));
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Xác thực thành công! Bạn có thể đăng nhập ngay bây giờ."));
    }

    @Data
    public static class RegisterRequest {
        private String fullName;
        private String email;
        private String password;
    }

    @Data
    public static class VerifyRequest {
        private String email;
        private String otp;
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !user.isEnabled()) {
            return ResponseEntity.status(404).body(Map.of("error", "Email không tồn tại hoặc chưa được kích hoạt!"));
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setVerificationCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5)); // Hạn 5 phút
        userRepository.save(user);

        // Gửi Mail
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Mã khôi phục mật khẩu WatchShop");
            message.setText("Chào " + user.getFullName() + ",\n\n"
                    + "Mã OTP khôi phục mật khẩu của bạn là: " + otp + "\n"
                    + "Mã này sẽ hết hạn sau 5 phút. Vui lòng không chia sẻ cho người khác.");
            mailSender.send(message);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi gửi mail. Vui lòng thử lại sau."));
        }

        return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi đến email của bạn."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Tài khoản không tồn tại!"));
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body(Map.of("error", "Mã OTP đã hết hạn. Vui lòng gửi lại yêu cầu!"));
        }
        if (!request.getOtp().equals(user.getVerificationCode())) {
            return ResponseEntity.status(400).body(Map.of("error", "Mã OTP không chính xác!"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        user.setVerificationCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập ngay!"));
    }
    @Data
    public static class ResetPasswordRequest {
        private String email;
        private String otp;
        private String newPassword;
    }
}