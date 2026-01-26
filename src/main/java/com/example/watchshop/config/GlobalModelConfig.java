package com.example.watchshop.config;

import com.example.watchshop.service.interfaces.CartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor // <--- QUAN TRỌNG: Phải có dòng này để Inject CartService
public class GlobalModelConfig {

    private final CartService cartService;

    // Lấy URI hiện tại để highlight menu active
    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    // Tự động thêm biến "cartCount" vào mọi file HTML (Header nào cũng cần)
    @ModelAttribute("cartCount")
    public int getCartCount() {
        return cartService.getCount();
    }

    // Biến cho thông báo chat/admin (Sau này phát triển thêm)
    @ModelAttribute("unreadMessages")
    public int getUnreadMessages() {
        return 0;
    }
}