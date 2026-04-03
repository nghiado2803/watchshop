package com.example.watchshop.config;

import com.example.watchshop.service.interfaces.CartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor //
public class GlobalModelConfig {

    private final CartService cartService;

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("cartCount")
    public int getCartCount() {
        return cartService.getCount();
    }

    @ModelAttribute("unreadMessages")
    public int getUnreadMessages() {
        return 0;
    }
}