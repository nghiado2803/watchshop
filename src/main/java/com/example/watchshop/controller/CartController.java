package com.example.watchshop.controller;

import com.example.watchshop.dto.CartItem;
import com.example.watchshop.entity.Product;
import com.example.watchshop.service.interfaces.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.example.watchshop.service.interfaces.ProductService;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart") // Gom nhóm đường dẫn bắt đầu bằng /cart
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final ProductService productService;

    // 1. Hiển thị trang giỏ hàng
    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("totalPrice", cartService.getTotalAmount());
        return "cart"; // Trả về cart.html
    }

    // 2. Thêm sản phẩm vào giỏ
    @PostMapping("/add")
    public String add(@RequestParam("productId") Long productId,
                      @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                      @RequestParam(value = "buyNow", required = false) Boolean buyNow,
                      HttpSession session) { // <--- Cần Session để lưu đồ tạm

        // TRƯỜNG HỢP 1: MUA NGAY (Xử lý riêng)
        if (buyNow != null && buyNow) {
            Product product = productService.findById(productId).orElse(null);
            if (product != null) {
                // Tạo một CartItem riêng lẻ
                CartItem directItem = new CartItem(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        quantity,
                        product.getImageUrl()
                );

                // Lưu item này vào Session riêng (khác giỏ hàng chính)
                session.setAttribute("buyNowItem", directItem);
            }
            // Chuyển hướng sang checkout với cờ hiệu mode=direct
            return "redirect:/checkout?mode=direct";
        }

        // TRƯỜNG HỢP 2: THÊM VÀO GIỎ (Logic cũ)
        cartService.add(productId, quantity);
        return "redirect:/cart";
    }

    // 3. Xóa sản phẩm
    @GetMapping("/remove/{id}")
    public String remove(@PathVariable("id") Long id) {
        cartService.remove(id);
        return "redirect:/cart";
    }

    // 4. Cập nhật số lượng
    @PostMapping("/update")
    public String update(@RequestParam("productId") Long productId,
                         @RequestParam("quantity") int quantity) {
        cartService.update(productId, quantity);
        return "redirect:/cart";
    }
}