package com.example.watchshop.controller;

import com.example.watchshop.dto.CartItem;
import com.example.watchshop.entity.Order;
import com.example.watchshop.entity.ShopSettings;
import com.example.watchshop.entity.User;
import com.example.watchshop.repository.OrderRepository;
import com.example.watchshop.repository.ShopSettingsRepository;
import com.example.watchshop.repository.UserRepository;
import com.example.watchshop.service.interfaces.CartService;
import com.example.watchshop.service.interfaces.OrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ShopSettingsRepository shopSettingsRepository;

    @GetMapping("/checkout")
    public String checkout(Model model,
                           @RequestParam(value = "mode", required = false) String mode,
                           HttpSession session,
                           Principal principal) {
        if (principal == null) return "redirect:/account/login";

        User currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
        Order order = new Order();
        if (currentUser != null) {
            order.setFullName(currentUser.getFullName());
            order.setPhoneNumber(currentUser.getPhoneNumber());
            order.setAddress(currentUser.getAddress());
        }

        List<CartItem> itemsToCheckout = new ArrayList<>();
        double totalAmount = 0;

        if ("direct".equals(mode)) {
            CartItem directItem = (CartItem) session.getAttribute("buyNowItem");
            if (directItem == null) return "redirect:/products";
            itemsToCheckout.add(directItem);
            totalAmount = directItem.getTotalPrice();
        } else {
            itemsToCheckout = new ArrayList<>(cartService.getCartItems());
            totalAmount = cartService.getTotalAmount();
            if (itemsToCheckout.isEmpty()) return "redirect:/cart";
        }

        // Lấy thông tin shop để JS tạo mã QR
        ShopSettings shop = shopSettingsRepository.findById(1).orElse(null);

        model.addAttribute("cartItems", itemsToCheckout);
        model.addAttribute("totalPrice", totalAmount);
        model.addAttribute("order", order);
        model.addAttribute("checkoutMode", mode);
        model.addAttribute("shop", shop);

        return "checkout";
    }

    @PostMapping("/checkout/place-order")
    public String placeOrder(@ModelAttribute("order") Order order,
                             @RequestParam(value = "checkoutMode", required = false) String mode,
                             HttpSession session,
                             Principal principal) {
        if (principal == null) return "redirect:/account/login";

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/account/login?error=user_not_found";

        order.setUser(user);
        List<CartItem> itemsToSave = new ArrayList<>();
        double finalPrice = 0;

        if ("direct".equals(mode)) {
            CartItem directItem = (CartItem) session.getAttribute("buyNowItem");
            if (directItem != null) {
                itemsToSave.add(directItem);
                finalPrice = directItem.getTotalPrice();
            }
        } else {
            itemsToSave = new ArrayList<>(cartService.getCartItems());
            finalPrice = cartService.getTotalAmount();
        }

        if (itemsToSave.isEmpty()) return "redirect:/";

        order.setOrderDate(LocalDateTime.now());
        order.setStatus("Chờ xác nhận");
        order.setTotalPrice(finalPrice);
        order.setCode("ORD-" + System.currentTimeMillis());

        orderService.save(order, itemsToSave);

        if ("direct".equals(mode)) {
            session.removeAttribute("buyNowItem");
        } else {
            cartService.clear();
        }

        return "redirect:/orders?success=true";
    }

    @GetMapping("/orders")
    public String listOrders(Model model, Principal principal) {
        if (principal == null) return "redirect:/account/login";
        model.addAttribute("orders", orderRepository.findByUser_EmailOrderByOrderDateDesc(principal.getName()));
        return "orders";
    }

    @GetMapping("/orders/detail/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/account/login";

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null || !order.getUser().getEmail().equals(principal.getName())) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);

        ShopSettings shop = shopSettingsRepository.findById(1).orElse(null);
        if (shop != null && shop.getBankName() != null && !shop.getBankName().isEmpty()) {
            long amount = order.getTotalPrice() != null ? order.getTotalPrice().longValue() : 0;
            String content = "THANHTOAN " + order.getCode();
            String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                    shop.getBankName(), shop.getBankAccountNumber(), amount, content, shop.getBankAccountName());
            model.addAttribute("qrCodeUrl", qrUrl);
            model.addAttribute("shopBank", shop);
        }
        return "order-detail";
    }
}