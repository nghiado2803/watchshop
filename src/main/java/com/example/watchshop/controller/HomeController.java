package com.example.watchshop.controller;

import com.example.watchshop.entity.Product;
import com.example.watchshop.service.interfaces.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request) {
        request.getSession(true); // Fix lỗi session

        // Lấy danh sách sản phẩm từ SQL
        List<Product> products = productService.findAll();

        // Lấy 4 sản phẩm đầu tiên làm Best Sellers
        if (!products.isEmpty()) {
            int limit = Math.min(products.size(), 4);
            model.addAttribute("bestSellers", products.subList(0, limit));
        } else {
            model.addAttribute("bestSellers", products);
        }
        return "index";
    }
}