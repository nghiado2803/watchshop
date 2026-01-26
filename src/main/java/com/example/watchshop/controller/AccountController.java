package com.example.watchshop.controller;

import com.example.watchshop.entity.ShopSettings;
import com.example.watchshop.entity.User;
import com.example.watchshop.repository.ShopSettingsRepository;
import com.example.watchshop.repository.UserRepository;
import com.example.watchshop.service.interfaces.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ShopSettingsRepository shopSettingsRepository;

    @GetMapping("/login")
    public String login(HttpServletRequest request, @RequestParam(value = "message", required = false) String message, Model model) {
        if (request.getUserPrincipal() != null) {
            return "redirect:/";
        }
        if ("pending_approval".equals(message)) {
            model.addAttribute("message", "Đăng ký thành công! Vui lòng chờ Admin duyệt tài khoản.");
        }
        return "account/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model, HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return "redirect:/";
        }
        model.addAttribute("user", new User());
        return "account/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user,
                           @RequestParam("confirmPassword") String confirmPassword,
                           Model model) {
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu nhập lại không khớp!");
            return "account/register";
        }
        try {
            // 1. Mã hóa mật khẩu
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);

            // 2. Thiết lập mặc định
            user.setRole("ROLE_USER");
            user.setCreatedAt(LocalDateTime.now());

            // 3. QUAN TRỌNG: Set false để chờ Admin duyệt
            user.setEnabled(false);

            userService.save(user);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Đăng ký thất bại. Email này có thể đã được sử dụng!");
            return "account/register";
        }
        // Chuyển hướng về login kèm thông báo chờ duyệt
        return "redirect:/account/login?message=pending_approval";
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        if (principal == null) return "redirect:/account/login";

        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        model.addAttribute("user", user);

        if (user != null && "ROLE_ADMIN".equals(user.getRole())) {
            ShopSettings settings = shopSettingsRepository.findById(1).orElse(new ShopSettings());
            model.addAttribute("shopSettings", settings);
        }

        return "account/profile";
    }

    @PostMapping("/profile/update")
    public String update(@ModelAttribute User userForm,
                         @ModelAttribute ShopSettings shopSettingsForm,
                         Principal principal) {

        if (principal == null) return "redirect:/account/login";

        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email).orElse(null);

        if (currentUser != null) {
            currentUser.setFullName(userForm.getFullName());
            currentUser.setPhoneNumber(userForm.getPhoneNumber());
            currentUser.setAddress(userForm.getAddress());
            userRepository.save(currentUser);

            if ("ROLE_ADMIN".equals(currentUser.getRole())) {
                ShopSettings currentSettings = shopSettingsRepository.findById(1).orElse(new ShopSettings());
                currentSettings.setBankName(shopSettingsForm.getBankName());
                currentSettings.setBankAccountNumber(shopSettingsForm.getBankAccountNumber());
                currentSettings.setBankAccountName(shopSettingsForm.getBankAccountName());
                shopSettingsRepository.save(currentSettings);
            }
        }
        return "redirect:/account/profile?success";
    }

    @GetMapping("/forgot-password")
    public String forgot() { return "account/forgot-password"; }

    @GetMapping("/change-password")
    public String changePasswordForm() { return "account/change-password"; }
}